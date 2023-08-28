package rinha;

import java.util.concurrent.Executors;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import rinha.controllers.PessoaController;

public class RinharApiStartup {

    private static final String HTTP_PORT_KEY = "http.port";

    public static void main(String[] args) throws Exception {

        var threadPool = new QueuedThreadPool();
        threadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());
        var server = new Server(threadPool);
        
        var httpPort = (System.getProperties().containsKey(HTTP_PORT_KEY)) ? System.getProperty(HTTP_PORT_KEY)
                : System.getenv("http.port");

        var connector = new ServerConnector(server);
        connector.setPort((httpPort != null) ? Integer.valueOf(httpPort) : 8080);
        server.addConnector(connector);

        var pessoaController = new PessoaController();
        var contextCollection = new ContextHandlerCollection();

        contextCollection.addHandler(new ContextHandler(pessoaController, "/"));

        server.setHandler(contextCollection);

        server.start();
    }

}
