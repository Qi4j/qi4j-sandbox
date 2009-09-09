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

package org.qi4j.entitystore.coherence;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import org.qi4j.api.configuration.Configuration;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.injection.scope.Uses;
import org.qi4j.api.service.Activatable;
import org.qi4j.entitystore.map.MapEntityStore;
import org.qi4j.spi.entity.EntityNotFoundException;
import org.qi4j.spi.entity.EntityStoreException;
import org.qi4j.spi.entity.EntityType;
import org.qi4j.spi.service.ServiceDescriptor;

public class CoherenceEntityStoreMixin
    implements Activatable, MapEntityStore, DatabaseExport, DatabaseImport
{
    @This private ReadWriteLock lock;
    @This private Configuration<CoherenceConfiguration> config;
    @Uses private ServiceDescriptor descriptor;

    private NamedCache cache;

    // Activatable implementation
    public void activate()
        throws Exception
    {
        String cacheName = config.configuration().cacheName().get();
        cache = CacheFactory.getCache( cacheName );
    }

    public void passivate()
        throws Exception
    {
        cache.destroy();
    }

    public Reader get( EntityReference entityReference ) throws EntityStoreException
    {
        byte[] data = (byte[]) cache.get( entityReference.identity() );

        if( data == null )
        {
            throw new EntityNotFoundException( entityReference );
        }
        try
        {
            return new StringReader( new String( data, "UTF-8" ) );
        }
        catch( UnsupportedEncodingException e )
        {
            // Can not happen.
            throw new InternalError();
        }
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
                            cache.put( ref.identity(), stateArray );
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
                            cache.put( ref.identity(), stateArray );
                        }
                    };
                }

                public void removeEntity( EntityReference ref, EntityType entityType ) throws EntityNotFoundException
                {
                    cache.remove( ref.identity() );
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


    public void visitMap( MapEntityStoreVisitor visitor )
    {
        Iterator<Map.Entry<String, byte[]>> list = cache.entrySet().iterator();
        while( list.hasNext() )
        {
            Map.Entry<String, byte[]> entry = list.next();
            String id = entry.getKey();
            byte[] data = entry.getValue();
            try
            {
                visitor.visitEntity( new StringReader( new String( data, "UTF-8" ) ) );
            }
            catch( UnsupportedEncodingException e )
            {
                // Can not happen!
            }
        }
    }

    public void exportTo( Writer out )
        throws IOException
    {
        Iterator<Map.Entry<String, byte[]>> list = cache.entrySet().iterator();
        while( list.hasNext() )
        {
            Map.Entry<String, byte[]> entry = list.next();
            byte[] data = entry.getValue();
            String value = new String( data, "UTF-8" );
            out.write( value );
            out.write( '\n' );
        }
    }

    public void importFrom( Reader in ) throws IOException
    {
        BufferedReader reader = new BufferedReader( in );
        String object;
        while( ( object = reader.readLine() ) != null )
        {
            String id = object.substring( "{\"identity\":\"".length() );
            id = id.substring( 0, id.indexOf( '"' ) );
            byte[] stateArray = object.getBytes( "UTF-8" );
            cache.put( id, stateArray );
        }
    }
}