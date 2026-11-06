package com.deso.api.dao.impl;

import com.deso.api.dao.CuestionarioDAO;
import com.deso.api.model.entity.Cuestionario;
import com.deso.api.model.enums.EstadoCuestionario;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class CuestionarioPostgreSQLDAO implements CuestionarioDAO {

    private final EntityManager entityManager;

    public CuestionarioPostgreSQLDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Cuestionario> buscarPorClave(String claveIngreso) {
        if (claveIngreso == null || claveIngreso.isBlank()) {
            return Optional.empty();
        }

        return entityManager.createQuery("""
                SELECT c FROM Cuestionario c
                JOIN FETCH c.candidato
                WHERE c.claveIngreso = :claveIngreso
                """, Cuestionario.class)
                .setParameter("claveIngreso", claveIngreso.strip())
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public Optional<Cuestionario> buscarActivoOEnProcesoPorCandidato(Long candidatoId) {
        if (candidatoId == null) {
            return Optional.empty();
        }

        return entityManager.createQuery("""
                SELECT c FROM Cuestionario c
                WHERE c.candidato.id = :candidatoId
                  AND c.estado IN (:activo, :enProceso)
                """, Cuestionario.class)
                .setParameter("candidatoId", candidatoId)
                .setParameter("activo", EstadoCuestionario.ACTIVO)
                .setParameter("enProceso", EstadoCuestionario.EN_PROCESO)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public Optional<Cuestionario> buscarPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(Cuestionario.class, id));
    }

    @Override
    public List<Cuestionario> buscarPorEvaluaciones(List<Long> evaluacionIds) {
        if (evaluacionIds == null || evaluacionIds.isEmpty()) {
            return List.of();
        }

        return entityManager.createQuery("""
                SELECT c FROM Cuestionario c
                WHERE c.evaluacion.id IN :evaluacionIds
                """, Cuestionario.class)
                .setParameter("evaluacionIds", evaluacionIds)
                .getResultList();
    }

    @Override
    @Transactional
    public boolean registrarAcceso(Long cuestionarioId, LocalDateTime ingreso) {
        int filas = entityManager.createQuery("""
                UPDATE Cuestionario c
                SET c.cantidadAccesos = c.cantidadAccesos + 1, c.ultimoIngreso = :ingreso
                WHERE c.id = :id
                  AND c.cantidadAccesos < c.cantidadMaximaAccesos
                  AND c.estado IN (:activo, :enProceso)
                """)
                .setParameter("ingreso", ingreso)
                .setParameter("id", cuestionarioId)
                .setParameter("activo", EstadoCuestionario.ACTIVO)
                .setParameter("enProceso", EstadoCuestionario.EN_PROCESO)
                .executeUpdate();
        return filas == 1;
    }

    @Override
    @Transactional
    public void marcarIncompleto(Long cuestionarioId, LocalDateTime fechaCambio) {
        entityManager.createQuery("""
                UPDATE Cuestionario c
                SET c.estado = :incompleto, c.fechaCambioEstado = :fechaCambio
                WHERE c.id = :id
                  AND c.estado IN (:activo, :enProceso)
                """)
                .setParameter("incompleto", EstadoCuestionario.INCOMPLETO)
                .setParameter("fechaCambio", fechaCambio)
                .setParameter("id", cuestionarioId)
                .setParameter("activo", EstadoCuestionario.ACTIVO)
                .setParameter("enProceso", EstadoCuestionario.EN_PROCESO)
                .executeUpdate();
    }

    @Override
    @Transactional
    public Cuestionario guardar(Cuestionario cuestionario) {
        if (cuestionario.getId() == null) {
            entityManager.persist(cuestionario);
            return cuestionario;
        } else {
            return entityManager.merge(cuestionario);
        }
    }
}
