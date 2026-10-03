package com.rexxo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RexxoException extends RuntimeException {
    private final HttpStatus status;

    public RexxoException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public static RexxoException notFound(String message) {
        return new RexxoException(message, HttpStatus.NOT_FOUND);
    }

    public static RexxoException badRequest(String message) {
        return new RexxoException(message, HttpStatus.BAD_REQUEST);
    }

    public static RexxoException unauthorized(String message) {
        return new RexxoException(message, HttpStatus.UNAUTHORIZED);
    }

    public static RexxoException conflict(String message) {
        return new RexxoException(message, HttpStatus.CONFLICT);
    }

    public static RexxoException forbidden(String message) {
        return new RexxoException(message, HttpStatus.FORBIDDEN);
    }

    public static RexxoException tooManyRequests(String message) {
        return new RexxoException(message, HttpStatus.TOO_MANY_REQUESTS);
    }

    public static RexxoException internal(String message) {
        return new RexxoException(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
