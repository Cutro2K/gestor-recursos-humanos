package com.deso.api.exception;

import lombok.Getter;

/**
 * Excepción lanzada al dar de alta o modificar entidades de catálogo (ej. Factor en CU17/CU18,
 * Competencia en CU13, Puesto o Pregunta) cuando se intenta registrar un valor que ya existe para un
 * atributo único (como código, nombre o número de orden dentro de una competencia).
 * Se traduce en una respuesta HTTP 409 Conflict.
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
