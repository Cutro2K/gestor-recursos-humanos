package com.deso.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de salida utilizado para el listado paginado de Gestión de Opciones de Respuesta (CU 24).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListadoOpcionRespuestaDTO {
    private Long id;
    private String nombre;
    private String descripcion;
}
