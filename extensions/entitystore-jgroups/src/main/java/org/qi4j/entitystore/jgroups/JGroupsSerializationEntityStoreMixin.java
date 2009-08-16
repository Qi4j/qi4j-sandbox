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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import org.jgroups.JChannel;
import org.jgroups.blocks.ReplicatedHashMap;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.api.service.Activatable;
import org.qi4j.entitystore.map.MapEntityStore;
import org.qi4j.spi.entity.EntityNotFoundException;
import org.qi4j.spi.entity.EntityStoreException;
import org.qi4j.spi.entity.EntityType;

/**
 * JGroups implementation of EntityStore
 */
public class JGroupsSerializationEntityStoreMixin
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
            if( data == null )
            {
                throw new EntityNotFoundException( entityReference );
            }
            return new StringReader( data );
        }
        catch( RuntimeException e )
        {
            throw new EntityStoreException( e );
        }
    }

    public void visitMap( MapEntityStoreVisitor visitor )
    {
        try
        {
            for( Map.Entry<String, String> key : replicatedMap.entrySet() )
            {
                visitor.visitEntity( new StringReader( key.getValue() ) );
            }
        }
        catch( RuntimeException e )
        {
            throw new EntityStoreException( e );
        }
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
                        @Override public void close() throws IOException
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
                        @Override public void close() throws IOException
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
        catch( RuntimeException e )
        {
            IOException exception = new IOException();
            exception.initCause( e );
            throw exception;
        }
    }
}