package com.deso.api.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record PrevisualizacionCandidatosDTO(
		List<Fila> filas,
		long aCrear,
		long aActualizar,
		long aEliminar,
		long rechazadas,
		ResumenErrores resumenErrores) {

	public PrevisualizacionCandidatosDTO {
		filas = List.copyOf(filas);
	}

	public static PrevisualizacionCandidatosDTO desdeFilas(List<Fila> filas) {
		Map<Accion, Long> cantidades = filas.stream()
				.collect(Collectors.groupingBy(Fila::accion, Collectors.counting()));

		List<ErrorFila> errores = filas.stream()
				.flatMap(fila -> fila.errores().stream())
				.toList();

		return new PrevisualizacionCandidatosDTO(
				filas,
				cantidades.getOrDefault(Accion.CREAR, 0L),
				cantidades.getOrDefault(Accion.ACTUALIZAR, 0L),
				cantidades.getOrDefault(Accion.ELIMINAR, 0L),
				cantidades.getOrDefault(Accion.RECHAZAR, 0L),
				ResumenErrores.desdeErrores(errores));
	}

	public enum Accion {
		CREAR,
		ACTUALIZAR,
		ELIMINAR,
		RECHAZAR
	}

	public record Fila(
			long fila,
			Map<String, String> valores,
			Accion accion,
			List<ErrorFila> errores) {

		public Fila {
			valores = Collections.unmodifiableMap(new LinkedHashMap<>(valores));
			errores = List.copyOf(errores);
		}
	}

	public record ErrorFila(long fila, String campo, String mensaje) {
	}

	public record ResumenErrores(long filasConErrores, Map<String, Long> filasPorCampo) {

		public ResumenErrores {
			filasPorCampo = Collections.unmodifiableMap(new LinkedHashMap<>(filasPorCampo));
		}

		/** Cuenta una sola vez cada combinación de fila y campo. */
		public static ResumenErrores desdeErrores(List<ErrorFila> errores) {
			Map<String, Long> filasPorCampo = errores.stream()
					.map(error -> Map.entry(error.campo(), error.fila()))
					.distinct()
					.collect(Collectors.groupingBy(
							Map.Entry::getKey,
							LinkedHashMap::new,
							Collectors.counting()));

			long filasConErrores = errores.stream()
					.map(ErrorFila::fila)
					.distinct()
					.count();

			return new ResumenErrores(filasConErrores, filasPorCampo);
		}
	}
}
