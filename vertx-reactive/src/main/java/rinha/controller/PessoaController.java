package rinha.controller;

import static rinha.controller.Errors.error;
import static rinha.controller.HttpStatus.BAD_REQUEST;
import static rinha.controller.HttpStatus.CREATED;
import static rinha.controller.HttpStatus.NOT_FOUND;
import static rinha.controller.HttpStatus.UNPROCESSABLE_CONTENT;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class PessoaController {

    private static final String JSON_CONTENT_TYPE = "application/json";

    private static Map<String, JsonObject> pessoasMap = new HashMap<>();
    private static Set<String> apelidos = new HashSet<>();
    

    public static void create(RoutingContext ctx) {

        JsonObject item;
        try {
            item = ctx.getBodyAsJson();
        } catch (RuntimeException e) {
            error(ctx, BAD_REQUEST.getCode(), "invalid payload");
            return;
        }
        
        item.put("id", UUID.randomUUID().toString());
        updateCache(item);
        
        var response = ctx.response();
        response.putHeader("Location", "/pessoas/" + item.getString("id"));
        response.putHeader("Content-Type", JSON_CONTENT_TYPE);
        response.setStatusCode(CREATED.getCode());
        response.end(item.toBuffer());

    }
    
    public static void show(RoutingContext ctx) {
        
        var id = ctx.request().getParam("id");
        if(id == null ) {
            error(ctx, BAD_REQUEST.getCode(), "invalid payload");
            return;
        }
        
        var item = pessoasMap.get(id);
        if(item == null) {
            error(ctx, NOT_FOUND.getCode(), "invalid payload");
            return;   
        }
        
     
        var response = ctx.response();
        response.putHeader("Location", "/pessoas/" + item.getString("id"));
        response.putHeader("Content-Type", JSON_CONTENT_TYPE);
        response.setStatusCode(CREATED.getCode());
        response.end(item.toBuffer());
      }


    private ResponseError validade(JsonObject pessoa) {

        if (pessoa.getString("nome") == null || pessoa.getString("apelido") == null
                || pessoa.getString("nascimento") == null
                || pessoa.getString("nome").length() > 100
                || pessoa.getString("apelido").length() > 32) {
            return ResponseError.builder().code(UNPROCESSABLE_CONTENT)
                    .errors(Collections.singletonList("UNPROCESSABLE_CONTENT")).build();
        }

        if (pessoa.getString("stack") != null) {
            for (var stack : pessoa.getJsonArray("stack")) { // vou ser cruzificado por isso, fodas
                if (stack.toString().length() > 32) {
                    return ResponseError.builder().code(UNPROCESSABLE_CONTENT)
                            .errors(Collections.singletonList("UNPROCESSABLE_CONTENT")).build();
                }
            }
        }

        if (apelidos.contains(pessoa.getString("apelido"))) {
            return ResponseError.builder().code(UNPROCESSABLE_CONTENT)
                    .errors(Collections.singletonList("Apelido duplicado")).build();
        }

        return null;
    }
    
    
    private static void updateCache(JsonObject pessoa) {
        pessoasMap.put(pessoa.getString("id"), pessoa.copy());
        apelidos.add(pessoa.getString("apelido"));
    }


}
