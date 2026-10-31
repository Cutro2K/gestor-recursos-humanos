package com.deso.api.dto;

import com.deso.api.model.enums.TipoDocumento;

import java.util.List;

/** Página de candidatos activos. siguienteCursor es null cuando no quedan más resultados. */
public record ListadoCandidatosDTO(List<CandidatoResumen> candidatos, Long siguienteCursor) {

	public ListadoCandidatosDTO {
		candidatos = List.copyOf(candidatos);
	}

	public record CandidatoResumen(
			Long id,
			Long nroCandidato,
			String apellido,
			String nombre,
			TipoDocumento tipoDocumento,
			String nroDocumento,
			String email) {
	}
}
