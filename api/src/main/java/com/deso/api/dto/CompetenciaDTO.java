package com.deso.api.dto;

import com.deso.api.model.enums.TipoCompetencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object para la entidad Competencia (CU 13).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetenciaDTO {

    private Long id;

    @NotBlank(message = "El código de la competencia es obligatorio.")
    @Size(max = 20, message = "El código no puede tener más de 20 caracteres.")
    private String codigo;

    @NotBlank(message = "El nombre de la competencia es obligatorio.")
    @Size(max = 75, message = "El nombre no puede tener más de 75 caracteres.")
    private String nombre;

    @NotNull(message = "El tipo de competencia es obligatorio (TECNICA o ACTITUDINAL).")
    private TipoCompetencia tipo;

    @Size(max = 1000, message = "La descripción no puede tener más de 1000 caracteres.")
    private String descripcion;

    @Builder.Default
    private Boolean eliminada = false;
}
