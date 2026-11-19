package com.deso.api.exception;

import lombok.Getter;

/**
 * Excepción lanzada cuando se intenta registrar un recurso con un valor único ya existente (código o nombre).
 */
@Getter
public class RecursoDuplicadoException extends RuntimeException {

    private final String campo;

    public RecursoDuplicadoException(String message) {
        super(message);
        this.campo = null;
    }

    public RecursoDuplicadoException(String campo, String message) {
        super(message);
        this.campo = campo;
    }
}
