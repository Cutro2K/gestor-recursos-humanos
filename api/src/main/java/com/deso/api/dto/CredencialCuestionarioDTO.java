package com.deso.api.dto;

import com.deso.api.model.enums.TipoDocumento;

/**
 * Candidato evaluado con la clave de ingreso de su cuestionario (CU 25, paso 11).
 */
public record CredencialCuestionarioDTO(
        Long candidatoId,
        String apellido,
        String nombre,
        TipoDocumento tipoDocumento,
        String nroDocumento,
        String claveIngreso
) {
}
