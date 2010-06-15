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
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.property.Immutable;
import org.qi4j.api.property.Property;
import org.qi4j.api.unitofwork.UnitOfWorkConcern;
import org.qi4j.api.unitofwork.UnitOfWorkFactory;
import org.qi4j.api.unitofwork.UnitOfWorkPropagation;
import org.qi4j.api.unitofwork.UnitOfWorkPropagation.Propagation;

@Mixins({
    EntityProxy.EntityProxyMixin.class
})
@Concerns({
    UnitOfWorkConcern.class
})
public interface EntityProxy
{

    String getEntityID();

    Class<?> getCommonClass();

    <EntityType> EntityType getEntity( Class<EntityType> entityClass );

    public interface EntityProxyState
    {
        @Immutable
        Property<String> entityID();

        @Immutable
        Property<Class<?>> commonClass();
    }

    public abstract class EntityProxyMixin
        implements EntityProxy
    {

        @This
        private EntityProxyState _state;

        @This
        private EntityProxy _meAsProxy;

        @Structure private UnitOfWorkFactory _uowf;

        @Override
        public String getEntityID()
        {
            return this._state.entityID().get();
        }

        @Override
        public Class<?> getCommonClass()
        {
            return this._state.commonClass().get();
        }

        @Override
        @UnitOfWorkPropagation(Propagation.REQUIRED)
        public <EntityType> EntityType getEntity( Class<EntityType> entityClass )
        {
            return this._uowf.currentUnitOfWork().get( entityClass, this._meAsProxy.getEntityID() );
        }

    }
}
