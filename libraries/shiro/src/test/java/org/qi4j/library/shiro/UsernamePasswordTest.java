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
package org.qi4j.library.shiro;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.qi4j.api.common.Visibility;
import org.qi4j.api.concern.Concerns;
import org.qi4j.api.entity.EntityBuilder;
import org.qi4j.api.entity.EntityComposite;
import org.qi4j.api.injection.scope.Structure;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.property.Property;
import org.qi4j.api.query.Query;
import org.qi4j.api.query.QueryBuilder;
import org.qi4j.api.query.QueryBuilderFactory;
import org.qi4j.api.service.ServiceComposite;
import static org.qi4j.api.query.QueryExpressions.*;
import org.qi4j.api.service.ServiceReference;
import org.qi4j.api.unitofwork.UnitOfWork;
import org.qi4j.api.unitofwork.UnitOfWorkCompletionException;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.entitystore.memory.MemoryEntityStoreService;
import org.qi4j.index.rdf.assembly.RdfMemoryStoreAssembler;
import org.qi4j.library.shiro.annotations.RequiresPermissions;
import org.qi4j.library.shiro.annotations.RequiresPermissionsConcern;
import org.qi4j.library.shiro.annotations.RequiresRoles;
import org.qi4j.library.shiro.annotations.RequiresRolesConcern;
import org.qi4j.library.shiro.annotations.RequiresUser;
import org.qi4j.library.shiro.annotations.RequiresUserConcern;
import org.qi4j.library.shiro.domain.Permission;
import org.qi4j.library.shiro.domain.Role;
import org.qi4j.library.shiro.domain.RoleAssignee;
import org.qi4j.library.shiro.domain.RoleAssignment;
import org.qi4j.library.shiro.domain.SecureHashFactory;
import org.qi4j.library.shiro.domain.SecureHashSecurable;
import org.qi4j.library.shiro.domain.ShiroDomainAssembler;
import org.qi4j.library.shiro.lifecycle.ShiroLifecycleAssembler;
import org.qi4j.library.shiro.realms.AbstractSecureHashQi4jRealm;
import org.qi4j.spi.uuid.UuidIdentityGeneratorService;
import org.qi4j.test.AbstractQi4jTest;

/**
 * A unit test showing how to use Shiro in Qi4j Applications with Username & Password credentials.
 *
 * This unit test use the provided domain Composites and Fragments to use secure password hashing as recommended
 * by the PKCS#5 standard: SHA-256 algorithm, 1000 iterations, random 64bit integer salt.
 * 
 * @author Paul Merlin <paul@nosphere.org>
 */
