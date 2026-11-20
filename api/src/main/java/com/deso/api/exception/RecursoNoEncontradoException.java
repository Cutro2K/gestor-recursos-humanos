package com.deso.api.exception;

/**
 * Excepción lanzada al consultar, modificar o eliminar una entidad por su identificador
 * o clave de negocio cuando el registro no existe en la base de datos o fue dado de baja lógica.
 * Se traduce en una respuesta HTTP 404 Not Found.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }

    public RecursoNoEncontradoException(String message, Throwable cause) {
        super(message, cause);
    }
}
