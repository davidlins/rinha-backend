package rinha.models;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ResponseError {

    private int code;
    private List<String> errors;

}
