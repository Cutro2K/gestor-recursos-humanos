package com.deso.api.dto;

import com.deso.api.dto.PrevisualizacionCandidatosDTO.ErrorFila;
import com.deso.api.dto.PrevisualizacionCandidatosDTO.ResumenErrores;

import java.util.List;

public record ResultadoImportacionCandidatosDTO(
		long creados,
		long actualizados,
		long eliminados,
		long rechazados,
		List<ErrorFila> errores,
		ResumenErrores resumenErrores) {

	public ResultadoImportacionCandidatosDTO {
		errores = List.copyOf(errores);
	}
}
