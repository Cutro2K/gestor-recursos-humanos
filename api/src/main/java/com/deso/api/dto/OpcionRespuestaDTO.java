package com.deso.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres.")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres.")
    private String descripcion;

    @NotNull(message = "Debe ingresar al menos dos ítems de opción.")
    @Size(min = 2, message = "Debe ingresar al menos dos ítems de opción.")
    private List<@NotNull(message = "El ítem de opción no puede ser nulo.") @Valid ItemOpcionDTO> items;
}
