/*
 * Copyright (c) 2010, Stanislav Muhametsin. All Rights Reserved.
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

package org.qi4j.library.entityproxy;

import org.qi4j.api.concern.Concerns;
import org.qi4j.api.injection.scope.Structure;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.service.ServiceComposite;
import org.qi4j.api.unitofwork.UnitOfWorkConcern;
import org.qi4j.api.unitofwork.UnitOfWorkFactory;
import org.qi4j.api.unitofwork.UnitOfWorkPropagation;
import org.qi4j.api.unitofwork.UnitOfWorkPropagation.Propagation;

/**
 *
 * @author Stanislav Muhametsin
 */
public interface EntityProxyHelper
{

    public <EType, ReturnType> ReturnType getEntity( Class<ReturnType> entityClass, EType proxyOrEntity );

    public <PType, ReturnType> ReturnType getProxy( Class<ReturnType> proxyClass, PType proxyOrEntity );

    @Mixins({EntityProxyHelperMixin.class})
    @Concerns({UnitOfWorkConcern.class})
    public interface EntityProxyHelperService extends EntityProxyHelper, ServiceComposite
    {

    }

    public abstract class EntityProxyHelperMixin
        implements EntityProxyHelper
    {
        @Structure private UnitOfWorkFactory _uowf;


        @Override
        @UnitOfWorkPropagation(Propagation.REQUIRED)
        public <EType, ReturnType> ReturnType getEntity( Class<ReturnType> entityClass, EType proxyOrEntity )
        {
            return proxyOrEntity instanceof EntityProxy ? ((EntityProxy) proxyOrEntity).getEntity( entityClass )
                : entityClass.cast( this._uowf.currentUnitOfWork().get( proxyOrEntity ));
        }

        @Override
        @UnitOfWorkPropagation(Propagation.REQUIRED)
        public <PType, ReturnType> ReturnType getProxy( Class<ReturnType> proxyClass, PType proxyOrEntity )
        {
            return proxyOrEntity instanceof ProxyableEntity ? ((ProxyableEntity) this._uowf.currentUnitOfWork().get( proxyOrEntity )).getProxy( proxyClass )
                : proxyClass.cast( proxyOrEntity );
        }
    }
}
