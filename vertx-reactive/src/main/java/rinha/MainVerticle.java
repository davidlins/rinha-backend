package rinha;

import io.vertx.core.Promise;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.handler.BodyHandler;
import io.vertx.rxjava3.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import rinha.controllers.PessoaController;
import rinha.repositories.PessoaRepository;

public class MainVerticle extends AbstractVerticle {

    private static final String HTTP_PORT_KEY = "http.port";
    private static final String POSTGRES_PORT_HOST = "postgres.host";
    private static final String POSTGRES_CONNECTION_SIZE = "postgres.connection.size";
            
    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        var server = vertx.createHttpServer();
        var router = Router.router(vertx);

        var pessoaController = new PessoaController(createPessoaRepository());

        router.route().handler(BodyHandler.create());
        router.post("/pessoas").handler(pessoaController::create);
        router.get("/pessoas/:id").handler(pessoaController::show);
        router.get("/pessoas").handler(pessoaController::list);
        router.get("/contagem-pessoas").handler(pessoaController::count);

        var httpPort = (System.getProperties().containsKey(HTTP_PORT_KEY)) ? System.getProperty(HTTP_PORT_KEY)
                : System.getenv(HTTP_PORT_KEY);
        
        server.requestHandler(router).listen((httpPort != null) ? Integer.valueOf(httpPort):  8080);

    }

    PessoaRepository createPessoaRepository() {
        
        var postgresHost = (System.getProperties().containsKey(POSTGRES_PORT_HOST)) ? System.getProperty(POSTGRES_PORT_HOST)
                : System.getenv(POSTGRES_PORT_HOST);
       
        
        var postgresConnectionSize = (System.getProperties().containsKey(POSTGRES_CONNECTION_SIZE)) ? System.getProperty(POSTGRES_CONNECTION_SIZE)
                : System.getenv(POSTGRES_CONNECTION_SIZE);
        
        var connectOptions = new PgConnectOptions()
                .setPort(5432)
                .setHost(postgresHost)
                .setDatabase("rinha")
                .setUser("rinha")
                .setPassword("rinha");

        
        
        var poolOptions = new PoolOptions();
        if(postgresConnectionSize != null) {
               poolOptions.setMaxSize(Integer.valueOf(postgresConnectionSize));
        }

        return new PessoaRepository(PgPool.pool(vertx, connectOptions, poolOptions));
    }

}
