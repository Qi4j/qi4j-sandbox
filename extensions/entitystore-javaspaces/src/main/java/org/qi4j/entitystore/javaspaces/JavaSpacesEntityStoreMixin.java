/*  Copyright 2008 Jan Kronquist.
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
package org.qi4j.entitystore.javaspaces;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.api.injection.scope.Service;
import org.qi4j.entitystore.map.MapEntityStore;
import org.qi4j.library.spaces.Space;
import org.qi4j.spi.entity.EntityType;
import org.qi4j.spi.entitystore.EntityNotFoundException;
import org.qi4j.spi.entitystore.EntityStoreException;

/**
 * Java Spaces implementation of EntityStore.
 */
public class JavaSpacesEntityStoreMixin
    implements MapEntityStore
{
    @Service
    private Space space;

    public Reader get( EntityReference entityReference )
        throws EntityStoreException
    {
        String id = entityReference.identity();
        String jsonData = space.readIfExists( id );
        if( jsonData == null )
        {
            throw new EntityNotFoundException( entityReference );
        }
        return new StringReader( jsonData );
    }

    public <ThrowableType extends Exception> void visitMap( MapEntityStoreVisitor<ThrowableType> visitor )
        throws ThrowableType
    {
        for( String json : space )
        {
            Reader state = new StringReader( json );
            visitor.visitEntity( state );
        }
    }

    public void applyChanges( MapChanges changes )
        throws IOException
    {
        try
        {
            changes.visitMap( new MapChanger()
            {
                public Writer newEntity( final EntityReference ref, EntityType entityType )
                    throws IOException
                {
                    return new StringWriter( 1000 )
                    {
                        @Override
                        public void close()
                            throws IOException
                        {
                            super.close();
                            String stateData = toString();
                            String indexKey = ref.toString();
                            space.takeIfExists( indexKey );
                            space.write( indexKey, stateData );
                        }
                    };
                }

                public Writer updateEntity( final EntityReference ref, EntityType entityType )
                    throws IOException
                {
                    return new StringWriter( 1000 )
                    {
                        @Override
                        public void close()
                            throws IOException
                        {
                            super.close();
                            String stateData = toString();
                            String indexKey = ref.toString();
                            space.takeIfExists( indexKey );
                            space.write( indexKey, stateData );
                        }
                    };
                }

                public void removeEntity( EntityReference ref, EntityType entityType )
                    throws EntityNotFoundException
                {
                    String indexKey = ref.toString();
                    space.takeIfExists( indexKey );
                }
            } );
        }
        catch( Exception e )
        {
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
