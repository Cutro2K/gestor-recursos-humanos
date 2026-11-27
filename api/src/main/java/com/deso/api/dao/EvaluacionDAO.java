package com.deso.api.dao;

import com.deso.api.model.entity.Evaluacion;

import java.util.Optional;

public interface EvaluacionDAO {
    Evaluacion guardar(Evaluacion evaluacion);
    Optional<Evaluacion> buscarPorId(Long id);
}
