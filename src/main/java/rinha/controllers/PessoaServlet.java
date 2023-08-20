package rinha.controllers;

import static java.util.Collections.singletonList;
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

import jakarta.servlet.ServletException;
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
public class PessoaServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String JSON_CONTENT_TYPE = "application/json";

    private PessoaRepository pessoaRepository;
    private Validator validator;
    private Map<String, Pessoa> pessoasMap = new ConcurrentHashMap<>();
    private Set<String> apelidos = Collections.synchronizedSet(new HashSet<>());

    public PessoaServlet() {
        super();
        this.pessoaRepository = new PessoaRepository();
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    public PessoaServlet(PessoaRepository pessoaRepository) {
        super();
        this.pessoaRepository = pessoaRepository;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        if (request.getRequestURI().startsWith("/contagem-pessoas")) {
            count(request, response);
        } else if (request.getPathInfo() != null) {
            show(request.getPathInfo().substring(1), request, response);
        } else {
            list(request, response);
        }

    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            Pessoa pessoa = JSON.parseObject(request.getInputStream(), Pessoa.class);

            var responseError = validade(pessoa);
            if (responseError == null) {
                try {
                    this.pessoaRepository.save(pessoa);
                    this.pessoasMap.put(pessoa.getId(), pessoa);
                    this.apelidos.add(pessoa.getApelido());
                    doResponse(response, CREATED.getCode(), pessoa,
                            singletonMap("Location", "/pessoas/" + pessoa.getId()));
                } catch (SQLException e) {

                    // e.printStackTrace();
                    var httpStatus = INTERNAL_SERVER_ERRORS;
                    if (e.getMessage().indexOf("pessoas_apelido_key") >= 0) {
                        httpStatus = UNPROCESSABLE_CONTENT;
                        this.apelidos.add(pessoa.getApelido());
                    }

                    responseError = ResponseError.builder().code(httpStatus.getCode())
                            .errors(Collections.singletonList(e.getMessage())).build();

                    doResponse(response, responseError.getCode(), responseError, null);
                }
            } else {
                doResponse(response, responseError.getCode(), responseError, null);
            }

        } catch (JSONException e) {
            doResponse(response, BAD_REQUEST.getCode(), ResponseError.builder().code(BAD_REQUEST.getCode())
                    .errors(singletonList("Json invalido")).build(), null);
        }

    }

    protected void show(String id, HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            var pessoa = this.pessoasMap.get(id);
            if (pessoa == null) {
                pessoa = this.pessoaRepository.findById(id);
                if(pessoa != null && !apelidos.contains(pessoa.getApelido())) {
                    apelidos.add(pessoa.getApelido());
                }
            }

            if (pessoa != null) {
                doResponse(response, OK.getCode(), pessoa, null);
            } else {
                doResponse(response, NOT_FOUND.getCode(), ResponseError.builder().code(NOT_FOUND.getCode())
                        .errors(Collections.singletonList("Não encontrou pessoa com id: " + id)).build(), null);
            }
        } catch (SQLException e) {
            // e.printStackTrace();
            doResponse(response, INTERNAL_SERVER_ERRORS.getCode(),
                    ResponseError.builder().code(INTERNAL_SERVER_ERRORS.getCode())
                            .errors(Collections.singletonList("Erro ao consultar pessoa: " + e.getMessage()))
                            .build(),
                    null);
        }

    }

    protected void list(HttpServletRequest request, HttpServletResponse response) throws IOException {

        var termo = request.getParameter("t");
        if (termo != null && termo.trim().length() > 0) {
            try {
                var pessoas = this.pessoaRepository.findByText(termo);
                doResponse(response, OK.getCode(), pessoas, null);
            } catch (SQLException e) {
                // e.printStackTrace();
                doResponse(response, INTERNAL_SERVER_ERRORS.getCode(),
                        ResponseError.builder().code(INTERNAL_SERVER_ERRORS.getCode())
                                .errors(Collections.singletonList("Erro ao consultar pessoas: " + e.getMessage()))
                                .build(),
                        null);
            }
        } else {
            doResponse(response, BAD_REQUEST.getCode(), ResponseError.builder().code(BAD_REQUEST.getCode())
                    .errors(Collections.singletonList("Pamametro 't' é obrigatório")).build(), null);
        }

    }

    protected void count(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            var count = this.pessoaRepository.count();
            response.setStatus(OK.getCode());
            response.getWriter().print(count);
        } catch (SQLException e) {
            // e.printStackTrace();
            doResponse(response, INTERNAL_SERVER_ERRORS.getCode(),
                    ResponseError.builder().code(INTERNAL_SERVER_ERRORS.getCode())
                            .errors(Collections.singletonList("Erro ao consultar qtd pessoas: " + e.getMessage()))
                            .build(),
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

    private void doResponse(HttpServletResponse response, int httpStatusCode, Object bodyObject,
            Map<String, String> headerMap) throws IOException {
        if (headerMap != null) {
            headerMap.forEach((key, value) -> response.addHeader(key, value));
        }
        response.setStatus(httpStatusCode);
        response.setContentType(JSON_CONTENT_TYPE);
        JSON.writeTo(response.getOutputStream(), bodyObject);
    }
}
