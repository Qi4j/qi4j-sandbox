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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletContextHandler;
import static org.junit.Assert.*;
import org.junit.Test;
import org.qi4j.api.structure.Module;
import org.qi4j.bootstrap.ApplicationAssembly;
import org.qi4j.bootstrap.ApplicationAssemblyFactory;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.library.servlet.Qi4jServlet;
import org.qi4j.library.servlet.lifecycle.AbstractQi4jServletBootstrap;
import org.qi4j.library.shiro.servlet.Qi4jShiroServletFilter;
import org.qi4j.library.shiro.tests.username.SecuredService;
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
 * TODO Test remember me
 */
public class UsernamePasswordHttpBasicTest
{

    private static final String TEST_LAYER = "Layer 1";
    private static final String TEST_MODULE = "Module 1";
    private static final String SERVLET_PATH = "/test";

    @Test
    public void test()
            throws Exception
    {

        // Server

        InetAddress loopback = InetAddress.getLocalHost();
        int port = findFreePortOnIfaceWithPreference( loopback, 8989 );
        Server jetty = new Server( port );

        FilterHolder filterHolder = new FilterHolder( new Qi4jShiroServletFilter() );
        filterHolder.setInitParameter( Qi4jShiroServletFilter.REALM_LAYER_PARAM, TEST_LAYER );
        filterHolder.setInitParameter( Qi4jShiroServletFilter.REALM_MODULE_PARAM, TEST_MODULE );
        filterHolder.setInitParameter( Qi4jShiroServletFilter.FILTER_CHAINS_PARAM, "{\"" + SERVLET_PATH + "\":\"authcBasic\"}" );

        ServletContextHandler sch = new ServletContextHandler( jetty, "/", ServletContextHandler.SESSIONS | ServletContextHandler.NO_SECURITY );
        sch.addEventListener( new ServletApplicationBootstrap() );
        sch.addServlet( ServletUsingSecuredService.class, SERVLET_PATH );
        sch.addFilter( filterHolder, SERVLET_PATH, FilterMapping.DEFAULT );

        jetty.start();


        // Client

        HttpHost httpHost = new HttpHost( loopback.getHostAddress(), port );
        HttpGet get = new HttpGet( SERVLET_PATH );
        ResponseHandler<String> responseHandler = new BasicResponseHandler();

        DefaultHttpClient client = new DefaultHttpClient();
        client.getCredentialsProvider().setCredentials( new AuthScope( httpHost.getHostName(), httpHost.getPort() ),
                                                        new UsernamePasswordCredentials( UsernameFixtures.USERNAME, new String( UsernameFixtures.PASSWORD ) ) );

        // First request with credentials
        String response = client.execute( httpHost, get, responseHandler );
        assertEquals( UsernamePasswordHttpBasicTest.class.getName(), response );

        // Cookies logging for the curious
        soutCookies( client.getCookieStore().getCookies() );

        // Second request without credentials, should work thanks to sessions
        client.getCredentialsProvider().clear();
        response = client.execute( httpHost, get, responseHandler );
        assertEquals( UsernamePasswordHttpBasicTest.class.getName(), response );

        jetty.stop();

    }

    public static class ServletApplicationBootstrap
            extends AbstractQi4jServletBootstrap
    {

        public ApplicationAssembly assemble( ApplicationAssemblyFactory applicationFactory )
                throws AssemblyException
        {
            ApplicationAssembly app = applicationFactory.newApplicationAssembly();
            ModuleAssembly module = app.layerAssembly( TEST_LAYER ).moduleAssembly( TEST_MODULE );

            new UsernameTestAssembler().assemble( module );

            return app;
        }

    }

    public static class ServletUsingSecuredService
            extends Qi4jServlet
    {

        @Override
        protected void doGet( HttpServletRequest req, HttpServletResponse resp )
                throws ServletException, IOException
        {
            Module module = application.findModule( TEST_LAYER, TEST_MODULE );
            SecuredService service = module.serviceFinder().<SecuredService>findService( SecuredService.class ).get();
            service.doSomethingThatRequiresNothing();
            service.doSomethingThatRequiresUser();
            service.doSomethingThatRequiresPermissions();
            service.doSomethingThatRequiresRoles();
            PrintWriter out = resp.getWriter();
            out.print( UsernamePasswordHttpBasicTest.class.getName() );
            out.close();
        }

    }

    private static void soutCookies( Iterable<Cookie> cookies )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "\nLogging cookies for the curious" );
        for ( Cookie eachCookie : cookies ) {
            sb.append( "\t" ).append( eachCookie.getName() ).append( ": " ).append( eachCookie.getValue() ).
                    append( " ( " ).append( eachCookie.getDomain() ).append( " - " ).append( eachCookie.getPath() ).append( " )" );
        }
        System.out.println( sb.append( "\n" ).toString() );
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
