package com.deso.api.dao.impl;

import com.deso.api.dao.PuestoDAO;
import com.deso.api.model.entity.Puesto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PuestoPostgreSQLDAO implements PuestoDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Puesto> listarActivos() {
        return entityManager.createQuery(
                "SELECT p FROM Puesto p WHERE p.eliminado = false ORDER BY p.nombre ASC",
                Puesto.class
        ).getResultList();
    }

    @Override
    public Optional<Puesto> buscarPorId(Long id) {
        return entityManager.createQuery(
                "SELECT p FROM Puesto p WHERE p.id = :id AND p.eliminado = false",
                Puesto.class
        ).setParameter("id", id)
         .getResultStream()
         .findFirst();
    }

    @Override
    public List<Long> obtenerCompetenciaIdsPorPuesto(Long puestoId) {
        @SuppressWarnings("unchecked")
        List<Number> rows = entityManager.createNativeQuery(
                "SELECT competencia_id FROM puesto_competencia WHERE puesto_id = :puestoId"
        ).setParameter("puestoId", puestoId)
         .getResultList();
        return rows.stream().map(Number::longValue).toList();
    }
}
