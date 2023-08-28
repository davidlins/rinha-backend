package rinha.controllers;

import static java.util.Collections.singletonMap;
import static rinha.controllers.HttpStatus.BAD_REQUEST;
import static rinha.controllers.HttpStatus.CREATED;
import static rinha.controllers.HttpStatus.INTERNAL_SERVER_ERRORS;
import static rinha.controllers.HttpStatus.NOT_FOUND;
import static rinha.controllers.HttpStatus.OK;
import static rinha.controllers.HttpStatus.UNPROCESSABLE_CONTENT;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content.Source;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;

import lombok.extern.slf4j.Slf4j;
import rinha.models.Pessoa;
import rinha.models.ResponseError;
import rinha.repositories.PessoaRepository;

@Slf4j
public class PessoaController extends Handler.Abstract.NonBlocking {

    private static final String JSON_CONTENT_TYPE = "application/json";

    private PessoaRepository pessoaRepository;

    private static Map<String, String> pessoasMap = new ConcurrentHashMap<>();
    private static Set<String> apelidos = Collections.synchronizedSet(new HashSet<>());

    public PessoaController() {
        super();
        this.pessoaRepository = new PessoaRepository();
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {

        if (request.getMethod().equals("POST")) {
            return insert(request, response, callback);
        }

        var uri = request.getHttpURI();
        
//        System.out.println("==========================");
//        
//        System.out.println("uri.getCanonicalPath() >>> "+uri.getCanonicalPath());
//        System.out.println("uri.getDecodedPath() >>> "+uri.getDecodedPath());
//        System.out.println("uri.getPath() >>> "+uri.getPath());
//        System.out.println("uri.getPathQuery() >>> "+uri.getPathQuery());
//        System.out.println("uri.getParam() >>> "+uri.getParam());
//        System.out.println("uri.getQuery() >>> "+uri.getQuery());
//        
//        System.out.println("==========================");
//        
        
        if (uri.getQuery() != null) {
            return list(uri.getQuery(), response, callback);
        }

        if (uri.getPath().startsWith("/contagem-pessoas")) {
            return count(request, response, callback);
        }

        return show(uri.getPath().replaceFirst("/pessoas", "").replaceFirst("/",""), request, response, callback);

    }

    private boolean insert(Request request, Response response, Callback callback) throws Exception {

        try {

            Pessoa pessoa = JSON.parseObject(Source.asInputStream(request), Pessoa.class);

            var responseError = validade(pessoa);
            if (responseError == null) {
                try {

                    pessoa.setId(UUID.randomUUID().toString());
                    this.pessoaRepository.save(pessoa);

                    updateCache(pessoa);

                    return doResponse(response, CREATED, pessoa, null,
                            singletonMap("Location", "/pessoas/" + pessoa.getId()), callback);

                } catch (Exception e) {

                    var httpStatus = INTERNAL_SERVER_ERRORS;

                    if (e.getMessage().indexOf("pessoas_apelido_key") >= 0) {

                        httpStatus = UNPROCESSABLE_CONTENT;
                        updateCache(pessoa);

                    } else {

                        log.error(e.getMessage(), e);
                    }

                    return doResponse(response, httpStatus, null,
                            null, null, callback);
                }
            } else {
                return doResponse(response, responseError.getCode(), null,
                        null, null, callback);
            }

        } catch (JSONException e) {
            return doResponse(response, BAD_REQUEST, null,
                    null, null, callback);
        }

    }

    private boolean show(String id, Request request, Response response, Callback callback) throws Exception {


        if (id == null || id.trim().length() == 0) {

            return doResponse(response, BAD_REQUEST, null,
                    null, null, callback);

        }
        
        
        var pessoaCache = pessoasMap.get(id);
        if (pessoaCache != null) {

            return doResponse(response, HttpStatus.OK, null, pessoaCache, null, callback);

        } else {

            var pessoa = this.pessoaRepository.findById(id);

            if (pessoa != null && !apelidos.contains(pessoa.getApelido())) {
                updateCache(pessoa);
            }

            return doResponse(response, (pessoa != null) ? HttpStatus.OK : NOT_FOUND, pessoa, null, null, callback);
        }

    }

    private boolean list(String querystring, Response response, Callback callback) throws Exception {

        var termo = (querystring == null || !querystring.startsWith("t=")) ? null : querystring.replaceFirst("t=", "");
        if (termo == null) {

            return doResponse(response, BAD_REQUEST, null,
                    null, null, callback);

        }

        return doResponse(response, OK, this.pessoaRepository.findByText(termo), null, null, callback);

    }

    private boolean count(Request request, Response response, Callback callback) throws Exception {

        return doResponse(response, UNPROCESSABLE_CONTENT, null, String.valueOf(this.pessoaRepository.count()), null,
                callback);
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

    private boolean doResponse(Response response, HttpStatus httpStatus, Object bodyObject,
            String json,
            Map<String, String> headerMap,
            Callback callback) throws IOException {

        response.setStatus(httpStatus.getCode());
        response.getHeaders().put(HttpHeader.CONTENT_TYPE, JSON_CONTENT_TYPE);

        if (headerMap != null) {
            headerMap.forEach((key, value) -> response.getHeaders().put(key, value));
        }

        if (bodyObject != null) {
            var out = new ByteArrayOutputStream();
            JSON.writeTo(out, bodyObject);

            response.write(true, BufferUtil.toBuffer(out.toByteArray()), callback);

        } else {
            response.write(true, BufferUtil.toBuffer((json != null) ? json : "{}"), callback);

        }
        callback.succeeded();
        return true;

    }
}