public class UsernamePasswordTest
        extends AbstractQi4jTest
{

    public static final String LAYER = "Layer 1";
    public static final String MODULE = "Module 1";
    public static final String TEST_USERNAME = "root";
    public static final char[] TEST_PASSWORD = "secret".toCharArray();
    public static final String TEST_PERMISSION = "gizmo";
    public static final String TEST_ROLE = "admin";

    public void assemble( ModuleAssembly module )
            throws AssemblyException
    {
        // Domain & Custom Realm
        module.addEntities( UserEntity.class );
        module.addServices( SecuredService.class );
        module.addObjects( CustomRealm.class ); // Indirectly implements RealmActivator, used by the ShiroLifecycleService

        // Shiro Domain & Lifecycle
        new ShiroDomainAssembler().assemble( module );
        new ShiroLifecycleAssembler().assemble( module );

        // EntityStore & co
        module.addServices( MemoryEntityStoreService.class, UuidIdentityGeneratorService.class );
        new RdfMemoryStoreAssembler( null, Visibility.module, Visibility.module ).assemble( module );
    }

    @Before
    public void before()
            throws UnitOfWorkCompletionException
    {
        // Create Test User
        UnitOfWork uow = unitOfWorkFactory.newUnitOfWork();

        ServiceReference<SecureHashFactory> secureHashFactoryRef = serviceLocator.findService( SecureHashFactory.class );
        SecureHashFactory secureHashFactory = secureHashFactoryRef.get();

        EntityBuilder<Permission> permissionBuilder = uow.newEntityBuilder( Permission.class );
        Permission permission = permissionBuilder.instance();
        permission.string().set( TEST_PERMISSION );
        permission = permissionBuilder.newInstance();

        EntityBuilder<Role> roleBuilder = uow.newEntityBuilder( Role.class );
        Role role = roleBuilder.instance();
        role.name().set( TEST_ROLE );
        role.permissions().add( permission );
        role = roleBuilder.newInstance();

        EntityBuilder<UserEntity> userBuilder = uow.newEntityBuilder( UserEntity.class );
        UserEntity user = userBuilder.instance();
        user.username().set( TEST_USERNAME );
        user.secureHash().set( secureHashFactory.create( TEST_PASSWORD ) );
        user = userBuilder.newInstance();

        EntityBuilder<RoleAssignment> roleAssignmentBuilder = uow.newEntityBuilder( RoleAssignment.class );
        RoleAssignment roleAssignment = roleAssignmentBuilder.instance();
        roleAssignment.role().set( role );
        user.roleAssignments().add( roleAssignment );
        roleAssignment.assignee().set( user );
        roleAssignment = roleAssignmentBuilder.newInstance();

        uow.complete();
    }

    @Test
    public void test()
    {
        ServiceReference<SecuredService> ref = serviceLocator.findService( SecuredService.class );
        SecuredService secured = ref.get();
        try {
            secured.doSomethingThatRequiresUser();
            fail( "We're not authenticated, must fail" );
        } catch ( UnauthenticatedException ex ) {
        }
        try {
            secured.doSomethingThatRequiresPermissions();
            fail( "We're not authenticated, must fail" );
        } catch ( UnauthorizedException ex ) {
        }
        try {
            secured.doSomethingThatRequiresRoles();
            fail( "We're not authenticated, must fail" );
        } catch ( UnauthorizedException ex ) {
        }
        SecurityUtils.getSubject().login( new UsernamePasswordToken( TEST_USERNAME, TEST_PASSWORD ) );
        secured.doSomethingThatRequiresUser();
        secured.doSomethingThatRequiresPermissions();
        secured.doSomethingThatRequiresRoles();
    }

    public interface UserEntity
            extends RoleAssignee, SecureHashSecurable, EntityComposite
    {

        Property<String> username();

    }

    @Mixins( SecuredService.Mixin.class )
    @Concerns( { RequiresUserConcern.class, RequiresPermissionsConcern.class, RequiresRolesConcern.class } )
    public interface SecuredService
            extends ServiceComposite
    {

        @RequiresUser
        void doSomethingThatRequiresUser();

        @RequiresPermissions( UsernamePasswordTest.TEST_PERMISSION )
        void doSomethingThatRequiresPermissions();

        @RequiresRoles( UsernamePasswordTest.TEST_ROLE )
        void doSomethingThatRequiresRoles();

        abstract class Mixin
                implements SecuredService
        {

            public void doSomethingThatRequiresUser()
            {
                System.out.println( "Doing something that requires a valid user" );
            }

            public void doSomethingThatRequiresPermissions()
            {
                System.out.println( "Doing something that requires permissions" );
            }

            public void doSomethingThatRequiresRoles()
            {
                System.out.println( "Doing something that requires roles" );
            }

        }

    }

    public static class CustomRealm
            extends AbstractSecureHashQi4jRealm
    {

        @Structure
        private QueryBuilderFactory qbf;

        public CustomRealm()
        {
            super();
            setName( CustomRealm.class.getSimpleName() );
        }

        @Override
        protected SecureHashSecurable getSecureHashSecurable( String username )
        {
            return findUserEntityByUsername( username );
        }

        @Override
        protected RoleAssignee getRoleAssignee( String username )
        {
            return findUserEntityByUsername( username );
        }

        private UserEntity findUserEntityByUsername( String username )
        {
            UnitOfWork uow = uowf.currentUnitOfWork();
            QueryBuilder<UserEntity> queryBuilder = qbf.newQueryBuilder( UserEntity.class );
            UserEntity userTemplate = templateFor( UserEntity.class );
            queryBuilder = queryBuilder.where( eq( userTemplate.username(), username ) );
            Query<UserEntity> query = queryBuilder.newQuery( uow ).maxResults( 1 );
            return query.iterator().next();
        }

    }

}
