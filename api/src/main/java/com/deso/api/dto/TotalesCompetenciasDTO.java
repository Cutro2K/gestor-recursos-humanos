package com.deso.api.dto;

/**
 * Totales del catálogo de competencias activas (CU 13), independientes de los filtros del listado.
 *
 * @param total         Cantidad de competencias activas.
 * @param tecnicas      Cantidad de competencias activas de tipo TECNICA.
 * @param actitudinales Cantidad de competencias activas de tipo ACTITUDINAL.
 */
public record TotalesCompetenciasDTO(long total, long tecnicas, long actitudinales) {
}
