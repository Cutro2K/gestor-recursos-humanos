package com.deso.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpcionRespuestaDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private List<ItemOpcionDTO> items;
}
