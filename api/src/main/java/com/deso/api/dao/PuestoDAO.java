package com.deso.api.dao;

import com.deso.api.model.entity.Puesto;

import java.util.List;
import java.util.Optional;

public interface PuestoDAO {
    List<Puesto> listarActivos();
    Optional<Puesto> buscarPorId(Long id);
    List<Long> obtenerCompetenciaIdsPorPuesto(Long puestoId);
}
