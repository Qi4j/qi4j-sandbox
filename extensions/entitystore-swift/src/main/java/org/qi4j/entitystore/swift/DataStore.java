/*
 * Copyright 2008 Niclas Hedhman.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.qi4j.entitystore.swift;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.entitystore.map.MapEntityStore;
import org.qi4j.spi.entitystore.EntityStoreException;

/**
 * This class handles the Heap Data file.
 * The format of the file is as follows;
 *
 * <code><pre>
 * At OFFSET = 0
 * [cleanShutDown]  1 byte
 * [formatVersion]  4 bytes
 * [noOfEntries]    4 bytes
 * [noOfIDentries]  4 bytes
 *
 * At OFFSET 256
 * [blockSize]     4 bytes
 * [usage]         1 byte    (0=Unused, 1=prime, 2=mirror, 3=primeChanged, 4=mirrorChanged)
 * [instanceVersion] 8 bytes
 * [schemaVersion] 4 bytes
 * [identitySize]  1 byte
 * [identity]      IDENTITY_MAX_LENGTH bytes
 * [mirrorPointer] 8 bytes
 * [primeDataLength] 4 bytes
 * [primeData]     n bytes
 * [mirrorDataLength] 4 bytes
 * [mirrorData]    n bytes
 *
 * At OFFSET 256 + [blockSize]
 * same as above, repeat until [blockSize] == -1 marking end of DataArea.
 * </pre></code>
 * The <b>mirrorPointer</b> points to the mirrorData block.
 */
public class DataStore
{
    static final long DATA_AREA_OFFSET = 256;
    private static final int BLOCK_OVERHEAD = 26;
    private static final int CURRENT_VERSION = 1;
    private static final String HEAP_DATA_FILENAME = "heap.data";

    private static final int USAGE_UNUSED = 0;
    private static final int USAGE_PRIME = 1;
    private static final int USAGE_MIRROR = 2;
    private static final int USAGE_PRIMECHANGE = 3;
    private static final int USAGE_MIRRORCHANGE = 4;

    private RandomAccessFile dataFile;
    private IdentityFile identityFile;
    private int identityMaxLength;
    private UndoManager undoManager;
    private int entries;
    private File dataDir;

    public DataStore( File dataDirectory, UndoManager undoManager )
        throws IOException
    {
        this.undoManager = undoManager;
        identityMaxLength = 128; // Default value...
        this.dataDir = dataDirectory.getAbsoluteFile();
        dataDir.mkdirs();
        File file = new File( dataDir, HEAP_DATA_FILENAME );
        if( !file.exists() )
        {
            file.createNewFile();
        }
        dataFile = new RandomAccessFile( file, "rw" );
        boolean cleanShutDown;
        if( file.length() > 0 )
        {
            dataFile.seek( 0 );
            cleanShutDown = dataFile.readBoolean();
            dataFile.seek( 0 );
            dataFile.writeBoolean( false );
            dataFile.writeInt( CURRENT_VERSION );  // Write Version.
            entries = dataFile.readInt();
            identityMaxLength = dataFile.readInt();
        }
        else
        {
            cleanShutDown = false;
            dataFile.writeBoolean( false );
            entries = 0;
            dataFile.writeInt( CURRENT_VERSION );  // Write Version.
            dataFile.writeInt( entries );
            dataFile.writeInt( identityMaxLength );
            dataFile.seek( DATA_AREA_OFFSET - 1 );
            dataFile.writeByte( 0 );
            dataFile.seek( DATA_AREA_OFFSET );
            dataFile.writeInt( -1 );  // EOF marker
        }
        // Ensure full flush, then reopen...
        dataFile.close();

        dataFile = new RandomAccessFile( file, "rw" );

        if( !cleanShutDown )
        {
            reIndex();
        }
        else
        {
            File idDir = new File( dataDir, "idx" );
            try
            {
                identityFile = IdentityFile.use( idDir );
            }
            catch( MalformedIdentityDirectoryException e )
            {
                reIndex();
            }
        }
        if( identityFile.entries() < entries * 2 )
        {
            reIndex();
        }
    }

    RandomAccessFile dataFile()
    {
        return dataFile;
    }

    IdentityFile identityFile()
    {
        return identityFile;
    }

    DataBlock readData( EntityReference reference )
        throws IOException
    {
        long pos = identityFile.find( reference );
        if( pos < 0 )
        {
            return null;
        }
        dataFile.seek( pos );
        dataFile.skipBytes( 4 ); // Skip BlockSize
        return readDataBlock( reference );
    }

    void putData( DataBlock data )
        throws IOException
    {
        long pos = identityFile.find( data.reference );
        if( pos < 0 )
        {
            putNewData( data );
        }
        else
        {
            dataFile.seek( pos );
            int blockSize = dataFile.readInt();
            long usagePointer = dataFile.getFilePointer();
            byte usage = dataFile.readByte();
            dataFile.skipBytes( -1 );
            dataFile.writeByte( usage == USAGE_PRIME ? USAGE_PRIMECHANGE : USAGE_MIRRORCHANGE );
            int dataAreaSize = ( blockSize - BLOCK_OVERHEAD ) / 2 - 4;
            if( dataAreaSize < data.data.length )
            {
                putTooLarge( data, pos, usagePointer, usage );
            }
            else
            {
                putOver( data, pos, usagePointer, usage );
            }
        }
    }

