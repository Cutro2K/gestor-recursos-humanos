package com.deso.api.dao.impl;

import com.deso.api.dao.PreguntaDAO;
import com.deso.api.model.entity.Pregunta;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class PreguntaPostgreSQLDAO implements PreguntaDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Pregunta> buscarActivasPorCompetencias(List<Long> competenciaIds) {
        if (competenciaIds == null || competenciaIds.isEmpty()) {
            return Collections.emptyList();
        }
        return entityManager.createQuery(
                "SELECT DISTINCT p FROM Pregunta p " +
                "JOIN FETCH p.opcionRespuesta o " +
                "LEFT JOIN FETCH o.items " +
                "WHERE p.activa = true AND p.eliminada = false " +
                "AND p.factor.eliminado = false " +
                "AND p.factor.competencia.id IN :competenciaIds " +
                "ORDER BY p.id ASC",
                Pregunta.class
        ).setParameter("competenciaIds", competenciaIds)
         .getResultList();
    }
}
