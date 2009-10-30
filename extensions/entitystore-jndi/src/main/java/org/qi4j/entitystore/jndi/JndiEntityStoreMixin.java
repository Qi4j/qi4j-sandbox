/*
 * Copyright 2008 Niclas Hedhman.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.qi4j.entitystore.jndi;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import org.qi4j.api.configuration.Configuration;
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.property.Property;
import org.qi4j.api.service.Activatable;
import org.qi4j.api.usecase.Usecase;
import org.qi4j.api.structure.Module;
import org.qi4j.spi.entitystore.EntityStore;
import org.qi4j.spi.entitystore.EntityStoreUnitOfWork;
import org.qi4j.spi.structure.ModuleSPI;

public class JndiEntityStoreMixin
    implements Activatable, EntityStore
{
    @This private Configuration<JndiConfiguration> configuration;
    private JndiSetup setup;

    public void activate()
        throws Exception
    {
        connect();
    }

    private void connect()
        throws NamingException
    {
        JndiConfiguration conf = configuration.configuration();
        setup = new JndiSetup();
        setup.instanceVersionAttribute = conf.versionAttribute().get();
        if( setup.instanceVersionAttribute == null )
        {
            setup.instanceVersionAttribute = "instanceVersion";
        }
        setup.lastModifiedDateAttribute = conf.lastModifiedDateAttribute().get();
        if( setup.lastModifiedDateAttribute == null )
        {
            setup.lastModifiedDateAttribute = "lastModifiedDate";
        }
        setup.identityAttribute = conf.identityAttribute().get();
        if( setup.identityAttribute == null )
        {
            setup.identityAttribute = "uid";
        }
        setup.qualifiedTypeAttribute = conf.qualifiedTypeAttribute().get();
        setup.baseDn = conf.baseDN().get();
        setup.isReadOnly = conf.readOnly().get();

        Hashtable<String, String> env = new Hashtable<String, String>();
        addToEnv( env, Context.AUTHORITATIVE, conf.authorative(), null );
        addToEnv( env, Context.BATCHSIZE, conf.batchSize(), null );
        addToEnv( env, Context.DNS_URL, conf.dnsUrl(), null );
        addToEnv( env, Context.INITIAL_CONTEXT_FACTORY, conf.initialContextFactory(), "com.sun.jndi.ldap.LdapCtxFactory" );
        addToEnv( env, Context.LANGUAGE, conf.language(), null );
        addToEnv( env, Context.OBJECT_FACTORIES, conf.objectFactories(), null );
        addToEnv( env, Context.PROVIDER_URL, conf.providerUrl(), null );
        addToEnv( env, Context.REFERRAL, conf.referral(), null );
        addToEnv( env, Context.SECURITY_AUTHENTICATION, conf.securityAuthentication(), null );
        addToEnv( env, Context.SECURITY_CREDENTIALS, conf.securityCredentials(), null );
        addToEnv( env, Context.SECURITY_PRINCIPAL, conf.securityPrincipal(), null );
        addToEnv( env, Context.SECURITY_PROTOCOL, conf.securityProtocol(), null );
        addToEnv( env, Context.STATE_FACTORIES, conf.stateFactories(), null );
        addToEnv( env, Context.URL_PKG_PREFIXES, conf.urlPkgPrefixes(), null );
        setup.context = new InitialDirContext( env );
    }

    private void addToEnv( Hashtable<String, String> env, String key, Property<String> property, String defaultValue )
    {
        String value = property.get();
        if( value != null )
        {
            env.put( key, value );
        }
        else if( defaultValue != null )
        {
            env.put( key, defaultValue );
        }
    }

    public void passivate()
        throws Exception
    {
        setup.context.close();
        setup = null;
    }

    public EntityStoreUnitOfWork newUnitOfWork( Usecase usecase, Module module )
    {
        return new JndiUow( setup, usecase, module );
    }

    public EntityStoreUnitOfWork visitEntityStates( EntityStateVisitor visitor, Module moduleInstance )
    {
        return null;
    }
}