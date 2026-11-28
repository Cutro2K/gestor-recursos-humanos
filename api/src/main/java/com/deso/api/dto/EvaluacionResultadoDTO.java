package com.deso.api.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de POST /api/evaluaciones (CU 25): evaluación generada y las claves de ingreso
 * de cada candidato. {@code nombreArchivoExcel} es el nombre con el que se exportan las claves.
 */
public record EvaluacionResultadoDTO(
        Long evaluacionId,
        Long puestoId,
        String puestoNombre,
        String empresa,
        LocalDateTime fechaCreacion,
        String nombreArchivoExcel,
        List<CredencialCuestionarioDTO> credenciales
) {
}
