package com.deso.api.dao.impl;

import com.deso.api.dao.PreguntaDAO;
import com.deso.api.model.entity.OpcionRespuesta;
import com.deso.api.model.entity.Pregunta;
import com.deso.api.model.entity.PreguntaItemOpcion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class PreguntaPostgreSQLDAO implements PreguntaDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Pregunta> buscarActivasPorCompetencias(Collection<Long> competenciaIds) {
        if (competenciaIds == null || competenciaIds.isEmpty()) {
            return List.of();
        }
        List<Pregunta> preguntas = entityManager.createQuery("""
                SELECT p FROM Pregunta p
                JOIN FETCH p.factor f
                JOIN FETCH f.competencia c
                JOIN FETCH p.opcionRespuesta o
                WHERE p.activa = true AND p.eliminada = false
                  AND f.eliminado = false
                  AND c.id IN :competenciaIds
                ORDER BY p.id ASC
                """, Pregunta.class)
                .setParameter("competenciaIds", competenciaIds)
                .getResultList();

        // Los ítems se traen aparte, una fila por opción: varias preguntas comparten la misma
        // opción de respuesta y traerlos en la consulta anterior los repetía en la colección.
        List<Long> opcionIds = preguntas.stream().map(p -> p.getOpcionRespuesta().getId()).distinct().toList();
        if (!opcionIds.isEmpty()) {
            entityManager.createQuery(
                    "SELECT DISTINCT o FROM OpcionRespuesta o LEFT JOIN FETCH o.items WHERE o.id IN :ids",
                    OpcionRespuesta.class)
                    .setParameter("ids", opcionIds)
                    .getResultList();
        }
        return preguntas;
    }

    @Override
    public List<PreguntaItemOpcion> buscarPonderaciones(Collection<Long> preguntaIds) {
        if (preguntaIds == null || preguntaIds.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery(
                "SELECT pio FROM PreguntaItemOpcion pio WHERE pio.id.preguntaId IN :preguntaIds",
                PreguntaItemOpcion.class)
                .setParameter("preguntaIds", preguntaIds)
                .getResultList();
    }
}
