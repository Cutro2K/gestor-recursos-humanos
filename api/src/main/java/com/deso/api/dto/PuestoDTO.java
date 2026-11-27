package com.deso.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PuestoDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private String empresa;
    private String descripcion;
}
