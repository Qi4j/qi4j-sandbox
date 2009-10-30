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

import org.qi4j.spi.entity.EntityState;
import org.qi4j.spi.entity.EntityStatus;
import org.qi4j.spi.entity.EntityDescriptor;
import org.qi4j.spi.entity.ManyAssociationState;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.api.common.TypeName;
import org.qi4j.api.common.QualifiedName;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

public class JndiEntityState
    implements EntityState
{
    private JndiUow unitOfWork;
    protected EntityStatus status;
    protected String version;
    protected long lastModified;
    private final EntityReference identity;

    private final EntityDescriptor entityDescriptor;
    protected final Map<QualifiedName, Object> properties;
    protected final Map<QualifiedName, EntityReference> associations;
    protected final Map<QualifiedName, List<EntityReference>> manyAssociations;

    public JndiEntityState( JndiUow unitOfWork, EntityReference identity, EntityDescriptor entityDescriptor)
    {
        this( unitOfWork, "",
              System.currentTimeMillis(),
              identity,
              EntityStatus.NEW,
                entityDescriptor,
              new HashMap<QualifiedName, Object>(),
              new HashMap<QualifiedName, EntityReference>(),
              new HashMap<QualifiedName, List<EntityReference>>() );
    }

    public JndiEntityState( JndiUow unitOfWork,
                               String version,
                               long lastModified,
                               EntityReference identity,
                               EntityStatus status,
                               EntityDescriptor entityDescriptor,
                               Map<QualifiedName, Object> properties,
                               Map<QualifiedName, EntityReference> associations,
                               Map<QualifiedName, List<EntityReference>> manyAssociations )
    {
        this.unitOfWork = unitOfWork;
        this.version = version;
        this.lastModified = lastModified;
        this.identity = identity;
        this.status = status;
        this.entityDescriptor = entityDescriptor;
        this.properties = properties;
        this.associations = associations;
        this.manyAssociations = manyAssociations;
    }


    public EntityReference identity()
    {
        return identity;
    }

    public String version()
    {
        return version;
    }

    public long lastModified()
    {
        return lastModified;
    }

    public void remove()
    {
    }

    public EntityStatus status()
    {
        return status;
    }

    public boolean isOfType( TypeName type )
    {
        return false;
    }

    public EntityDescriptor entityDescriptor()
    {
        return entityDescriptor;
    }

    public Object getProperty( QualifiedName stateName )
    {
        return properties.get( stateName );
    }

    public void setProperty( QualifiedName stateName, Object json )
    {
        properties.put( stateName, json );
    }

    public EntityReference getAssociation( QualifiedName stateName )
    {
        return associations.get( stateName );
    }

    public void setAssociation( QualifiedName stateName, EntityReference newEntity )
    {
        associations.put( stateName, newEntity );
    }

    public ManyAssociationState getManyAssociation( QualifiedName stateName )
    {
        return null;
    }

    public void refresh()
    {
    }

    public void hasBeenApplied()
    {
        status = EntityStatus.LOADED;
        version = unitOfWork.identity();
    }

    @Override
    public String toString()
    {
        return identity + "(" +
               properties.size() + " properties, " +
               associations.size() + " associations, " +
               manyAssociations.size() + " many-associations)";
    }

}
