package com.deso.api.dao;

import com.deso.api.model.entity.Evaluacion;

import java.util.Optional;

/**
 * Contrato de acceso a datos de las evaluaciones (CU 25).
 */
public interface EvaluacionDAO {

    /** Inserta la evaluación junto con su modelo y sus cuestionarios (en cascada). */
    Evaluacion guardar(Evaluacion evaluacion);

    /** Busca una evaluación con su puesto y sus cuestionarios (con candidato) ya cargados. */
    Optional<Evaluacion> buscarConCuestionarios(Long id);
}
