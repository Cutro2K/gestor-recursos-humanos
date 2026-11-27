package com.deso.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class PreguntaDTO {

    private Long id;

    @NotNull(message = "El factor es obligatorio.")
    private Long factorId;

    @NotNull(message = "La opción de respuesta es obligatoria.")
    private Long opcionRespuestaId;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres.")
    private String nombre;

    @NotBlank(message = "El enunciado es obligatorio.")
    @Size(max = 255, message = "El enunciado no puede superar los 255 caracteres.")
    private String enunciado;

    private String descripcion;

    private Boolean activa;

    @NotEmpty(message = "Debe asignar ponderaciones a los ítems de la opción de respuesta.")
    @Valid
    private List<PonderacionDTO> ponderaciones;
}