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

import org.qi4j.api.common.Optional;
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.property.Property;
import org.qi4j.api.unitofwork.UnitOfWorkPropagation;
import org.qi4j.api.unitofwork.UnitOfWorkPropagation.Propagation;

/**
 *
 * @author Stanislav Muhametsin
 */
@Mixins({
    SomeRole.SomeRoleMixin.class
})
public interface SomeRole
{

    public String getMyString();

    public void setMyString(String str);

    public interface SomeRoleState
    {
        @Optional
        Property<String> myString();
    }

    public class SomeRoleMixin implements SomeRole
    {

        @This private SomeRoleState _state;

        @Override
        public String getMyString()
        {
            return this._state.myString().get();
        }

        @Override
        public void setMyString( String str )
        {
            this._state.myString().set( str );
        }

    }

    // The reason why proxy mixins are not generic is that sometimes, for example, entity may return the value of @This-injected variable,
    // which is not instance of ProxyableEntity, but still an entity. Then a full control is needed.
    public class SomeRoleProxyMixin implements SomeRole
    {
        @This private EntityProxy _proxy;

        @Override
        @UnitOfWorkPropagation(Propagation.REQUIRED)
        public String getMyString()
        {
            return this._proxy.getEntity( SomeRole.class ).getMyString();
        }

        @Override
        @UnitOfWorkPropagation(Propagation.REQUIRED)
        public void setMyString( String str )
        {
            this._proxy.getEntity( SomeRole.class ).setMyString( str );
        }
    }
}
