package com.deso.api.dto;

/**
 * Respuesta de GET /api/opciones-respuesta/{id}/uso (CU 31, paso 2): indica si la opción
 * de respuesta tiene preguntas activas asociadas y, por lo tanto, no puede darse de baja.
 */
public record UsoOpcionRespuestaDTO(boolean enUso) {
}
