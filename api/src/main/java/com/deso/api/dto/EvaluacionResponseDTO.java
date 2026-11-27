package com.deso.api.dto;

import com.deso.api.model.enums.EstadoCuestionario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluacionResponseDTO {

    private Long evaluacionId;
    private Long puestoId;
    private String puestoNombre;
    private LocalDateTime fechaCreacion;
    private int cantidadCuestionarios;
    private List<CuestionarioGeneradoDTO> cuestionarios;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CuestionarioGeneradoDTO {
        private Long cuestionarioId;
        private Long candidatoId;
        private String candidatoNombre;
        private String candidatoApellido;
        private String claveIngreso;
        private EstadoCuestionario estado;
        private int cantidadBloques;
        private int cantidadPreguntas;
    }
}
