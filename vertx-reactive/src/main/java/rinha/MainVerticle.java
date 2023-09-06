package rinha;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import rinha.controller.PessoaController;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);

   
        router.route().handler(BodyHandler.create());
        router.post("/pessoas").handler(PessoaController::create);
        router.get("/pessoas/:id").handler(PessoaController::show);

//        router.route().path("/pessoas/*").handler(ctx -> {
//O
//            // This handler will be called for every request
//            HttpServerResponse response = ctx.response();
//            response.putHeader("content-type", "text/plain");
//
//            // Write to the response and end it
//            response.end("Hello World from Vert.x-Web!");
//        });

        server.requestHandler(router).listen(8080);

    }
}
