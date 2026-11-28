package com.deso.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PonderacionDTO {

    @NotNull(message = "El ítem de opción es obligatorio.")
    private Long itemOpcionId;

    @NotNull(message = "La ponderación es obligatoria.")
    @DecimalMin(value = "0.0", inclusive = true, message = "La ponderación no puede ser negativa.")
    private BigDecimal ponderacion;
}