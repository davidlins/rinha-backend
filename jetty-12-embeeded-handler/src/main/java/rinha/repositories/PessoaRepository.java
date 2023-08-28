package rinha.repositories;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;
import rinha.models.Pessoa;

@Slf4j
public class PessoaRepository {

    private static final String SQL_INSERT = "insert into pessoas (id, apelido, nome, nascimento, stack, text_searchable) values (?, ?, ?, ?, ?, ?)";
    private static final String SQL_FIND_BY_ID = "select id, apelido, nome, nascimento, stack  from pessoas where id = ''{0}''";
    private static final String SQL_FIND_BY_TERMO = "select id, apelido, nome, nascimento, stack  from pessoas where text_searchable like ''%{0}%'' limit 50";
    private static final String SQL_COUNT = "select count(*) from pessoas";

    private DataSource ds;

    public PessoaRepository() {
        super();
        this.ds = DatasorceFactory.createDataSourceHikari();
    }

    public void save(Pessoa pessoa) throws SQLException {

        var staks = (pessoa.getStack() != null && !pessoa.getStack().isEmpty()
                ? pessoa.getStack().stream().collect(joining("|"))
                : null);

        var searchable = pessoa.getApelido().toLowerCase() + " " + pessoa.getNome().toLowerCase() + " "
                + ((staks != null) ? staks.toLowerCase() : "");

        try (Connection conn = ds.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT,
                        Statement.RETURN_GENERATED_KEYS)) {

            var index = 1;
            pstmt.setString(index++, pessoa.getId());
            pstmt.setString(index++, pessoa.getApelido());
            pstmt.setString(index++, pessoa.getNome());
            pstmt.setDate(index++, Date.valueOf(pessoa.getNascimento()));
            pstmt.setString(index++, staks);
            pstmt.setString(index++, searchable);

            pstmt.executeUpdate();
        }
    }

    public Pessoa findById(String id) throws SQLException {

        Pessoa pessoa = null;

        try (Connection conn = ds.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(MessageFormat.format(SQL_FIND_BY_ID, id));) {

            if (rs.next()) {
                pessoa = buidPessoa(rs);
            }
        }
        return pessoa;
    }

    public List<Pessoa> findByText(String termo) throws SQLException {

        var pessoas = new ArrayList<Pessoa>();

        var query = MessageFormat.format(SQL_FIND_BY_TERMO, termo.toLowerCase());
        try (Connection conn = ds.getConnection();
                PreparedStatement stmt = conn
                        .prepareStatement(query);
                ResultSet rs = stmt.executeQuery();) {

            while (rs.next()) {
                pessoas.add(buidPessoa(rs));
            }
        }

        return pessoas;
    }

    public int count() throws SQLException {

        try (Connection conn = ds.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(SQL_COUNT);) {

            return (rs.next()) ? rs.getInt(1) : 0;
        }
    }

    private Pessoa buidPessoa(ResultSet rs) throws SQLException {

        var stack = rs.getString("stack");

        return Pessoa.builder().id(rs.getString("id")).apelido(rs.getString("apelido"))
                .nome(rs.getString("nome"))
                .nascimento(rs.getDate("nascimento").toLocalDate())
                .stack((stack != null) ? asList(stack.split("\\|")) : null)
                .build();
    }
}
