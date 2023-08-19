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
import java.util.ArrayList;
import java.util.List;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import rinha.models.Pessoa;

public class PessoaRepository {

    private HikariDataSource ds = new HikariDataSource(new HikariConfig("/hikari.properties"));
    private static final String SQL_INSERT = "insert into pessoas (apelido, nome, nascimento, stack) values (?, ?, ?, ?)";
    private static final String SQL_FIND_BY_ID = "select apelido, nome, nascimento, stack  from pessoas where id = '";
    private static final String SQL_FIND_BY_TERMO = "select id, apelido, nome, nascimento, stack  from pessoas where text_searchable like ''%{0}%'' limit 50";
    private static final String SQL_COUNT = "select count(*) from pessoas";

    public void save(Pessoa pessoa) throws SQLException {

        try (Connection conn = ds.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT,
                        Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, pessoa.getApelido());
            pstmt.setString(2, pessoa.getNome());
            pstmt.setDate(3, Date.valueOf(pessoa.getNascimento()));
            pstmt.setString(4,
                    (pessoa.getStack() != null && !pessoa.getStack().isEmpty()
                            ? pessoa.getStack().stream().collect(joining("|"))
                            : null));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        pessoa.setId(rs.getString(1));
                    }
                }
            }
        }
    }

    public Pessoa findById(String id) throws SQLException {

        Pessoa pessoa = null;

        try (Connection conn = ds.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(SQL_FIND_BY_ID + id + "'");) {

            if (rs.next()) {

                String stack = rs.getString("stack");

                pessoa = Pessoa.builder().id(id).apelido(rs.getString("apelido")).nome(rs.getString("nome"))
                        .nascimento(rs.getDate("nascimento").toLocalDate())
                        .stack((stack != null) ? asList(stack.split("\\|")) : null)
                        .build();
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

                String stack = rs.getString("stack");

                pessoas.add(Pessoa.builder().id(rs.getString("id")).apelido(rs.getString("apelido"))
                        .nome(rs.getString("nome"))
                        .nascimento(rs.getDate("nascimento").toLocalDate())
                        .stack((stack != null) ? asList(stack.split("\\|")) : null)
                        .build());
            }

        }

        return pessoas;
    }

    public int count() throws SQLException {

        int count = 0;
        try (Connection conn = ds.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(SQL_COUNT);) {

            if (rs.next()) {
                count = rs.getInt(1);
            }
        }

        return count;
    }
}
