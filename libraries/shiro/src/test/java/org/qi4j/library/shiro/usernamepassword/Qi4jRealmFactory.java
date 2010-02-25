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

import java.util.Arrays;
import java.util.Collection;
import org.apache.shiro.authc.credential.Sha256CredentialsMatcher;
import org.apache.shiro.realm.Realm;
import org.qi4j.api.object.ObjectBuilderFactory;
import org.qi4j.api.structure.Module;
import org.qi4j.library.shiro.realms.AbstractQi4jRealmFactory;

/**
 * @author Paul Merlin <paul@nosphere.org>
 */
public class Qi4jRealmFactory
        extends AbstractQi4jRealmFactory
{

    public Collection<Realm> getRealms()
    {
        // Instanciate the Qi4jRealm
        Module usernamePasswordModule = application.findModule( UsernamePasswordTest.LAYER, UsernamePasswordTest.MODULE );
        ObjectBuilderFactory obf = usernamePasswordModule.objectBuilderFactory();
        Qi4jRealm realm = obf.newObject( Qi4jRealm.class );

        // Set-up the CredentialMatcher
        Sha256CredentialsMatcher credentialMatcher = new Sha256CredentialsMatcher();
        credentialMatcher.setHashIterations( UsernamePasswordTest.PASSWORD_HASH_ITERATIONS );
        // We use salted Base64 encoding, see UsernamePasswordTest.before()
        credentialMatcher.setHashSalted( true );
        credentialMatcher.setStoredCredentialsHexEncoded( false );
        realm.setCredentialsMatcher( credentialMatcher );
        
        return Arrays.asList( new Realm[]{ realm } );
    }

}
