package com.jawaher.GPT.experiment.web;

import org.springframework.http.HttpStatus;

/** Expected, user-facing error (bad input, limit reached, unauthorised...). */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
