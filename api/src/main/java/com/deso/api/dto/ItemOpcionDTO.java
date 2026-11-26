package com.deso.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemOpcionDTO {
    private Long id;
    private Integer orden;
    private String descripcion;
}
