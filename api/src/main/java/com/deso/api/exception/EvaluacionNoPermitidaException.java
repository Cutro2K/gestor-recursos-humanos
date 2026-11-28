package com.deso.api.exception;

/**
 * Excepción lanzada cuando no se puede generar una evaluación de candidatos (CU 25):
 * candidatos con cuestionarios activos o en proceso (flujo 6.A) o competencias del puesto
 * que no pueden ser evaluadas (flujo 9.A). El mensaje nombra a los involucrados.
 * Se traduce en una respuesta HTTP 409 Conflict.
 */
public class EvaluacionNoPermitidaException extends RuntimeException {

    public EvaluacionNoPermitidaException(String message) {
        super(message);
    }
}
