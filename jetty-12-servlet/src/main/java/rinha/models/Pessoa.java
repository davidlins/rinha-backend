package rinha.models;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Pessoa {

    private String id;
    private String apelido;
    private String nome;
    private LocalDate nascimento;
    private List<String> stack;
}