    /* In this case we need to write the new data to the opposite of the current active block. */
    private void putOver( DataBlock data, long pos, long usagePointer, byte usage )
        throws IOException
    {
        dataFile.seek( usagePointer ); // Point to "usage"
        dataFile.skipBytes( 13 ); // Skip usage, instanceVersion and schemaVersion
        EntityReference existingReference = readReference();
        if( !existingReference.equals( data.reference ) )
        {
            throw new EntityStoreException( "Inconsistent Data Heap: was " + existingReference + ", expected " + data.reference );
        }
        long mirror = dataFile.readLong();
        if( usage == USAGE_PRIME )
        {
            dataFile.seek( mirror );
        }
        UndoModifyCommand undoModifyCommand = new UndoModifyCommand( pos, usage, data.instanceVersion, data.schemaVersion );
        undoManager.saveUndoCommand( undoModifyCommand );

        dataFile.writeInt( data.data.length );
        dataFile.write( data.data );
        dataFile.seek( usagePointer );
        dataFile.writeByte( usage == USAGE_PRIME ? USAGE_MIRROR : USAGE_PRIME );
    }

    /* This case is when the data doesn't fit in the pre-allocated extra space. Write it to the end, and mark the
       previous block unused.
     */
    private void putTooLarge( DataBlock data, long pos, long usagePointer, byte usage )
        throws IOException
    {
        long newPosition = addData( data );
        UndoModifyCommand undoModifyCommand = new UndoModifyCommand( pos, usage, data.instanceVersion, data.schemaVersion );
        undoManager.saveUndoCommand( undoModifyCommand );
        dataFile.seek( usagePointer );
        dataFile.writeByte( USAGE_UNUSED );
        UndoDropIdentityCommand undoDropIdentityCommand = new UndoDropIdentityCommand( data.reference, pos );
        undoManager.saveUndoCommand( undoDropIdentityCommand );
        identityFile.remember( data.reference, newPosition );
    }

    private void putNewData( DataBlock data )
        throws IOException
    {
        long pos;
        pos = addData( data );
        UndoNewIdentityCommand undoNewIdentityCommand = new UndoNewIdentityCommand( data.reference );
        undoManager.saveUndoCommand( undoNewIdentityCommand );
        identityFile.remember( data.reference, pos );
    }

    public void delete( EntityReference reference )
        throws IOException
    {
        long pos = identityFile.find( reference );
        if( pos < 0 )
        {
            // Doesn't exist.
            return;
        }
        dataFile.seek( pos );
        dataFile.skipBytes( 4 ); // Skip BlockSize
        byte usage = dataFile.readByte();
        if( usage == USAGE_UNUSED )
        {
            // Not used?? Why is the IdentityFile pointing to it then?? Should the following line actually be
            // executed here.
            //    identityFile.drop( identity );
            return;
        }
        UndoDropIdentityCommand undoDropIdentityCommand = new UndoDropIdentityCommand( reference, pos );
        undoManager.saveUndoCommand( undoDropIdentityCommand );

        UndoDeleteCommand undoDeleteCommand = new UndoDeleteCommand( pos, usage );
        undoManager.saveUndoCommand( undoDeleteCommand );

        identityFile.drop( reference );
        dataFile.skipBytes( -1 );
        dataFile.writeByte( USAGE_UNUSED );   // Mark Unused block
    }

    void flush()
        throws IOException
    {
//        dataFile.getFD().sync();
    }

    void close()
        throws IOException
    {
        identityFile.close();
        dataFile.seek( 0 );
        dataFile.writeBoolean( true );
        dataFile.writeInt( entries );
        dataFile.close();
    }

    private long addData( DataBlock block )
        throws IOException
    {
        dataFile.seek( dataFile.length() - 4 ); // last 4 bytes contain a -1
        long blockStart = dataFile.getFilePointer();

        // Allow each datablock to grow to twice its size, and provide a primary and mirror allocation.
        int dataAreaSize = ( block.data.length * 2 + 4 ) * 2;
        UndoExtendCommand undoExtendCommand = new UndoExtendCommand( blockStart );
        undoManager.saveUndoCommand( undoExtendCommand );

        int blockSize = dataAreaSize + identityMaxLength + BLOCK_OVERHEAD;
        dataFile.writeInt( blockSize );
        long usagePointer = dataFile.getFilePointer();
        dataFile.writeByte( USAGE_PRIMECHANGE ); // In-progress
        dataFile.writeLong( block.instanceVersion );
        dataFile.writeInt( block.schemaVersion );
        writeIdentity( block.reference );

        long mirrorPosition = blockStart + BLOCK_OVERHEAD + identityMaxLength + dataAreaSize / 2;
        dataFile.writeLong( mirrorPosition );
        dataFile.writeInt( block.data.length );
        dataFile.write( block.data );
        dataFile.seek( blockStart + blockSize );
        dataFile.writeInt( -1 ); // Write EOF marker.
        dataFile.seek( usagePointer );
        dataFile.write( USAGE_PRIME );
        return blockStart;
    }

