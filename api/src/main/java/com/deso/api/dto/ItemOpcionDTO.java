package com.deso.api.dto;

import com.deso.api.validation.OnModificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemOpcionDTO {

    @NotNull(groups = OnModificacion.class, message = "El ID del ítem de opción es obligatorio.")
    @Positive(groups = OnModificacion.class, message = "El ID del ítem de opción debe ser positivo.")
    private Long id;

    @NotNull(message = "El orden del ítem de opción es obligatorio.")
    @Positive(message = "El orden del ítem de opción debe ser un número positivo.")
    private Integer orden;

    @NotBlank(message = "La descripción del ítem de opción es obligatoria.")
    @Size(max = 150, message = "La descripción del ítem de opción no puede superar los 150 caracteres.")
    private String descripcion;
}
