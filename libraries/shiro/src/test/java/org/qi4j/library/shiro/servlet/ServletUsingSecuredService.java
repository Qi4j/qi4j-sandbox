package org.qi4j.library.shiro.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.qi4j.api.structure.Module;
import org.qi4j.library.servlet.Qi4jServlet;
import org.qi4j.library.shiro.tests.username.SecuredService;

public class ServletUsingSecuredService
        extends Qi4jServlet
{

    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp )
            throws ServletException, IOException
    {
        Module module = application.findModule( Qi4jShiroWebTest.LAYER, Qi4jShiroWebTest.MODULE );
        SecuredService service = module.serviceFinder().<SecuredService>findService( SecuredService.class ).get();
        service.doSomethingThatRequiresNothing();
        service.doSomethingThatRequiresUser();
        service.doSomethingThatRequiresPermissions();
        service.doSomethingThatRequiresRoles();
        PrintWriter out = resp.getWriter();
        out.println( Qi4jShiroWebTest.class.getName() );
    }

}
