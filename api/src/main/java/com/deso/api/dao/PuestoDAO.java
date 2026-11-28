package com.deso.api.dao;

import com.deso.api.model.entity.Competencia;
import com.deso.api.model.entity.Puesto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos de los puestos que usa la evaluación de candidatos (CU 25).
 */
public interface PuestoDAO {

    /** Competencia que requiere un puesto junto con la ponderación mínima exigida. */
    record CompetenciaPonderada(Competencia competencia, BigDecimal ponderacionMinima) {}

    /** Lista los puestos no eliminados ordenados por nombre. */
    List<Puesto> listarActivos();

    /** Busca un puesto no eliminado por su ID. */
    Optional<Puesto> buscarPorId(Long id);

    /** Competencias asociadas al puesto con su ponderación mínima, ordenadas por nombre. */
    List<CompetenciaPonderada> listarCompetenciasRequeridas(Long puestoId);
}
