package rinha.models;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import rinha.models.valitador.Stack;

@Data
@Builder
@AllArgsConstructor
public class Pessoa {

    private String id;
    
    @NotNull(message = "Apelido é obrigatório.") 
    @Size(max = 32, message = "Apelido dever ser menor que 32 caracters.")
    private String apelido;
   
    @NotNull(message = "Nome é obrigatório.") 
    @Size(max = 100, message = "Nome dever ser menor que 100 caracters.")
    private String nome;
    
    @Past
    @NotNull(message = "Nascimento é obrigatório.") 
    private LocalDate nascimento;
   
    @Stack()
    private List<String> stack;

}
