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
package org.qi4j.library.shiro.usernamepassword;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.config.Ini;
import org.apache.shiro.config.Ini.Section;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.crypto.hash.Sha256Hash;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.qi4j.api.common.Visibility;
import org.qi4j.api.entity.EntityBuilder;
import org.qi4j.api.service.ServiceReference;
import org.qi4j.api.unitofwork.UnitOfWork;
import org.qi4j.api.unitofwork.UnitOfWorkCompletionException;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.entitystore.memory.MemoryEntityStoreService;
import org.qi4j.index.rdf.assembly.RdfMemoryStoreAssembler;
import org.qi4j.library.shiro.domain.Permission;
import org.qi4j.library.shiro.domain.Role;
import org.qi4j.library.shiro.domain.RoleAssignment;
import org.qi4j.spi.uuid.UuidIdentityGeneratorService;
import org.qi4j.test.AbstractQi4jTest;

/**
 * A unit test showing how to use Shiro in Qi4j Applications with Username & Password credentials.
 * 
 * WARNING Username is used as salt in order to use the Shiro HashedCredentialMatcher as is.
 *
 * A 64 bits random number different for each user is recommended in RSA PKCS5 standard.
 * In this case you must store it in the UserEntity alongside the hash, write your own
 * Shiro CredentialMatcher and register it in your Realm.
 * 
 * See {@link http://www.owasp.org/index.php/Hashing_Java}
 *
 * @author Paul Merlin <paul@nosphere.org>
 */
public class UsernamePasswordTest
        extends AbstractQi4jTest
{

    public static final String LAYER = "Layer 1";
    public static final String MODULE = "Module 1";
    public static final int PASSWORD_HASH_ITERATIONS = 11;
    public static final String TEST_USERNAME = "root";
    public static final String TEST_PASSWORD = "secret";
    public static final String TEST_PERMISSION = "gizmo";
    public static final String TEST_ROLE = "admin";

    public void assemble( ModuleAssembly module )
            throws AssemblyException
    {
        module.layerAssembly().setName( LAYER );
        module.setName( MODULE );
        module.addEntities( Permission.class,
                            Role.class,
                            RoleAssignment.class,
                            UserEntity.class );
        module.addObjects( Qi4jRealm.class );
        module.addServices( SecuredService.class );
        module.addServices( MemoryEntityStoreService.class, UuidIdentityGeneratorService.class ).visibleIn( Visibility.module );
        new RdfMemoryStoreAssembler( null, Visibility.module, Visibility.module ).assemble( module );

    }

    @Before
    public void before()
            throws UnitOfWorkCompletionException
    {
        // Qi4jRealmFactory is instanciated by Shiro and need a reference to the qi4j Application
        Qi4jRealmFactory.setQi4jApplication( application );

        // Setup Shiro
        Ini ini = new Ini();
        Section main = ini.addSection( "main" );
        main.put( Qi4jRealmFactory.class.getSimpleName(), Qi4jRealmFactory.class.getName() );
        SecurityUtils.setSecurityManager( new IniSecurityManagerFactory( ini ).getInstance() );

        // Create Test User
        UnitOfWork uow = unitOfWorkFactory.newUnitOfWork();

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
        user.passwordHash().set( new Sha256Hash( TEST_PASSWORD, TEST_USERNAME, PASSWORD_HASH_ITERATIONS ).toBase64() );
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

}
