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
import java.util.UUID;
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
import lombok.extern.slf4j.Slf4j;
import rinha.models.Pessoa;
import rinha.models.ResponseError;
import rinha.repositories.PessoaRepository;

@WebServlet(name = "PessoaServlet", urlPatterns = { "/pessoas/*", "/contagem-pessoas" }, loadOnStartup = 1,
    asyncSupported = true)
@Slf4j
public class PessoaServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String JSON_CONTENT_TYPE = "application/json";

    private PessoaRepository pessoaRepository;

    private static Map<String, String> pessoasMap = new ConcurrentHashMap<>();
    private static Set<String> apelidos = Collections.synchronizedSet(new HashSet<>());

    public PessoaServlet() {
        super();
        this.pessoaRepository = new PessoaRepository();
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

        var async = request.startAsync();

        try {

            Pessoa pessoa = JSON.parseObject(request.getInputStream(), Pessoa.class);

            var responseError = validade(pessoa);
            if (responseError == null) {
                try {

                    pessoa.setId(UUID.randomUUID().toString());
                    this.pessoaRepository.save(pessoa);

                    updateCache(pessoa);

                    doResponse(async, response, CREATED, pessoa,
                            singletonMap("Location", "/pessoas/" + pessoa.getId()));

                } catch (Exception e) {

                    var httpStatus = INTERNAL_SERVER_ERRORS;

                    if (e.getMessage().indexOf("pessoas_apelido_key") >= 0) {

                        httpStatus = UNPROCESSABLE_CONTENT;
                        updateCache(pessoa);

                    } else {

                        log.error(e.getMessage(), e);
                    }

                    doResponse(async, response, httpStatus, null,
                            null);
                }
            } else {
                doResponse(async, response, responseError.getCode(), null,
                        null);
            }

        } catch (JSONException e) {
            doResponse(async, response, BAD_REQUEST, null,
                    null);
        }

    }

    private void show(String id, HttpServletRequest request, HttpServletResponse response) throws IOException {

        var async = request.startAsync();

        try {
            var pessoaCache = pessoasMap.get(id);
            if (pessoaCache != null) {
                
                doResponse(async, response, OK, null, pessoaCache, null);
                
            } else {

                var pessoa = this.pessoaRepository.findById(id);

                if (pessoa != null && !apelidos.contains(pessoa.getApelido())) {
                    updateCache(pessoa);
                }

                doResponse(async, response, (pessoa != null) ? OK : NOT_FOUND, pessoa, null);

            }
        } catch (SQLException e) {

            log.error("Erro ao consultar com id" + id + ": " + e.getMessage(), e);

            doResponse(async, response, INTERNAL_SERVER_ERRORS, null,
                    null);
        }
    }

    private void list(HttpServletRequest request, HttpServletResponse response) throws IOException {

        var async = request.startAsync();

        var termo = request.getParameter("t");
        if (termo == null) {

            doResponse(async, response, BAD_REQUEST, null,
                    null);

        } else {

            try {

                var pessoas = this.pessoaRepository.findByText(termo);
                doResponse(async, response, OK, pessoas, null);

            } catch (SQLException e) {

                log.error("Erro ao consultar termo" + termo + ": " + e.getMessage(), e);
                doResponse(async, response, INTERNAL_SERVER_ERRORS, null,
                        null);
            }
        }
    }

    private void count(HttpServletRequest request, HttpServletResponse response) throws IOException {

        var async = request.startAsync();

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
            doResponse(async, response, INTERNAL_SERVER_ERRORS, null,
                    null);
        }
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

        if (apelidos.contains(pessoa.getApelido())) {
            return ResponseError.builder().code(UNPROCESSABLE_CONTENT)
                    .errors(Collections.singletonList("Apelido duplicado")).build();
        }

        return null;
    }

    private void updateCache(Pessoa pessoa) {
        pessoasMap.put(pessoa.getId(), JSON.toJSONString(pessoa));
        apelidos.add(pessoa.getApelido());
    }

    private void doResponse(AsyncContext async, HttpServletResponse response, HttpStatus httpStatus, Object bodyObject,
            Map<String, String> headerMap) throws IOException {

        doResponse(async, response, httpStatus, bodyObject, null, headerMap);
    }

    private void doResponse(AsyncContext async, HttpServletResponse response, HttpStatus httpStatus, Object bodyObject,
            String json,
            Map<String, String> headerMap) throws IOException {

        ServletOutputStream out = response.getOutputStream();
        out.setWriteListener(new WriteListener() {
            @Override
            public void onWritePossible() throws IOException {

                if (headerMap != null) {
                    headerMap.forEach((key, value) -> response.addHeader(key, value));
                }
                response.setStatus(httpStatus.getCode());
                response.setContentType(JSON_CONTENT_TYPE);
                if (bodyObject != null) {
                    JSON.writeTo(out, bodyObject);
                } else if (json != null) {
                    out.print(json);
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
