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
package org.qi4j.library.shiro.servlet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletContextHandler;
import static org.junit.Assert.*;
import org.junit.Test;
import org.qi4j.bootstrap.ApplicationAssembly;
import org.qi4j.bootstrap.ApplicationAssemblyFactory;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.library.servlet.lifecycle.AbstractQi4jServletBootstrap;
import org.qi4j.library.shiro.tests.username.UsernameFixtures;
import org.qi4j.library.shiro.tests.username.UsernameTestAssembler;

/**
 * A unit test showing how to use Shiro in Qi4j Web Applications with Username & Password credentials.
 *
 * Authentication scheme used is HTTP Basic as Shiro does not provide a Digest implementation yet.
 * Remember that the user password goes up from the client to the http server.
 * For now, you can use SSL/TLS to prevent evesdropping but the server code still has access to the
 * password and that can be a problem.
 *
 * TODO Test sessions
 * TODO Test remember me
 */
public class Qi4jShiroWebTest
{

    public static final String LAYER = "Layer 1";
    public static final String MODULE = "Module 1";
    public static final String TEST_USERNAME = "root";
    public static final char[] TEST_PASSWORD = "secret".toCharArray();
    public static final String TEST_PERMISSION = "gizmo";
    public static final String TEST_ROLE = "admin";

    @Test
    public void test()
            throws Exception
    {
        int port = findFreePortOnIfaceWithPreference( InetAddress.getLocalHost(), 8989 );
        Server jetty = new Server( port );

        ServletContextHandler sch = new ServletContextHandler( jetty, "/", ServletContextHandler.SESSIONS | ServletContextHandler.NO_SECURITY );
        sch.addEventListener( new AbstractQi4jServletBootstrap()
        {

            public ApplicationAssembly assemble( ApplicationAssemblyFactory applicationFactory )
                    throws AssemblyException
            {
                ApplicationAssembly app = applicationFactory.newApplicationAssembly();
                ModuleAssembly module = app.layerAssembly( LAYER ).moduleAssembly( MODULE );

                new UsernameTestAssembler().assemble( module );

                return app;
            }

        } );
        sch.addServlet( ServletUsingSecuredService.class, "/test" );

        FilterHolder filterHolder = new FilterHolder( new ShiroServletFilter() );
        filterHolder.setInitParameter( ShiroServletFilter.LAYER, LAYER );
        filterHolder.setInitParameter( ShiroServletFilter.MODULE, MODULE );
        filterHolder.setInitParameter( ShiroServletFilter.FILTER_CHAINS, "{\"/test/**\":\"authcBasic\"}" );
        sch.addFilter( filterHolder, "/test", FilterMapping.DEFAULT );

        jetty.start();

        // Client

        HttpHost httpHost = new HttpHost( "localhost", port );
        HttpContext httpCtx = new BasicHttpContext();
        DefaultHttpClient client = new DefaultHttpClient();

        UsernamePasswordCredentials httpCredentials = new UsernamePasswordCredentials( UsernameFixtures.USERNAME, new String( UsernameFixtures.PASSWORD ) );
        client.getCredentialsProvider().setCredentials( new AuthScope( httpHost.getHostName(), httpHost.getPort() ), httpCredentials );

        HttpGet get = new HttpGet( "/test" );

        String response = client.execute( httpHost, get, new BasicResponseHandler(), httpCtx );
        assertEquals( Qi4jShiroWebTest.class.getName(), response.trim() );

        jetty.stop();
    }

    private static int findFreePortOnIfaceWithPreference( final InetAddress address, final int prefered )
            throws IOException
    {
        ServerSocket server;
        if ( prefered > 0 ) {
            server = new ServerSocket( prefered, 1, address );
        } else {
            server = new ServerSocket( 0, 1, address );
        }
        int port = server.getLocalPort();
        server.close();
        return port;
    }

}
