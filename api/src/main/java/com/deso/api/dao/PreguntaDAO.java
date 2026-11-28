package com.deso.api.dao;

import com.deso.api.model.entity.Pregunta;
import com.deso.api.model.entity.PreguntaItemOpcion;

import java.util.Collection;
import java.util.List;

/**
 * Contrato de acceso a las preguntas del catálogo que usa la evaluación de candidatos (CU 25).
 */
public interface PreguntaDAO {

    /**
     * Preguntas activas y no eliminadas de factores no eliminados de esas competencias, con su
     * factor, competencia, opción de respuesta e ítems ya cargados.
     */
    List<Pregunta> buscarActivasPorCompetencias(Collection<Long> competenciaIds);

    /** Ponderaciones que otorga cada ítem de opción en esas preguntas (pregunta_item_opcion). */
    List<PreguntaItemOpcion> buscarPonderaciones(Collection<Long> preguntaIds);
}
