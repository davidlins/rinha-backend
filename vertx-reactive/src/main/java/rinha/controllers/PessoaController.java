package rinha.controllers;

import static com.alibaba.fastjson2.JSON.parseObject;
import static com.alibaba.fastjson2.JSON.toJSONString;
import static rinha.controllers.Errors.error;
import static rinha.controllers.HttpStatus.BAD_REQUEST;
import static rinha.controllers.HttpStatus.CREATED;
import static rinha.controllers.HttpStatus.INTERNAL_SERVER_ERRORS;
import static rinha.controllers.HttpStatus.UNPROCESSABLE_CONTENT;

import java.util.Collections;

import io.vertx.rxjava3.ext.web.RoutingContext;
import rinha.models.Pessoa;
import rinha.repositories.PessoaRepository;

public class PessoaController {

    private static final String JSON_CONTENT_TYPE = "application/json";

    private PessoaRepository pessoaRepository;

    public PessoaController(PessoaRepository pessoaRepository) {
        super();
        this.pessoaRepository = pessoaRepository;
    }

    public void create(RoutingContext ctx) {

        Pessoa pessoa = null;
        try {
            pessoa = parseObject(ctx.body().buffer().getBytes(), Pessoa.class);
        } catch (RuntimeException e) {
            error(ctx, BAD_REQUEST.getCode(), "invalid payload");
            return;
        }

        var responseError = validade(pessoa);
        if (responseError != null) {
            error(ctx, responseError.getCode().getCode(), responseError.getCode().toString());
            return;
        }

        pessoaRepository.save(pessoa).subscribe(result -> ctx.response()
                .putHeader("Location", "/pessoas/" + result.getId())
                .putHeader("Content-Type", JSON_CONTENT_TYPE)
                .setStatusCode(CREATED.getCode()).end(toJSONString(result)),
                ex -> ctx.fail(
                        (ex.getMessage().contains("pessoas_apelido_key") ? UNPROCESSABLE_CONTENT.getCode()
                                : INTERNAL_SERVER_ERRORS.getCode())));

    }

    public void show(RoutingContext ctx) {

        var id = ctx.request().getParam("id");
        if (id == null) {
            error(ctx, BAD_REQUEST.getCode(), "Not found parameter id");
            return;
        }

        this.pessoaRepository.findById(id).subscribe(
                pessoaEntity -> {
                    ctx.response().end(toJSONString(pessoaEntity));
                },
                throwable -> ctx.fail(404)

        );
    }

    public void list(RoutingContext ctx) {

        var termo = ctx.request().getParam("t");
        if (termo == null || termo.trim().length() == 0) {
            error(ctx, BAD_REQUEST.getCode(), "Not found parameter t");
            return;
        }

        this.pessoaRepository.findByText(termo).toList()
                .subscribe(
                        pessoas -> ctx.response().end(toJSONString(pessoas)),
                        throwable -> ctx.fail(500, throwable));
    }

    public void count(RoutingContext ctx) {

        this.pessoaRepository.count().subscribe(
                count -> ctx.response().end(count.toString()));
    }

    private ResponseError validade(Pessoa pessoa) {

        if (pessoa.getNome() == null || pessoa.getApelido() == null || pessoa.getNascimento() == null
                || pessoa.getNome().length() > 100
                || pessoa.getApelido().length() > 32) {
            return ResponseError.builder().code(UNPROCESSABLE_CONTENT)
                    .errors(Collections.singletonList("UNPROCESSABLE_CONTENT")).build();
        }

        if (pessoa.getStack() != null) {
            for (String stack : pessoa.getStack()) {
                if (stack.length() > 32) {
                    return ResponseError.builder().code(UNPROCESSABLE_CONTENT)
                            .errors(Collections.singletonList("UNPROCESSABLE_CONTENT")).build();
                }
            }
        }

        return null;
    }

}
