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

    private String codigo;
    private String nombreFactor;
    private String nombreCompetencia;
    private Integer orden;
    private String descripcion;
}
