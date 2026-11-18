package com.deso.api.dao.impl;

import com.deso.api.dao.FactorDAO;
import com.deso.api.model.entity.Factor;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class FactorPostgreSQLDAO implements FactorDAO {

    private final EntityManager entityManager;

    public FactorPostgreSQLDAO(
            EntityManager entityManager
        ) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Factor> buscarPorId(Long id) {
        // IMPORTANTE: Este método busca por clave primaria sin filtrar por baja lógica.
        // Quien lo invoque debe verificar el atributo factor.getEliminado() según corresponda.
        if (id == null) return Optional.empty();
        return Optional.ofNullable(entityManager.find(Factor.class, id));
    }

    @Override
    public boolean existePorCodigo(
            String codigo
        ) {
        Long count = entityManager.createQuery("""
                SELECT COUNT(f) FROM Factor f 
                WHERE lower(f.codigo) = lower(:codigo)
                """, Long.class)
                .setParameter("codigo", codigo.strip())
                .getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existePorNombre(
            String nombre
        ) {
        Long count = entityManager.createQuery("""
                SELECT COUNT(f) FROM Factor f 
                WHERE lower(f.nombre) = lower(:nombre)
                """, Long.class)
                .setParameter("nombre", nombre.strip())
                .getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existePorCompetenciaYOrden(
            Long competenciaId, 
            Integer orden
        ) {
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
    public List<Factor> listarActivosPorCompetencia(
            Long competenciaId
        ) {
        return entityManager.createQuery("""
                SELECT f FROM Factor f
                JOIN FETCH f.competencia
                WHERE f.competencia.id = :competenciaId AND f.eliminado = false
                ORDER BY f.orden ASC
                """, Factor.class)
                .setParameter("competenciaId", competenciaId)
                .getResultList();
    }

    @Override
    @Transactional
    public Factor guardar(
            Factor factor
        ) {
        if (factor.getId() == null) {
            entityManager.persist(factor);
            return factor;
        } else {
            return entityManager.merge(factor);
        }
    }

    @Override
    public List<Factor> buscarPorNombres(
            String nombreCompetencia, 
            String nombreFactor, 
            Integer offset, 
            Integer cantidad
        ) {
        String patronCompetencia = patronDeBusqueda(nombreCompetencia);
        String patronFactor = patronDeBusqueda(nombreFactor);

        int primerResultado = (offset != null && offset >= 0) ? offset : 0;
        int maxResultados = (cantidad != null && cantidad > 0) ? cantidad : 50;

        return entityManager.createQuery("""
                SELECT f FROM Factor f
                JOIN FETCH f.competencia c
                WHERE f.eliminado = false
                AND (:nombreCompetencia IS NULL OR lower(c.nombre) LIKE :nombreCompetencia ESCAPE '!')
                AND (:nombreFactor IS NULL OR lower(f.nombre) LIKE :nombreFactor ESCAPE '!')
                ORDER BY f.nombre ASC, f.id ASC
                """, Factor.class)
                .setParameter("nombreCompetencia", patronCompetencia)
                .setParameter("nombreFactor", patronFactor)
                .setFirstResult(primerResultado)
                .setMaxResults(maxResultados)
                .getResultList();
    }

    private String patronDeBusqueda(
            String valor
        ) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String texto = valor.strip().toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + texto + "%";
    }
}