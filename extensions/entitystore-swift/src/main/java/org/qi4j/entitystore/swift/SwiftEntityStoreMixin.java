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

import org.qi4j.api.configuration.Configuration;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.injection.scope.Uses;
import org.qi4j.api.io.Input;
import org.qi4j.api.service.Activatable;
import org.qi4j.entitystore.map.MapEntityStore;
import org.qi4j.spi.entity.EntityType;
import org.qi4j.spi.entitystore.EntityNotFoundException;
import org.qi4j.spi.entitystore.EntityStoreException;
import org.qi4j.spi.service.ServiceDescriptor;

import java.io.*;
import java.util.concurrent.locks.ReadWriteLock;

public class SwiftEntityStoreMixin
    implements Activatable, MapEntityStore
{
    private @This ReadWriteLock lock;
    @Uses private ServiceDescriptor descriptor;
    @This private Configuration<SwiftConfiguration> configuration;
    private RecordManager recordManager;

    public void activate()
        throws Exception
    {
        SwiftConfiguration conf = configuration.configuration();
        String storage = conf.storageDirectory().get();
        File storageDir;
        storageDir = new File( storage );
        Boolean recover = conf.recover().get();
        if( recover == null )
        {
            recover = Boolean.TRUE;
        }
        recordManager = new RecordManager( storageDir, recover );
    }

    public void passivate()
        throws Exception
    {
        recordManager.close();
    }

    public Reader get( EntityReference entityReference )
        throws EntityStoreException
    {
        try
        {
            DataBlock dataBlock = recordManager.readData( entityReference );
            if( dataBlock == null )
            {
                throw new EntityNotFoundException( entityReference );
            }
            StringReader reader = new StringReader( new String( dataBlock.data, "UTF-8" ) );
            return reader;
        }
        catch( UnsupportedEncodingException e )
        {
            // Can not happen.
            throw new InternalError();
        }
        catch( IOException e )
        {
            throw new EntityStoreException( "Unable to read '" + entityReference + "' from the store.", e );
        }
    }

    public Input<Reader, IOException> entityStates()
    {
        return recordManager.data();
    }

    public void applyChanges( MapChanges changes )
        throws IOException
    {
        try
        {
            changes.visitMap( new MapChanger()
            {
                public Writer newEntity( final EntityReference ref, EntityType entityType ) throws IOException
                {
                    return new StringWriter( 1000 )
                    {
                        @Override public void close() throws IOException
                        {
                            super.close();

                            byte[] stateArray = toString().getBytes( "UTF-8" );
                            DataBlock block = new DataBlock( ref, stateArray, 0, 0 );
                            recordManager.putData( block );
                        }
                    };
                }

                public Writer updateEntity( final EntityReference ref, EntityType entityType ) throws IOException
                {
                    return new StringWriter( 1000 )
                    {
                        @Override public void close() throws IOException
                        {
                            super.close();
                            byte[] stateArray = toString().getBytes( "UTF-8" );
                            DataBlock block = new DataBlock( ref, stateArray, 0, 0 );
                            recordManager.putData( block );
                        }
                    };
                }

                public void removeEntity( EntityReference ref, EntityType entityType ) throws EntityNotFoundException
                {
                    try
                    {
                        recordManager.deleteData( ref );
                    }
                    catch( IOException e )
                    {
                        throw new EntityStoreException( e );
                    }
                }
            } );
            recordManager.commit();
        }
        catch( Exception e )
        {
            recordManager.discard();
            if( e instanceof IOException )
            {
                throw (IOException) e;
            }
            else if( e instanceof EntityStoreException )
            {
                throw (EntityStoreException) e;
            }
            else
            {
                IOException exception = new IOException();
                exception.initCause( e );
                throw exception;
            }
        }
    }
}
