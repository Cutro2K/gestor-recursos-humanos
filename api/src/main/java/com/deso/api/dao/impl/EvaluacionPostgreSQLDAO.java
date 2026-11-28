package com.deso.api.dao.impl;

import com.deso.api.dao.EvaluacionDAO;
import com.deso.api.model.entity.Evaluacion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class EvaluacionPostgreSQLDAO implements EvaluacionDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Evaluacion guardar(Evaluacion evaluacion) {
        if (evaluacion.getId() == null) {
            entityManager.persist(evaluacion);
            return evaluacion;
        }
        return entityManager.merge(evaluacion);
    }

    @Override
    public Optional<Evaluacion> buscarConCuestionarios(Long id) {
        return entityManager.createQuery("""
                SELECT DISTINCT e FROM Evaluacion e
                JOIN FETCH e.puesto
                LEFT JOIN FETCH e.cuestionarios cu
                LEFT JOIN FETCH cu.candidato
                WHERE e.id = :id
                """, Evaluacion.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }
}
