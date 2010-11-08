/*  Copyright 2008 Rickard Öberg.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qi4j.entitystore.jgroups;

import org.jgroups.JChannel;
import org.jgroups.blocks.ReplicatedHashMap;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.api.io.Input;
import org.qi4j.api.io.Output;
import org.qi4j.api.io.Receiver;
import org.qi4j.api.io.Sender;
import org.qi4j.api.service.Activatable;
import org.qi4j.entitystore.map.MapEntityStore;
import org.qi4j.spi.entity.EntityType;
import org.qi4j.spi.entitystore.EntityNotFoundException;
import org.qi4j.spi.entitystore.EntityStoreException;

import java.io.*;

/**
 * JGroups implementation of EntityStore
 */
public class JGroupsEntityStoreMixin
        implements Activatable, MapEntityStore
{
    private JChannel channel;
    private ReplicatedHashMap<String, String> replicatedMap;

    public void activate() throws Exception
    {
        channel = new JChannel();
        channel.connect( "entitystore" );
        replicatedMap = new ReplicatedHashMap<String, String>( channel, false );
        replicatedMap.setBlockingUpdates( true );
    }

    public void passivate() throws Exception
    {
        channel.close();
    }

    public Reader get( EntityReference entityReference )
            throws EntityStoreException
    {
        try
        {
            String data = replicatedMap.get( entityReference.identity() );
            if (data == null)
            {
                throw new EntityNotFoundException( entityReference );
            }
            return new StringReader( data );
        }
        catch (RuntimeException e)
        {
            throw new EntityStoreException( e );
        }
    }

    public Input<Reader, IOException> entityStates()
    {
        return new Input<Reader, IOException>()
        {
            public <ReceiverThrowableType extends Throwable> void transferTo( Output<Reader, ReceiverThrowableType> output ) throws IOException, ReceiverThrowableType
            {
                output.receiveFrom( new Sender<Reader, IOException>()
                {
                    public <ReceiverThrowableType extends Throwable> void sendTo( Receiver<Reader, ReceiverThrowableType> receiver ) throws ReceiverThrowableType, IOException
                    {
                        for (String json : replicatedMap.values())
                        {
                            receiver.receive( new StringReader(json) );
                        }
                    }
                } );
            }
        };
    }

    public void applyChanges( MapChanges changes ) throws IOException
    {
        try
        {
            changes.visitMap( new MapChanger()
            {
                public Writer newEntity( final EntityReference ref, EntityType entityType ) throws IOException
                {
                    return new StringWriter( 1000 )
                    {
                        @Override
                        public void close() throws IOException
                        {
                            super.close();
                            String value = toString();
                            String key = ref.identity();
                            replicatedMap.put( key, value );
                        }
                    };
                }

                public Writer updateEntity( final EntityReference ref, EntityType entityType ) throws IOException
                {
                    return new StringWriter( 1000 )
                    {
                        @Override
                        public void close() throws IOException
                        {
                            super.close();
                            String value = toString();
                            String key = ref.identity();
                            replicatedMap.put( key, value );
                        }
                    };
                }

                public void removeEntity( EntityReference ref, EntityType entityType ) throws EntityNotFoundException
                {
                    replicatedMap.remove( ref.identity() );
                }
            } );
        }
        catch (RuntimeException e)
        {
            IOException exception = new IOException();
            exception.initCause( e );
            throw exception;
        }
    }
}