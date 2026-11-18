package com.deso.api.exception;

/**
 * Excepción lanzada cuando se consulta o intenta operar sobre una entidad que existe
 * en la base de datos pero se encuentra dada de baja lógica (eliminado/eliminada = true).
 * Se traduce en una respuesta HTTP 404 Not Found.
 */
public class RecursoActualmenteEliminadoException extends RuntimeException {

    public RecursoActualmenteEliminadoException(String message) {
        super(message);
    }

    public RecursoActualmenteEliminadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
