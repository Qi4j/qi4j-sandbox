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
package org.qi4j.library.ldap.server;

import java.util.HashSet;
import java.util.Set;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.qi4j.api.configuration.Configuration;
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.service.Activatable;
import org.qi4j.api.service.ServiceComposite;

public class ApacheDirectoryServiceMixin
    implements Activatable, Ldap
{
    @This private Configuration<LdapConfiguration> configuration;

    /**
     * The directory service
     */
    private DirectoryService service;
    private boolean running;

    public ApacheDirectoryServiceMixin()
    {
        System.out.println( "Starting LDAP." );
    }

    public void activate()
        throws Exception
    {
        // Initialize the LDAP service
        service = new DefaultDirectoryService();

        // Disable the ChangeLog system
        service.getChangeLog().setEnabled( false );


        LdapConfiguration conf = configuration.configuration();
        String partitionId = conf.partitionId().get();
        Partition partition = addPartition( partitionId, conf.partitionDn().get() );

        // Index some attributes on the apache partition
        String[] attrs = { "objectClass", "ou", "uid" };
        addIndex( partition, attrs );
        addIndex( partition, configAttributes() );

        // Inject the apache root entry if it does not already exist
        try
        {
            service.getAdminSession().lookup( partition.getSuffixDn() );
        }
        catch( LdapNameNotFoundException lnnfe )
        {

            LdapDN dnApache = new LdapDN( "dc=Apache,dc=Org" );
            ServerEntry entryApache = service.newEntry( dnApache );
            entryApache.add( "objectClass", "top", "domain", "extensibleObject" );
            entryApache.add( "dc", "Apache" );
            service.getAdminSession().add( entryApache );
        }
        service.startup();
        running = true;
    }

    private String[] configAttributes()
    {
        String attrs = configuration.configuration().indexAttributes().get();
        return attrs.split( "," );
    }

    public void passivate()
        throws Exception
    {
        running = false;
        service.shutdown();
    }


    /**
     * Add a new partition to the server
     *
     * @param partitionId The partition Id
     * @param partitionDn The partition DN
     * @return The newly added partition
     * @throws Exception If the partition can't be added
     */
    private Partition addPartition( String partitionId, String partitionDn ) throws Exception
    {
        Partition partition = new JdbmPartition();
        partition.setId( partitionId );
        partition.setSuffix( partitionDn );
        service.addPartition( partition );

        return partition;
    }

    /**
     * Add a new set of index on the given attributes
     *
     * @param partition The partition on which we want to add index
     * @param attrs     The list of attributes to index
     */
    private void addIndex( Partition partition, String... attrs )
    {
        // Index some attributes on the apache partition
        Set indexedAttributes = new HashSet();

        for( String attribute : attrs )
        {
            indexedAttributes.add( new JdbmIndex<String, ServerEntry>( attribute ) );
        }

        ( (JdbmPartition) partition ).setIndexedAttributes( indexedAttributes );
    }

    public boolean isRunning()
    {
        return running;
    }
}
