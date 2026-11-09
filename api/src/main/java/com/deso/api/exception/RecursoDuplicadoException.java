package com.deso.api.exception;

public class RecursoDuplicadoException extends RuntimeException {

    public RecursoDuplicadoException(String message) {
        super(message);
    }

    public RecursoDuplicadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
