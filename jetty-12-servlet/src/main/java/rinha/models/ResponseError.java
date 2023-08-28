package rinha.models;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import rinha.controllers.HttpStatus;

@Builder
@Getter
public class ResponseError {

    private HttpStatus code;
    private List<String> errors;

}
