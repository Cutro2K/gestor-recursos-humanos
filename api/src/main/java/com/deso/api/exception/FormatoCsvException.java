package com.deso.api.exception;

/** Error de estructura: no es seguro continuar interpretando las filas del archivo. */
public class FormatoCsvException extends IllegalArgumentException {

	public FormatoCsvException(String mensaje) {
		super(mensaje);
	}

	public FormatoCsvException(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}
}
