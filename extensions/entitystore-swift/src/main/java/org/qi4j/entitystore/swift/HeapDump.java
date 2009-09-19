/*
 * Copyright 2009 Niclas Hedhman.
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
import org.qi4j.api.entity.EntityReference;

public class HeapDump
{
    static final long DATA_AREA_OFFSET = 256;
    private static final String HEAP_DATA_FILENAME = "heap.data";

    private static RandomAccessFile dataFile;

    public static void main( String[] args )
        throws Exception
    {
        File dataDirectory = new File( args[ 0 ] ).getAbsoluteFile();
        File dataDir = dataDirectory.getAbsoluteFile();
        File file = new File( dataDir, HEAP_DATA_FILENAME );
        dataFile = new RandomAccessFile( file, "rw" );

        long position = 256;
        dataFile.seek( position );  // skip maintenance block.
        while( dataFile.getFilePointer() < dataFile.length() )
        {
            int blockSize = dataFile.readInt();
            if( blockSize == -1 )
            {
                break;
            }
            int usage = dataFile.readByte();
            if( usage != 0 )
            {
                long instanceVersion = dataFile.readLong();
                int schemaVersion = dataFile.readInt();
                long refPos = dataFile.getFilePointer();
                String ref = readReference().identity();
                System.out.print( ref );
                dataFile.seek( refPos + 129 );
                long mirror = dataFile.readLong();
                if( usage == 2 )
                    dataFile.seek( mirror );
                if( usage == 3 || usage == 4 )
                {
                    System.err.println( "Inconsistent Heap: " + usage + ", pos: " + position  );
                    System.err.flush();
                }
                int dataSize = dataFile.readInt();
                byte[] data = new byte[ dataSize ];
                dataFile.read( data, 0, dataSize );
                System.out.println( new String( data, "UTF-8" ) );
                System.out.flush();
            }
            position = position + blockSize;
            dataFile.seek( position );
        }
    }

    private static EntityReference readReference()
        throws IOException
    {
        int idSize = dataFile.readByte();
        if( idSize < 0 )
        {
            idSize = idSize + 256;  // Fix 2's-complement negative values of bytes into unsigned 8 bit.
        }
        byte[] idData = new byte[idSize];
        dataFile.read( idData );
        dataFile.skipBytes( 128 - idSize );
        return new EntityReference( new String( idData ) );
    }
}
