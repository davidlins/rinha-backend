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

        server.requestHandler(router).listen(8080);

    }

    PessoaRepository createPessoaRepository() {

        var connectOptions = new PgConnectOptions()
                .setPort(5432)
                .setHost("postgres")
                .setDatabase("rinha")
                .setUser("rinha")
                .setPassword("rinha");

        var poolOptions = new PoolOptions().setMaxSize(150);

        return new PessoaRepository(PgPool.pool(vertx, connectOptions, poolOptions));
    }

}
