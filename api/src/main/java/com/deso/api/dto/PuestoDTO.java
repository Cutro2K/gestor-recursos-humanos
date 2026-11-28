package com.deso.api.dto;

/**
 * Puesto que se puede elegir para evaluar candidatos (CU 25, paso 8).
 */
public record PuestoDTO(Long id, String codigo, String nombre, String empresa) {
}
