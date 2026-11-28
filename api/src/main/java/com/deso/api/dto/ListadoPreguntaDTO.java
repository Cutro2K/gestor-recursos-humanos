package com.deso.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListadoPreguntaDTO {
    private Long id;
    private String nombre;
    private String enunciado;
    private Long factorId;
    private String nombreFactor;
    private Long competenciaId;
    private String nombreCompetencia;
    private Long opcionRespuestaId;
    private String nombreOpcionRespuesta;
    private Boolean activa;
}