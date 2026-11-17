package com.deso.api.dao.impl;

import com.deso.api.dao.FactorDAO;
import com.deso.api.model.entity.Factor;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class FactorPostgreSQLDAO implements FactorDAO {

    private final EntityManager entityManager;

    public FactorPostgreSQLDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Factor> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(entityManager.find(Factor.class, id));
    }

    @Override
    public boolean existePorCodigo(String codigo) {
        Long count = entityManager.createQuery("""
                SELECT COUNT(f) FROM Factor f 
                WHERE lower(f.codigo) = lower(:codigo)
                """, Long.class)
                .setParameter("codigo", codigo.strip())
                .getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existePorNombre(String nombre) {
        Long count = entityManager.createQuery("""
                SELECT COUNT(f) FROM Factor f 
                WHERE lower(f.nombre) = lower(:nombre)
                """, Long.class)
                .setParameter("nombre", nombre.strip())
                .getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existePorCompetenciaYOrden(Long competenciaId, Integer orden) {
        Long count = entityManager.createQuery("""
                SELECT COUNT(f) FROM Factor f 
                WHERE f.competencia.id = :competenciaId AND f.orden = :orden
                """, Long.class)
                .setParameter("competenciaId", competenciaId)
                .setParameter("orden", orden)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public List<Factor> listarActivosPorCompetencia(Long competenciaId) {
        return entityManager.createQuery("""
                SELECT f FROM Factor f 
                WHERE f.competencia.id = :competenciaId AND f.eliminado = false
                ORDER BY f.orden ASC
                """, Factor.class)
                .setParameter("competenciaId", competenciaId)
                .getResultList();
    }

    @Override
    @Transactional
    public Factor guardar(Factor factor) {
        if (factor.getId() == null) {
            entityManager.persist(factor);
            return factor;
        } else {
            return entityManager.merge(factor);
        }
    }

    @Override
    public List<Factor> buscar(String nombre) {
        // Preparamos el parámetro para la búsqueda parcial (ej: "%tiempo%")
        String parametroNombre = (nombre == null || nombre.isBlank()) 
                ? null 
                : "%" + nombre.strip().toLowerCase(java.util.Locale.ROOT) + "%";

        return entityManager.createQuery("""
                SELECT f FROM Factor f
                JOIN FETCH f.competencia
                WHERE f.eliminado = false
                AND (:nombre IS NULL OR lower(f.nombre) LIKE :nombre)
                ORDER BY f.competencia.id ASC, f.orden ASC
                """, Factor.class)
                .setParameter("nombre", parametroNombre)
                .getResultList();
    }
}