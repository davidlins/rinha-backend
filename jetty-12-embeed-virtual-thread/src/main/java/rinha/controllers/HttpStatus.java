package rinha.controllers;

import lombok.Getter;

@Getter
public enum HttpStatus {

    OK(200, "OK"),
    CREATED(201, "Created"), 
    BAD_REQUEST(400, "Bad request"), 
    NOT_FOUND(404, "Not Found"),
    UNPROCESSABLE_CONTENT(422, "Unprocessable Content"), 
    INTERNAL_SERVER_ERRORS(500, "Internal Server Error");

    private int code;
    private String text;

    private HttpStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }
}
