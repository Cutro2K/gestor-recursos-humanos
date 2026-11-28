package com.deso.api.dto;

import java.math.BigDecimal;

/**
 * Competencia de un puesto con la ponderación requerida (CU 25, paso 9). {@code evaluable}
 * es false cuando no tiene ningún factor con al menos 2 preguntas (flujo 9.A).
 */
public record CompetenciaPuestoDTO(
        Long competenciaId,
        String codigo,
        String nombre,
        BigDecimal ponderacionMinima,
        boolean evaluable
) {
}
