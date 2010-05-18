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

package org.qi4j.entitystore.jndi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import org.qi4j.api.common.QualifiedName;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.api.structure.Module;
import org.qi4j.api.usecase.Usecase;
import org.qi4j.spi.entity.EntityDescriptor;
import org.qi4j.spi.entity.EntityState;
import org.qi4j.spi.entity.EntityStatus;
import org.qi4j.spi.entity.EntityType;
import org.qi4j.spi.entity.association.AssociationType;
import org.qi4j.spi.entity.association.ManyAssociationType;
import org.qi4j.spi.entitystore.EntityNotFoundException;
import org.qi4j.spi.entitystore.EntityStoreException;
import org.qi4j.spi.entitystore.EntityStoreUnitOfWork;
import org.qi4j.spi.entitystore.ReadOnlyEntityStoreException;
import org.qi4j.spi.entitystore.StateCommitter;
import org.qi4j.spi.property.PropertyType;
import org.qi4j.spi.structure.ModuleSPI;

public class JndiUow implements EntityStoreUnitOfWork
{
    private static final ArrayList<String> RESTRICTED_PROPERTIES = new ArrayList<String>();

    static
    {
        RESTRICTED_PROPERTIES.add( "identity" );
    }

    private Usecase usecase;
    private Module module;
    private String uowIdentity;
    private JndiSetup setup;

    public JndiUow( JndiSetup setup, Usecase usecase, Module module )
    {
        this.setup = setup;
        uowIdentity = UUID.randomUUID().toString();
        this.usecase = usecase;
        this.module = module;
    }

    public String identity()
    {
        return uowIdentity;
    }

    public EntityState newEntityState( EntityReference anIdentity, EntityDescriptor entityDescriptor )
        throws EntityStoreException
    {
        throw new ReadOnlyEntityStoreException( "JndiEntityStore is read-only." );
    }

    public EntityState getEntityState( EntityReference identity )
        throws EntityStoreException, EntityNotFoundException
    {
        try
        {
            String id = identity.identity();
            Attributes attrs = lookup( id );

            String version = Long.toString( getVersion( attrs ) );
            long lastModified = getLastModified( attrs );
            EntityStatus status = EntityStatus.LOADED;
            EntityDescriptor descriptor = ((ModuleSPI) module).entityDescriptor( getType( attrs ) );
            EntityType entityType = descriptor.entityType();
            Map<QualifiedName, Object> properties = getProperties( attrs, entityType );
            Map<QualifiedName, EntityReference> associations = getAssociations( attrs, entityType );
            Map<QualifiedName, List<EntityReference>> manyAssocations = getManyAssociations( attrs, entityType );
            return new JndiEntityState( this,
                                        version,
                                        lastModified,
                                        identity,
                                        status,
                                        descriptor,
                                        properties,
                                        associations,
                                        manyAssocations );
        }
        catch( NamingException e )
        {
            throw new EntityStoreException( e );
        }
    }

    public StateCommitter applyChanges()
        throws EntityStoreException
    {
        return new StateCommitter()
        {
            public void commit()
            {
            }

            public void cancel()
            {
            }
        };
    }

    public void discard()
    {
    }

    private Attributes lookup( String id )
        throws NamingException
    {
        // TODO: Caching
        LdapName dn = new LdapName( setup.identityAttribute + "=" + id + "," + setup.baseDn );
        return setup.context.getAttributes( dn );
    }


    private String getType( Attributes attrs )
        throws NamingException
    {
        Attribute typeAttr = attrs.get( setup.qualifiedTypeAttribute );
        if( typeAttr == null )
        {
            return null;
        }
        return (String) typeAttr.get();
    }

    private long getLastModified( Attributes attrs )
        throws NamingException
    {
        Attribute lastModifiedAttr = attrs.get( setup.lastModifiedDateAttribute );
        if( lastModifiedAttr == null )
        {
            return -1;
        }
        String lastModifiedValue = (String) lastModifiedAttr.get();
        return Long.parseLong( lastModifiedValue );
    }

    private long getVersion( Attributes attrs )
        throws NamingException
    {
        Attribute versionAttr = attrs.get( setup.instanceVersionAttribute );
        if( versionAttr == null )
        {
            return -1;
        }
        String versionValue = (String) versionAttr.get();
        return Long.parseLong( versionValue );
    }


    private Map<QualifiedName, Object> getProperties( Attributes attrs, EntityType entityType )
        throws NamingException
    {
        Map<QualifiedName, Object> result = new HashMap<QualifiedName, Object>();
        Iterable<PropertyType> props = entityType.properties();
        for( PropertyType property : props )
        {
            QualifiedName qualifiedName = property.qualifiedName();
            String propertyName = qualifiedName.name();
            if( !RESTRICTED_PROPERTIES.contains( propertyName ) )
            {
                Attribute attribute = attrs.get( propertyName );
                if( attribute != null )
                {
                    result.put( qualifiedName, attribute.get() );
                }
            }
        }
        return result;
    }

    private Map<QualifiedName, EntityReference> getAssociations( Attributes attrs, EntityType entityType )
        throws NamingException
    {
        Map<QualifiedName, EntityReference> result = new HashMap<QualifiedName, EntityReference>();
        Iterable<AssociationType> assocs = entityType.associations();
        for( AssociationType associationType : assocs )
        {
            QualifiedName qualifiedName = associationType.qualifiedName();
            String associationName = qualifiedName.name();
            Attribute attribute = attrs.get( associationName );
            String identity = (String) attribute.get();
            EntityReference entityReference = EntityReference.getEntityReference( identity );
            result.put( qualifiedName, entityReference );
        }
        return result;
    }

    private Map<QualifiedName, List<EntityReference>> getManyAssociations( Attributes attrs, EntityType entityType )
        throws NamingException
    {
        Map<QualifiedName, List<EntityReference>> result = new HashMap<QualifiedName, List<EntityReference>>();
        Iterable<ManyAssociationType> assocs = entityType.manyAssociations();
        for( ManyAssociationType associationType : assocs )
        {
            QualifiedName qualifiedName = associationType.qualifiedName();
            String associationName = qualifiedName.name();
            Attribute attribute = attrs.get( associationName );
            String identity = (String) attribute.get();
            EntityReference entityRef = new EntityReference( identity );
            List<EntityReference> entry = result.get( qualifiedName );
            if( entry == null )
            {
                entry = new ArrayList<EntityReference>();
                result.put( qualifiedName, entry );
            }
            entry.add( entityRef );
        }
        return result;
    }


}
