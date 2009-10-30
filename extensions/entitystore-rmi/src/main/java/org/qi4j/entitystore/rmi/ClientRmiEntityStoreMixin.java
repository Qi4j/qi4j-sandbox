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
package org.qi4j.entitystore.rmi;

import java.io.IOException;
import java.io.Reader;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.api.service.Activatable;
import org.qi4j.entitystore.map.MapEntityStore;
import org.qi4j.spi.entitystore.EntityStoreException;

/**
 * RMI client implementation of Entity
 */
public class ClientRmiEntityStoreMixin
    implements Activatable, MapEntityStore
{
    private RemoteEntityStore remote;

    // Activatable implementation
    public void activate()
        throws Exception
    {
        Registry registry = LocateRegistry.getRegistry( "localhost" );
        remote = (RemoteEntityStore) registry.lookup( ServerRmiEntityStoreService.class.getSimpleName() );
    }

    public void passivate()
        throws Exception
    {
        remote = null;
    }

    public Reader get( EntityReference entityReference )
        throws EntityStoreException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void visitMap( MapEntityStoreVisitor visitor )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void applyChanges( MapChanges changes )
        throws IOException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}