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
package org.qi4j.library.jini.lookup;

import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.DiscoveryManagement;
import net.jini.discovery.LookupDiscoveryManager;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryEvent;
import net.jini.lookup.ServiceDiscoveryListener;
import net.jini.lookup.ServiceDiscoveryManager;
import net.jini.security.policy.DynamicPolicyProvider;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.qi4j.api.injection.scope.Service;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.entitystore.memory.MemoryEntityStoreService;
import org.qi4j.library.http.JettyServiceAssembler;
import org.qi4j.test.AbstractQi4jTest;

import java.io.IOException;
import java.security.*;
import java.util.Collection;
import java.util.logging.*;

public class ReggieStartTest extends AbstractQi4jTest
{
    private static Logger logger = Logger.getLogger( "" );

    static
    {
        Handler[] handlers = logger.getHandlers();
        for( Handler handler : handlers )
        {
            Formatter formatter = new SimpleFormatter();
            handler.setFormatter( formatter );
        }
        logger.setLevel( Level.FINEST );
        if( System.getSecurityManager() == null )
        {
            Policy basePolicy = new AllPolicy();
            DynamicPolicyProvider policyProvider = new DynamicPolicyProvider( basePolicy );
            Policy.setPolicy( policyProvider );
            System.setSecurityManager( new SecurityManager() );
        }
    }


    public void assemble( ModuleAssembly module ) throws AssemblyException
    {
        new JettyServiceAssembler().assemble( module );
        module.addObjects( Holder.class );
        module.addServices( MemoryEntityStoreService.class );
        new JiniLookupServiceAssembler().assemble( module );
    }

    @Test
    public void whenStartingReggieExpectFoundServiceRegistrar()
        throws Exception
    {
        LookupCache cache = initialize();
        MyServiceDiscoveryListener listener = new MyServiceDiscoveryListener();
        cache.addListener( listener );
        Holder object = objectBuilderFactory.newObject( Holder.class );
        synchronized( this )
        {
            if( !listener.added )
            {
                wait( 25000 );
            }
        }
        synchronized( listener )
        {
            assertTrue( listener.added );
        }
    }


    private LookupCache initialize()
        throws IOException
    {
        DiscoveryManagement discoveryManager = new LookupDiscoveryManager( null, null, new MyDiscoveryListener() );
        ServiceDiscoveryManager sdm = new ServiceDiscoveryManager( discoveryManager, null );
        Class[] types = new Class[]{ ServiceRegistrar.class };
        ServiceTemplate template = new ServiceTemplate( null, types, null );
        LookupCache lookupCache = sdm.createLookupCache( template, null, null );
        return lookupCache;

    }

    public static class Holder
    {
        @Service ServiceRegistryService service;
    }

    private static class MyDiscoveryListener
        implements DiscoveryListener
    {

        public void discovered( DiscoveryEvent e )
        {
            printEvent( e, "Discovered: " );
        }

        public void discarded( DiscoveryEvent e )
        {
            printEvent( e, "Discarded: " );
        }

        private void printEvent( DiscoveryEvent e, String message )
        {
            Collection<String[]> collection = e.getGroups().values();
            for( String[] array : collection )
            {
                StringBuffer groups = new StringBuffer();
                boolean first = true;
                for( String group : array )
                {
                    if( !first )
                    {
                        groups.append( "," );
                    }
                    first = false;
                    groups.append( group );
                    System.out.println( message + groups );
                }
            }
        }
    }

    private class MyServiceDiscoveryListener
        implements ServiceDiscoveryListener
    {
        boolean added = false;
        boolean removed = false;

        public MyServiceDiscoveryListener()
        {
        }

        public void serviceAdded( ServiceDiscoveryEvent event )
        {
            synchronized( ReggieStartTest.this )
            {
                logger.info( "Added: " + event.getPostEventServiceItem() );
                added = true;
                ReggieStartTest.this.notifyAll();
            }
        }

        public void serviceRemoved( ServiceDiscoveryEvent event )
        {
            synchronized( ReggieStartTest.this )
            {
                logger.info( "Removed: " + event.getPostEventServiceItem() );
                removed = true;
                ReggieStartTest.this.notifyAll();
            }
        }

        public void serviceChanged( ServiceDiscoveryEvent event )
        {
        }
    }

    public static class AllPolicy extends Policy
    {

        public AllPolicy()
        {
        }

        public PermissionCollection getPermissions( CodeSource codeSource )
        {
            Permissions allPermission;
            allPermission = new Permissions();
            allPermission.add( new AllPermission() );
            return allPermission;
        }

        public void refresh()
        {
        }
    }
}
