package rinha;

import java.util.concurrent.Executors;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import rinha.controllers.PessoaServlet;

public class RinharApiStartup {

    private static final String HTTP_PORT_KEY = "http.port";

    public static void main(String[] args) throws Exception {

        var threadPool = new QueuedThreadPool(600);
        threadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());

        var server = new Server(threadPool);

        var httpPort = (System.getProperties().containsKey(HTTP_PORT_KEY)) ? System.getProperty(HTTP_PORT_KEY)
                : System.getenv("http.port");

        ServerConnector connector = new ServerConnector(server);
        connector.setPort((httpPort != null) ? Integer.valueOf(httpPort) : 8080);
        server.addConnector(connector);

        var context = new ServletContextHandler("/");
        server.setHandler(context);

        var servletHolder = new ServletHolder("PessoaServlet", PessoaServlet.class);
        context.addServlet(servletHolder, "/pessoas/*");
        context.addServlet(servletHolder, "/contagem-pessoas");

        server.start();

    }

}
