/*
 * Copyright (c) 2010 Paul Merlin <paul@nosphere.org>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.qi4j.library.shiro.tests.username;

import org.qi4j.api.entity.EntityBuilder;
import org.qi4j.api.injection.scope.Service;
import org.qi4j.api.injection.scope.Structure;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.service.Activatable;
import org.qi4j.api.service.ServiceComposite;
import org.qi4j.api.unitofwork.UnitOfWork;
import org.qi4j.api.unitofwork.UnitOfWorkFactory;
import org.qi4j.library.shiro.domain.Permission;
import org.qi4j.library.shiro.domain.Role;
import org.qi4j.library.shiro.domain.RoleAssignment;
import org.qi4j.library.shiro.domain.SecureHashFactory;

/**
 * @author Paul Merlin <paul@nosphere.org>
 */
@Mixins( UsernameFixtures.Mixin.class )
public interface UsernameFixtures
        extends ServiceComposite, Activatable
{

    String USERNAME = "root";
    char[] PASSWORD = "secret".toCharArray();
    String PERMISSION = "gizmo";
    String ROLE = "admin";

    abstract class Mixin
            implements UsernameFixtures
    {

        @Structure
        private UnitOfWorkFactory uowf;
        @Service
        private SecureHashFactory hashFactory;

        public void activate()
                throws Exception
        {
            // Create Test User
            UnitOfWork uow = uowf.newUnitOfWork();

            EntityBuilder<Permission> permissionBuilder = uow.newEntityBuilder( Permission.class );
            Permission permission = permissionBuilder.instance();
            permission.string().set( PERMISSION );
            permission = permissionBuilder.newInstance();

            EntityBuilder<Role> roleBuilder = uow.newEntityBuilder( Role.class );
            Role role = roleBuilder.instance();
            role.name().set( ROLE );
            role.permissions().add( permission );
            role = roleBuilder.newInstance();

            EntityBuilder<UserEntity> userBuilder = uow.newEntityBuilder( UserEntity.class );
            UserEntity user = userBuilder.instance();
            user.username().set( USERNAME );
            user.secureHash().set( hashFactory.create( PASSWORD ) );
            user = userBuilder.newInstance();

            EntityBuilder<RoleAssignment> roleAssignmentBuilder = uow.newEntityBuilder( RoleAssignment.class );
            RoleAssignment roleAssignment = roleAssignmentBuilder.instance();
            roleAssignment.role().set( role );
            user.roleAssignments().add( roleAssignment );
            roleAssignment.assignee().set( user );
            roleAssignment = roleAssignmentBuilder.newInstance();

            uow.complete();
        }

        public void passivate()
                throws Exception
        {
        }

    }

}