    private void writeIdentity( EntityReference reference )
        throws IOException
    {
        byte[] idBytes = reference.identity().getBytes("UTF-8");
        if( idBytes.length > identityMaxLength )
        {
            throw new EntityStoreException( "Identity is too long. Only " + identityMaxLength + " characters are allowed in this EntityStore." );
        }
        byte[] id = new byte[identityMaxLength];
        System.arraycopy( idBytes, 0, id, 0, idBytes.length );
        dataFile.writeByte( idBytes.length );
        dataFile.write( id );
    }

    private void compact()
        throws IOException
    {
/*
        File newFileName = new File( dataDir, "heap-compacting.data" );
        RandomAccessFile newFile = new RandomAccessFile( newFileName, "rw" );
        File oldFileName = new File( dataDir, "heap.data" );
        RandomAccessFile oldFile = new RandomAccessFile( oldFileName, "r" );

        oldFile.seek( DATA_AREA_OFFSET ); // Skip initial bytes;
        newFile.seek( DATA_AREA_OFFSET ); // Skip initial bytes;

        int counter = 0;

        // Move the Records!!

        entries = counter;

        newFile.writeBoolean( false );
        newFile.writeInt( CURRENT_VERSION );  // Write Version.

        newFile.writeInt( entries );
        reIndex( dataDir );
        dataFile.close();
        newFile.close();

        File standardFilename = new File( dataDir, "heap.data" );
        newFileName.renameTo( standardFilename );
        dataFile = new RandomAccessFile( standardFilename, "rw" );
*/
    }

    private void reIndex()
        throws IOException
    {
        identityFile = IdentityFile.create( new File( dataDir, "idx" ), identityMaxLength + 16, entries < 5000 ? 10000 : entries * 2 );

        dataFile.seek( DATA_AREA_OFFSET );
        while( dataFile.getFilePointer() < dataFile.length() )
        {
            long blockStart = dataFile.getFilePointer();
            int blockSize = dataFile.readInt();
            if( blockSize == -1 )
            {
                break;
            }
            byte usage = dataFile.readByte();
            dataFile.skipBytes( 12 ); // Skip instanceVersion and schemaVersion
            EntityReference reference = readReference();
            if( usage != USAGE_UNUSED )
            {
                identityFile.remember( reference, blockStart );
            }
            dataFile.seek( blockStart + blockSize );
        }
    }

    public void visitMap( MapEntityStore.MapEntityStoreVisitor visitor )
    {
        try
        {
            long position = DATA_AREA_OFFSET;
            while( position < dataFile.length() )
            {
                dataFile.seek( position );
                int blockSize = dataFile.readInt();
                if( blockSize == -1 ) // EOF marker
                {
                    return;
                }
                if( blockSize == 0 )
                {
                    // TODO This is a bug. Why does it occur??
                    throw new InternalError();
                }
                position = position + blockSize;  // position for next round...
                DataBlock block = readDataBlock( null );
                if( block != null )
                {
                    visitor.visitEntity( new StringReader( new String( block.data, "UTF-8" ) ) );
                }
            }
        }
        catch( IOException e )
        {
            throw new EntityStoreException(e);
        }
    }

    private DataBlock readDataBlock( EntityReference reference )
        throws IOException
    {
        byte usage = dataFile.readByte();
        if( usage == USAGE_UNUSED )
        {
            return null;
        }
        long instanceVersion = dataFile.readLong();
        int schemaVersion = dataFile.readInt();
        EntityReference existingReference = readReference();
        if( reference == null )
        {
            reference = existingReference;
        }
        if( !existingReference.equals( reference ) )
        {
            throw new EntityStoreException( "Inconsistent Data Heap." );
        }
        if( usage == USAGE_MIRROR )
        {
            long mirror = dataFile.readLong();
            dataFile.seek( mirror );
        }
        else
        {
            dataFile.skipBytes( 8 ); // skip the MirrorPointer
        }
        int dataSize = dataFile.readInt();
        if( dataSize < 0 )
        {
            throw new InternalError();
        }
        byte[] data = new byte[dataSize];
        dataFile.read( data );
        return new DataBlock( reference, data, instanceVersion, schemaVersion );
    }

    private EntityReference readReference()
        throws IOException
    {
        int idSize = dataFile.readByte();
        if( idSize < 0 )
        {
            idSize = idSize + 256;  // Fix 2's-complement negative values of bytes into unsigned 8 bit.
        }
        byte[] idData = new byte[idSize];
        dataFile.read( idData );
        dataFile.skipBytes( identityMaxLength - idSize );
        return new EntityReference( new String( idData ) );
    }
}
