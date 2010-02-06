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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.PermissionUtils;
import org.qi4j.api.common.AppliesTo;
import org.qi4j.api.concern.ConcernOf;
import org.qi4j.api.injection.scope.Invocation;

/**
 * FIXME Do not work because of {@link http://issues.ops4j.org/browse/QI-241 QI-241}.
 * 
 * @author Paul Merlin <paul@nosphere.org>
 */
@AppliesTo( { RequiresAuthentication.class,
              RequiresGuest.class,
              RequiresPermissions.class,
              RequiresRoles.class,
              RequiresUser.class } )
public class SecurityConcern
        extends ConcernOf<InvocationHandler>
        implements InvocationHandler
{

    @Invocation
    private RequiresAuthentication requiresAuthentication;
    @Invocation
    private RequiresGuest requiresGuest;
    @Invocation
    private RequiresPermissions requiresPermissions;
    @Invocation
    private RequiresRoles requiresRoles;
    @Invocation
    private RequiresUser requiresUser;

    public Object invoke( Object proxy, Method method, Object[] args )
            throws Throwable
    {
        Subject subject = SecurityUtils.getSubject();
        if ( requiresGuest != null ) {
            System.err.println( "SecurityConcern::RequiresGuest" );
            if ( subject.getPrincipal() != null ) {
                throw new UnauthenticatedException( "Attempting to perform a guest-only operation.  The current Subject is "
                        + "not a guest (they have been authenticated or remembered from a previous login).  Access "
                        + "denied." );

            }
        } else {
            System.err.println( "SecurityConcern::RequiresGuest: not concerned" );
        }
        if ( requiresUser != null ) {
            System.err.println( "SecurityConcern::RequiresUser" );
            if ( subject.getPrincipal() == null ) {
                throw new UnauthenticatedException( "Attempting to perform a user-only operation.  The current Subject is "
                        + "not a user (they haven't been authenticated or remembered from a previous login).  "
                        + "Access denied." );
            }
        } else {
            System.err.println( "SecurityConcern::RequiresUser: not concerned" );
        }
        if ( requiresAuthentication != null ) {
            System.err.println( "SecurityConcern::RequiresAuthentication" );
            if ( !subject.isAuthenticated() ) {
                throw new UnauthenticatedException( "The current Subject is not authenticated.  Access denied." );
            }
        } else {
            System.err.println( "SecurityConcern::RequiresAuthentication: not concerned" );
        }
        if ( requiresRoles != null ) {
            System.err.println( "SecurityConcern::RequiresRoles" );
            String roleId = requiresRoles.value();
            String[] roles = roleId.split( "," );
            if ( roles.length == 1 ) {
                if ( !subject.hasRole( roles[0] ) ) {
                    String msg = "Calling Subject does not have required role [" + roleId + "].  "
                            + "MethodInvocation denied.";
                    throw new UnauthorizedException( msg );
                }
            } else {
                Set<String> rolesSet = new LinkedHashSet<String>( Arrays.asList( roles ) );
                if ( !subject.hasAllRoles( rolesSet ) ) {
                    String msg = "Calling Subject does not have required roles [" + roleId + "].  "
                            + "MethodInvocation denied.";
                    throw new UnauthorizedException( msg );
                }
            }
        } else {
            System.err.println( "SecurityConcern::RequiresRoles: not concerned" );
        }
        if ( requiresPermissions != null ) {
            System.err.println( "SecurityConcern::RequiresPermissions" );
            String permsString = requiresPermissions.value();
            Set<String> permissions = PermissionUtils.toPermissionStrings( permsString );
            if ( permissions.size() == 1 ) {
                if ( !subject.isPermitted( permissions.iterator().next() ) ) {
                    String msg = "Calling Subject does not have required permission [" + permsString + "].  "
                            + "Method invocation denied.";
                    throw new UnauthorizedException( msg );
                }
            } else {
                String[] permStrings = new String[ permissions.size() ];
                permStrings = permissions.toArray( permStrings );
                if ( !subject.isPermittedAll( permStrings ) ) {
                    String msg = "Calling Subject does not have required permissions [" + permsString + "].  "
                            + "Method invocation denied.";
                    throw new UnauthorizedException( msg );
                }

            }
        } else {
            System.err.println( "SecurityConcern::RequiresPermissions: not concerned" );
        }
        return next.invoke( proxy, method, args );
    }

}
