package rinha.controllers;

import static java.util.Collections.singletonMap;
import static rinha.controllers.HttpStatus.BAD_REQUEST;
import static rinha.controllers.HttpStatus.CREATED;
import static rinha.controllers.HttpStatus.INTERNAL_SERVER_ERRORS;
import static rinha.controllers.HttpStatus.NOT_FOUND;
import static rinha.controllers.HttpStatus.OK;
import static rinha.controllers.HttpStatus.UNPROCESSABLE_CONTENT;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import rinha.models.Pessoa;
import rinha.models.ResponseError;
import rinha.repositories.PessoaRepository;

@WebServlet(name = "PessoaServlet", urlPatterns = { "/pessoas/*", "/contagem-pessoas" }, loadOnStartup = 1)
public class PessoaServletAsync extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String JSON_CONTENT_TYPE = "application/json";

    private PessoaRepository pessoaRepository;
    private Validator validator;
    private static Map<String, Pessoa> pessoasMap = new ConcurrentHashMap<>();
    private static Set<String> apelidos = Collections.synchronizedSet(new HashSet<>());

    public PessoaServletAsync() {
        super();
        this.pessoaRepository = new PessoaRepository();
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    public PessoaServletAsync(PessoaRepository pessoaRepository) {
        super();
        this.pessoaRepository = pessoaRepository;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (request.getMethod().equals("POST")) {
            insert(request, response);
        } else {
            if (request.getRequestURI().startsWith("/contagem-pessoas")) {
                count(request, response);
            } else if (request.getPathInfo() == null) {
                list(request, response);
            } else {
                show(request.getPathInfo().substring(1), request, response);
            }
        }
    }

    private void insert(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        AsyncContext async = request.startAsync();

        try {

            Pessoa pessoa = JSON.parseObject(request.getInputStream(), Pessoa.class);

            var responseError = validade(pessoa);
            if (responseError == null) {
                try {
                    this.pessoaRepository.save(pessoa);
                    pessoasMap.put(pessoa.getId(), pessoa);
                    apelidos.add(pessoa.getApelido());
                    doResponse(async, response, CREATED.getCode(), pessoa,
                            singletonMap("Location", "/pessoas/" + pessoa.getId()));
                } catch (SQLException e) {

                    // e.printStackTrace();
                    var httpStatus = INTERNAL_SERVER_ERRORS;
                    if (e.getMessage().indexOf("pessoas_apelido_key") >= 0) {
                        httpStatus = UNPROCESSABLE_CONTENT;
                        apelidos.add(pessoa.getApelido());
                    }

//                    responseError = ResponseError.builder().code(httpStatus.getCode())
//                            .errors(Collections.singletonList(e.getMessage())).build();
//
//                    doResponse(async, response, responseError.getCode(), responseError, null);

                    doResponse(async, response, httpStatus.getCode(), null,
                            null);
                }
            } else {
                // doResponse(async, response, responseError.getCode(), responseError, null);
                doResponse(async, response, responseError.getCode(), null,
                        null);
            }

        } catch (JSONException e) {
//            doResponse(async, response, BAD_REQUEST.getCode(), ResponseError.builder().code(BAD_REQUEST.getCode())
//                    .errors(singletonList("Json invalido")).build(), null);
            doResponse(async, response, BAD_REQUEST.getCode(), null,
                    null);
        }

    }

    private void show(String id, HttpServletRequest request, HttpServletResponse response) throws IOException {

        AsyncContext async = request.startAsync();

        try {
            var pessoa = pessoasMap.get(id);
            if (pessoa == null) {
                pessoa = this.pessoaRepository.findById(id);
                if (pessoa != null && !apelidos.contains(pessoa.getApelido())) {
                    apelidos.add(pessoa.getApelido());
                }
            }

            if (pessoa != null) {
                doResponse(async, response, OK.getCode(), pessoa, null);
            } else {
//                doResponse(async, response, NOT_FOUND.getCode(), ResponseError.builder().code(NOT_FOUND.getCode())
//                        .errors(Collections.singletonList("Não encontrou pessoa com id: " + id)).build(), null);
                doResponse(async, response, NOT_FOUND.getCode(), null,
                        null);
            }
        } catch (SQLException e) {
            // e.printStackTrace();
//            doResponse(async, response, INTERNAL_SERVER_ERRORS.getCode(),
//                    ResponseError.builder().code(INTERNAL_SERVER_ERRORS.getCode())
//                            .errors(Collections.singletonList("Erro ao consultar pessoa: " + e.getMessage()))
//                            .build(),
//                    null);
            doResponse(async, response, INTERNAL_SERVER_ERRORS.getCode(), null,
                    null);
        }

    }

    private void list(HttpServletRequest request, HttpServletResponse response) throws IOException {

        AsyncContext async = request.startAsync();

        var termo = request.getParameter("t");
        if (termo != null && termo.trim().length() > 0) {
            try {
                var pessoas = this.pessoaRepository.findByText(termo);
                doResponse(async, response, OK.getCode(), pessoas, null);
            } catch (SQLException e) {
                // e.printStackTrace();
//                doResponse(async, response, INTERNAL_SERVER_ERRORS.getCode(),
//                        ResponseError.builder().code(INTERNAL_SERVER_ERRORS.getCode())
//                                .errors(Collections.singletonList("Erro ao consultar pessoas: " + e.getMessage()))
//                                .build(),
//                        null);
                doResponse(async, response, INTERNAL_SERVER_ERRORS.getCode(), null,
                        null);
            }
        } else {
//            doResponse(async, response, BAD_REQUEST.getCode(), ResponseError.builder().code(BAD_REQUEST.getCode())
//                    .errors(Collections.singletonList("Pamametro 't' é obrigatório")).build(), null);
            doResponse(async, response, BAD_REQUEST.getCode(), null,
                    null);
        }

    }

    private void count(HttpServletRequest request, HttpServletResponse response) throws IOException {

        AsyncContext async = request.startAsync();

        try {
            var count = this.pessoaRepository.count();

            ServletOutputStream out = response.getOutputStream();
            out.setWriteListener(new WriteListener() {
                @Override
                public void onWritePossible() throws IOException {

                    response.setStatus(OK.getCode());
                    out.print(count);
                    async.complete();
                }

                @Override
                public void onError(Throwable t) {
                    getServletContext().log("Async Error", t);
                    async.complete();
                }
            });

        } catch (SQLException e) {
            // e.printStackTrace();
//            doResponse(async, response, INTERNAL_SERVER_ERRORS.getCode(),
//                    ResponseError.builder().code(INTERNAL_SERVER_ERRORS.getCode())
//                            .errors(Collections.singletonList("Erro ao consultar qtd pessoas: " + e.getMessage()))
//                            .build(),
//                    null);
            doResponse(async, response, INTERNAL_SERVER_ERRORS.getCode(), null,
                    null);
        }
    }

    private ResponseError validade(Pessoa pessoa) {

        var constraintViolations = validator.validate(pessoa);
        if (constraintViolations == null || constraintViolations.isEmpty()) {
            return (!apelidos.contains(pessoa.getApelido())) ? null
                    : ResponseError.builder().code(UNPROCESSABLE_CONTENT.getCode())
                            .errors(Collections.singletonList("Apeliodo duplicado")).build();
        }

        return ResponseError.builder().code(UNPROCESSABLE_CONTENT.getCode())
                .errors(constraintViolations.stream().map(contraint -> contraint.getMessage()).toList()).build();

    }

    private void doResponse(AsyncContext async, HttpServletResponse response, int httpStatusCode, Object bodyObject,
            Map<String, String> headerMap) throws IOException {

        ServletOutputStream out = response.getOutputStream();
        out.setWriteListener(new WriteListener() {
            @Override
            public void onWritePossible() throws IOException {

                if (headerMap != null) {
                    headerMap.forEach((key, value) -> response.addHeader(key, value));
                }
                response.setStatus(httpStatusCode);
                response.setContentType(JSON_CONTENT_TYPE);
                if (bodyObject != null) {
                    JSON.writeTo(out, bodyObject);
                } else {
                    out.print("{}");
                }
                async.complete();
            }

            @Override
            public void onError(Throwable t) {
                getServletContext().log("Async Error", t);
                async.complete();
            }
        });
    }
}
