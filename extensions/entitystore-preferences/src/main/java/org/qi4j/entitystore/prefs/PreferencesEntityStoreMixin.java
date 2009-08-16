/*
 * Copyright (c) 2008, Rickard Öberg. All Rights Reserved.
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

package org.qi4j.entitystore.prefs;

import java.util.prefs.Preferences;
import org.qi4j.api.common.MetaInfo;
import org.qi4j.api.injection.scope.Structure;
import org.qi4j.api.injection.scope.Uses;
import org.qi4j.api.service.Activatable;
import org.qi4j.api.structure.Application;
import org.qi4j.api.usecase.Usecase;
import org.qi4j.spi.entity.EntityStore;
import org.qi4j.spi.service.ServiceDescriptor;
import org.qi4j.spi.structure.ModuleSPI;
import org.qi4j.spi.unitofwork.EntityStoreUnitOfWork;

/**
 * Implementation of EntityStore that is backed by the Preferences API.
 *
 * @see Preferences
 */
public class PreferencesEntityStoreMixin
    implements Activatable, EntityStore
{
    private @Uses ServiceDescriptor descriptor;
    private @Structure Application application;

    private Preferences root;

    public void activate()
        throws Exception
    {
        root = getApplicationRoot();
    }

    private Preferences getApplicationRoot()
    {
        MetaInfo metaInfo = descriptor.metaInfo();
        PreferenceEntityStoreInfo storeInfo = metaInfo.get( PreferenceEntityStoreInfo.class );

        Preferences preferences;
        if( storeInfo == null )
        {
            // Default to use system root
            preferences = Preferences.systemRoot();
        }
        else
        {
            PreferenceEntityStoreInfo.PreferenceNode rootNode = storeInfo.getRootNode();
            preferences = rootNode.getNode();
        }

        String name = application.name();
        return preferences.node( name );
    }

    public void passivate()
        throws Exception
    {
    }

    public EntityStoreUnitOfWork newUnitOfWork( Usecase usecase, MetaInfo unitOfWorkMetaInfo, ModuleSPI module )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public EntityStoreUnitOfWork visitEntityStates( EntityStateVisitor visitor, ModuleSPI moduleInstance )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
