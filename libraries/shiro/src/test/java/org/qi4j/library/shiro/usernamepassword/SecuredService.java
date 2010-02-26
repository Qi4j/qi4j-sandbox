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

import org.qi4j.api.concern.Concerns;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.service.ServiceComposite;
import org.qi4j.library.shiro.annotations.RequiresPermissions;
import org.qi4j.library.shiro.annotations.RequiresPermissionsConcern;
import org.qi4j.library.shiro.annotations.RequiresRoles;
import org.qi4j.library.shiro.annotations.RequiresRolesConcern;
import org.qi4j.library.shiro.annotations.RequiresUser;
import org.qi4j.library.shiro.annotations.RequiresUserConcern;

/**
 * @author Paul Merlin <p.merlin@nosphere.org>
 */
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
