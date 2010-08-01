/*
 * Copyright (c) 2010, Paul Merlin. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.qi4j.entitystore.jclouds;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.BlobStoreContextFactory;
import org.jclouds.blobstore.InputStreamMap;

import org.qi4j.api.entity.EntityReference;
import org.qi4j.api.service.Activatable;
import org.qi4j.api.unitofwork.NoSuchEntityException;
import org.qi4j.entitystore.map.MapEntityStore;
import org.qi4j.spi.entity.EntityType;
import org.qi4j.spi.entitystore.EntityNotFoundException;
import org.qi4j.spi.entitystore.EntityStoreException;

public class JCloudsMapEntityStoreMixin
        implements Activatable, MapEntityStore
{

    private static final String CONTAINER_NAME = "qi4j-data";

    private BlobStoreContext blobStoreContext;

    private InputStreamMap map;

    public void activate()
            throws Exception
    {
        blobStoreContext = new BlobStoreContextFactory().createContext( "transient", "", "" );
        if ( !blobStoreContext.getBlobStore().createContainerInLocation( null, CONTAINER_NAME ) ) {
            throw new EntityStoreException( "Container '" + CONTAINER_NAME + "' did not exists in BlobStore and could not be created." );
        }
        map = blobStoreContext.createInputStreamMap( CONTAINER_NAME );
    }

    public void passivate()
            throws Exception
    {
        blobStoreContext.close();
    }

    public Reader get( EntityReference er )
            throws EntityStoreException
    {
        InputStream is = null;
        try {
            is = map.get( er.identity() );
            if ( is == null ) {
                throw new NoSuchEntityException( er );
            }
            return new StringReader( readInputStream( is ).toString() );
        } finally {
            if ( is != null ) {
                try {
                    is.close();
                } catch ( IOException ignored ) {
                }
            }
        }
    }

    public void visitMap( MapEntityStoreVisitor visitor )
    {
        for ( Map.Entry<String, InputStream> eachEntry : map.entrySet() ) {
            InputStream is = eachEntry.getValue();
            try {
                visitor.visitEntity( new StringReader( readInputStream( is ).toString() ) );
            } finally {
                if ( is != null ) {
                    try {
                        is.close();
                    } catch ( IOException ignored ) {
                    }
                }
            }
        }
    }

    public void applyChanges( MapChanges changes )
            throws IOException
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
                        map.putString( ref.identity(), toString() );
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
                        map.putString( ref.identity(), toString() );
                    }

                };
            }

            public void removeEntity( EntityReference ref, EntityType entityType )
                    throws EntityNotFoundException
            {
                map.remove( ref.identity() );
            }

        } );
    }

    private CharSequence readInputStream( InputStream is )
    {
        Reader in = null;
        try {
            final char[] buffer = new char[ 0x10000 ]; // 64K Blocks
            StringBuilder out = new StringBuilder();
            in = new InputStreamReader( is, "UTF-8" );
            int read;
            do {
                read = in.read( buffer, 0, buffer.length );
                if ( read > 0 ) {
                    out.append( buffer, 0, read );
                }
            } while ( read >= 0 );
            return out;
        } catch ( IOException ex ) {
            throw new EntityStoreException( "Unable to read data from BlobStore", ex );
        } finally {
            try {
                if ( in != null ) {
                    in.close();
                }
            } catch ( IOException ignored ) {
            }
        }
    }

}
