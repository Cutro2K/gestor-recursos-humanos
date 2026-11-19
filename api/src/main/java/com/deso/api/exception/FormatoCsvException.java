package com.deso.api.exception;

/**
 * Excepción lanzada en la previsualización o importación de candidatos por CSV (CU06)
 * cuando el archivo presenta un error estructural (codificación inválida, archivo vacío,
 * columnas faltantes o delimitador incorrecto) que impide continuar procesando las filas.
 * Se traduce en una respuesta HTTP 400 Bad Request.
 */
public class FormatoCsvException extends IllegalArgumentException {

	public FormatoCsvException(String mensaje) {
		super(mensaje);
	}

	public FormatoCsvException(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}
}
