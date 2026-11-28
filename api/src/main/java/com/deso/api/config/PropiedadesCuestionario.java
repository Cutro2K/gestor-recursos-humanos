package com.deso.api.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Parámetros operativos de los cuestionarios (CU 25, CU 26 y CU 32),
 * leídos de {@code cuestionario.*} en application.properties.
 */
@Validated
@ConfigurationProperties("cuestionario")
public record PropiedadesCuestionario(
        @NotBlank String instrucciones,
        @Positive int cantidadMaximaAccesos,
        @Positive int minutosMaximosActivo,
        @Positive int minutosMaximosResolucion,
        @Positive int preguntasPorBloque
) {
}
