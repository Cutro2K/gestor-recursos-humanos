package com.deso.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body de POST /api/evaluaciones (CU 25): puesto elegido y candidatos a evaluar.
 * El consultor no viaja en el body: sale de la sesión.
 */
public record EvaluacionDTO(
        @NotNull(message = "Debe seleccionar una función o puesto.")
        @Positive(message = "El ID del puesto debe ser positivo.")
        Long puestoId,

        @NotEmpty(message = "Debe seleccionar uno o más candidatos.")
        @Size(max = 500, message = "No se pueden evaluar más de 500 candidatos a la vez.")
        List<@NotNull(message = "El ID de candidato no puede ser nulo.")
             @Positive(message = "El ID de candidato debe ser positivo.") Long> candidatoIds
) {
}
