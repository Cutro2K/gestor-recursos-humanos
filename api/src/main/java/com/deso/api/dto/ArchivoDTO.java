package com.deso.api.dto;

/**
 * Archivo generado por el sistema para descargar (CU 25, exportación de claves a Excel).
 * En la web la carpeta destino la elige el navegador, por eso no lleva ruta.
 */
public record ArchivoDTO(String nombre, String tipoMime, byte[] contenido) {
}
