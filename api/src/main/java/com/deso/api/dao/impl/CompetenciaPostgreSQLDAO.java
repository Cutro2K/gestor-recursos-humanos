package com.deso.api.dao.impl;

import com.deso.api.dao.CompetenciaDAO;
import com.deso.api.model.entity.Competencia;
import com.deso.api.model.enums.TipoCompetencia;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class CompetenciaPostgreSQLDAO implements CompetenciaDAO {

    private final EntityManager entityManager;

    public CompetenciaPostgreSQLDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Competencia> listarActivas(String nombre, String codigo, TipoCompetencia tipo) {
        return entityManager.createQuery("""
                SELECT c FROM Competencia c
                WHERE c.eliminada = false
                  AND (:nombre IS NULL OR lower(c.nombre) LIKE :nombre ESCAPE '!')
                  AND (:codigo IS NULL OR lower(c.codigo) LIKE :codigo ESCAPE '!')
                  AND (:tipo IS NULL OR c.tipo = :tipo)
                ORDER BY c.id ASC
                """, Competencia.class)
                .setParameter("nombre", patronDeBusqueda(nombre))
                .setParameter("codigo", patronDeBusqueda(codigo))
                .setParameter("tipo", tipo)
                .getResultList();
    }

    @Override
    public List<Competencia> buscar(String nombre) {
        return listarActivas(nombre, null, null);
    }

    @Override
    public Optional<Competencia> buscarPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(Competencia.class, id));
    }

    @Override
    public Optional<Competencia> buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }

        return entityManager.createQuery("""
                SELECT c FROM Competencia c
                WHERE lower(c.codigo) = lower(:codigo)
                """, Competencia.class)
                .setParameter("codigo", codigo.strip())
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public Optional<Competencia> buscarPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return Optional.empty();
        }

        return entityManager.createQuery("""
                SELECT c FROM Competencia c
                WHERE lower(c.nombre) = lower(:nombre)
                """, Competencia.class)
                .setParameter("nombre", nombre.strip())
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public boolean existePorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return false;
        }

        Long count = entityManager.createQuery("""
                SELECT count(c) FROM Competencia c
                WHERE lower(c.codigo) = lower(:codigo)
                """, Long.class)
                .setParameter("codigo", codigo.strip())
                .getSingleResult();

        return count != null && count > 0;
    }

    @Override
    public boolean existePorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return false;
        }

        Long count = entityManager.createQuery("""
                SELECT count(c) FROM Competencia c
                WHERE lower(c.nombre) = lower(:nombre)
                """, Long.class)
                .setParameter("nombre", nombre.strip())
                .getSingleResult();

        return count != null && count > 0;
    }

    @Override
    @Transactional
    public Competencia guardar(Competencia competencia) {
        if (competencia.getId() == null) {
            entityManager.persist(competencia);
            entityManager.flush();
            return competencia;
        } else {
            Competencia mergeada = entityManager.merge(competencia);
            entityManager.flush();
            return mergeada;
        }
    }

    @Override
    @Transactional
    public void eliminarLogicamente(Long id) {
        if (id == null) {
            return;
        }

        Competencia competencia = entityManager.find(Competencia.class, id);
        if (competencia != null) {
            competencia.setEliminada(true);
            entityManager.merge(competencia);
            entityManager.flush();
        }
    }

    private String patronDeBusqueda(String valor) {
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
