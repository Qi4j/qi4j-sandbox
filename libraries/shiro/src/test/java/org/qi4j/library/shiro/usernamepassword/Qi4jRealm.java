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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.qi4j.api.injection.scope.Structure;
import org.qi4j.api.query.Query;
import org.qi4j.api.query.QueryBuilder;
import org.qi4j.api.query.QueryBuilderFactory;
import static org.qi4j.api.query.QueryExpressions.*;
import org.qi4j.api.unitofwork.UnitOfWork;
import org.qi4j.api.unitofwork.UnitOfWorkFactory;
import org.qi4j.library.shiro.domain.Permission;
import org.qi4j.library.shiro.domain.Role;
import org.qi4j.library.shiro.domain.RoleAssignment;

/**
 * @author Paul Merlin <paul@nosphere.org>
 */
public class Qi4jRealm
        extends AuthorizingRealm
{

    @Structure
    private UnitOfWorkFactory uowf;
    @Structure
    private QueryBuilderFactory qbf;

    public Qi4jRealm()
    {
        setName( Qi4jRealm.class.getSimpleName() );
        setCachingEnabled( false );
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo( AuthenticationToken token )
            throws AuthenticationException
    {
        // Finding UserEntity
        UsernamePasswordToken upToken = ( UsernamePasswordToken ) token;
        UnitOfWork uow = uowf.newUnitOfWork();
        UserEntity user = findUserEntityByUsername( upToken.getUsername() );
        if ( user == null ) {
            return null;
        }

        // Building AuthenticationInfo
        AuthenticationInfo authc = new SimpleAuthenticationInfo( user.username().get(), user.passwordHash().get(), getName() );

        uow.discard();
        return authc;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo( PrincipalCollection principals )
    {
        UnitOfWork uow = uowf.newUnitOfWork();

        // Finding UserEntity
        String username = ( String ) principals.fromRealm( Qi4jRealm.class.getSimpleName() ).iterator().next();
        UserEntity user = findUserEntityByUsername( username );
        if ( user == null ) {
            return null;
        }

        // Loading Roles & Permissions
        Set<String> roleNames = new LinkedHashSet<String>();
        Set<String> permissions = new LinkedHashSet<String>();
        for ( RoleAssignment eachAssignment : user.roleAssignments() ) {
            Role eachRole = eachAssignment.role().get();
            roleNames.add( eachRole.name().get() );
            for ( Permission eachPermission : eachRole.permissions() ) {
                permissions.add( eachPermission.string().get() );
            }
        }

        // Building AuthorizationInfo
        SimpleAuthorizationInfo authz = new SimpleAuthorizationInfo( roleNames );
        authz.setStringPermissions( permissions );

        uow.discard();
        return authz;
    }

    private UserEntity findUserEntityByUsername( String username )
    {
        UnitOfWork uow = uowf.currentUnitOfWork();
        QueryBuilder<UserEntity> queryBuilder = qbf.newQueryBuilder( UserEntity.class );
        UserEntity userTemplate = templateFor( UserEntity.class );
        queryBuilder = queryBuilder.where( eq( userTemplate.username(), username ) );
        Query<UserEntity> query = queryBuilder.newQuery( uow ).maxResults( 1 );
        Iterator<UserEntity> it = query.iterator();
        return it.next();
    }

}
