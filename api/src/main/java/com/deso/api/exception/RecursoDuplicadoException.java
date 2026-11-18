package com.deso.api.exception;

/**
 * Excepción lanzada al dar de alta o modificar entidades de catálogo (ej. Factor en CU17/CU18,
 * Competencia, Puesto o Pregunta) cuando se intenta registrar un valor que ya existe para un
 * atributo único (como código, nombre o número de orden dentro de una competencia).
 * Se traduce en una respuesta HTTP 409 Conflict.
 */
public class RecursoDuplicadoException extends RuntimeException {

    public RecursoDuplicadoException(String message) {
        super(message);
    }

    public RecursoDuplicadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
