package com.deso.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluacionRequestDTO {

    @NotNull(message = "El puesto es obligatorio.")
    private Long puestoId;

    @NotEmpty(message = "Debe seleccionar al menos un candidato.")
    private List<@NotNull(message = "El ID de candidato no puede ser nulo.") Long> candidatoIds;

    private Long consultorId;

    private String versionModelo;

    private String nombreArchivoExcel;
}
