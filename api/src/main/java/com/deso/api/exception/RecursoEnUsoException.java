package com.deso.api.exception;

/**
 * Excepción lanzada cuando se intenta dar de baja una entidad de catálogo que está
 * siendo utilizada por al menos una entidad dependiente activa (ej. Opción de Respuesta
 * asociada a una Pregunta activa en CU 31).
 * Se traduce en una respuesta HTTP 409 Conflict.
 */
public class RecursoEnUsoException extends RuntimeException {

    public RecursoEnUsoException(String message) {
        super(message);
    }

    public RecursoEnUsoException(String message, Throwable cause) {
        super(message, cause);
    }
}
