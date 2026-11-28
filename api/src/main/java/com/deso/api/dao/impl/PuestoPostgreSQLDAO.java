package com.deso.api.dao.impl;

import com.deso.api.dao.PuestoDAO;
import com.deso.api.model.entity.Competencia;
import com.deso.api.model.entity.Puesto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public List<CompetenciaPonderada> listarCompetenciasRequeridas(Long puestoId) {
        // puesto_competencia todavía no tiene entidad: se lee con SQL y las competencias con JPQL.
        @SuppressWarnings("unchecked")
        List<Object[]> filas = entityManager.createNativeQuery(
                "SELECT competencia_id, ponderacion_minima FROM puesto_competencia WHERE puesto_id = :puestoId"
        ).setParameter("puestoId", puestoId)
         .getResultList();
        if (filas.isEmpty()) {
            return List.of();
        }

        Map<Long, BigDecimal> ponderaciones = filas.stream().collect(Collectors.toMap(
                fila -> ((Number) fila[0]).longValue(),
                fila -> new BigDecimal(fila[1].toString())));
        Map<Long, Competencia> competencias = entityManager.createQuery(
                "SELECT c FROM Competencia c WHERE c.id IN :ids", Competencia.class)
                .setParameter("ids", ponderaciones.keySet())
                .getResultStream()
                .collect(Collectors.toMap(Competencia::getId, Function.identity()));

        return competencias.values().stream()
                .sorted(Comparator.comparing(Competencia::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(c -> new CompetenciaPonderada(c, ponderaciones.get(c.getId())))
                .toList();
    }
}
