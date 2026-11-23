package com.deso.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de salida utilizado para el resultado de búsqueda por nombres en Gestión de Factores (CU16).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListadoGestorFactoresDTO {
    private Long id;
    private String codigo;
    private String nombreFactor;
    private Long competenciaId;
    private String nombreCompetencia;
    private Integer orden;
    private String descripcion;
}
