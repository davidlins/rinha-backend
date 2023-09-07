package rinha.repositories;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

import java.text.MessageFormat;
import java.util.UUID;
import java.util.function.Function;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.MaybeSource;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.pgclient.PgPool;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.RowSet;
import io.vertx.rxjava3.sqlclient.Tuple;
import rinha.models.Pessoa;

public class PessoaRepository {

    private final PgPool client;

    private static final String SQL_INSERT = "insert into pessoas (id, apelido, nome, nascimento, stack, text_searchable) values ($1, $2, $3, $4, $5, $6)";
    private static final String SQL_FIND_BY_ID = "select id, apelido, nome, nascimento, stack  from pessoas where id = $1";
    private static final String SQL_FIND_BY_TERMO = "select id, apelido, nome, nascimento, stack  from pessoas where text_searchable like ''%{0}%'' limit 50";
    private static final String SQL_COUNT = "select count(*) from pessoas";

    public PessoaRepository(PgPool client) {
        this.client = client;
    }

    public Maybe<Pessoa> save(Pessoa pessoa) {

        pessoa.setId(UUID.randomUUID().toString());

        var staks = (pessoa.getStack() != null && pessoa.getStack().size() > 0)
                ? pessoa.getStack().stream().collect(joining("|"))
                : null;

        var searchable = pessoa.getApelido().toLowerCase() + " " + pessoa.getNome().toLowerCase() + " "
                + ((staks != null) ? staks.toLowerCase() : "");

        return client.preparedQuery(SQL_INSERT).rxExecute(Tuple.of(pessoa.getId(),
                pessoa.getApelido(),
                pessoa.getNome(),
                pessoa.getNascimento(),
                staks,
                searchable))
                .flatMapMaybe( rows -> Maybe.just(pessoa));
    }

    public Single<Pessoa> findById(String id) {

        return client.preparedQuery(SQL_FIND_BY_ID).rxExecute(Tuple.of(id))
                .map(RowSet::iterator)
                .flatMap(iterator -> (iterator != null && iterator.hasNext())
                        ? Single.just(MAPPER.apply(iterator.next()))
                        : Single.error(() -> new RuntimeException()));

    }

    public Flowable<Pessoa> findByText(String termo) {

        var query = MessageFormat.format(SQL_FIND_BY_TERMO, termo.toLowerCase());

        return client.preparedQuery(query).rxExecute()
                .flattenAsFlowable(
                        rows -> stream(rows.spliterator(), false)
                                .map(MAPPER)
                                .collect(toList()));
    }

    public Single<Integer> count() {
        return client.preparedQuery(SQL_COUNT).rxExecute()
                .map(RowSet::iterator)
                .flatMap(iterator -> Single.just(iterator.next().getInteger("count")));
    }

    private static Function<Row, Pessoa> MAPPER = ((row) -> {

        var stack = row.getString("stack");

        return Pessoa.builder().id(row.getString("id"))
                .nome(row.getString("nome"))
                .apelido(row.getString("apelido"))
                .nascimento(row.getLocalDate("nascimento"))
                .stack((stack != null) ? asList(stack.split("\\|")) : null)
                .build();
    });

}
