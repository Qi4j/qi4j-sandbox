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
package org.qi4j.library.shiro.realms;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.qi4j.api.unitofwork.UnitOfWork;
import org.qi4j.library.shiro.authc.SecureHashAuthenticationInfo;
import org.qi4j.library.shiro.authc.SecureHashCredentialsMatcher;
import org.qi4j.library.shiro.domain.RoleAssignee;
import org.qi4j.library.shiro.domain.SecureHashSecurable;

/**
 * @author Paul Merlin <paul@nosphere.org>
 */
public abstract class AbstractSecureHashQi4jRealm
        extends AbstractQi4jRealm
{

    public AbstractSecureHashQi4jRealm()
    {
        super();
        setCredentialsMatcher( new SecureHashCredentialsMatcher() );
    }

    @Override
    protected final AuthenticationInfo doGetAuthenticationInfo( AuthenticationToken token )
    {
        UnitOfWork uow = uowf.newUnitOfWork();
        String username = ( ( UsernamePasswordToken ) token ).getUsername();
        SecureHashSecurable secured = getSecureHashSecurable( username );
        if ( secured == null ) {
            return null;
        }
        AuthenticationInfo authc = new SecureHashAuthenticationInfo( username, secured.secureHash().get(), getName() );
        uow.discard();
        return authc;
    }

    protected abstract SecureHashSecurable getSecureHashSecurable( String username );

    @Override
    protected final RoleAssignee getRoleAssignee( PrincipalCollection principals )
    {
        return getRoleAssignee( ( String ) principals.fromRealm( getName() ).iterator().next() );
    }

    protected abstract RoleAssignee getRoleAssignee( String username );

}
