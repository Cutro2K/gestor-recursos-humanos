package com.deso.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body de POST /api/evaluaciones/verificacion-candidatos (CU 25, paso 6): candidatos de la
 * grilla "Candidatos a evaluar" que se quieren evaluar.
 */
public record CandidatosAEvaluarDTO(
        @NotEmpty(message = "Debe seleccionar uno o más candidatos.")
        @Size(max = 500, message = "No se pueden evaluar más de 500 candidatos a la vez.")
        List<@NotNull(message = "El ID de candidato no puede ser nulo.")
             @Positive(message = "El ID de candidato debe ser positivo.") Long> candidatoIds
) {
}
