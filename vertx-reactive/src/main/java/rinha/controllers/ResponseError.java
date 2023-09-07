package rinha.controllers;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ResponseError {

    private HttpStatus code;
    private List<String> errors;

}
