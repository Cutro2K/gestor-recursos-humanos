package com.deso.api.dao.impl;

import com.deso.api.dao.PreguntaDAO;
import com.deso.api.model.entity.Pregunta;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class PreguntaPostgreSQLDAO implements PreguntaDAO {

    private final EntityManager entityManager;

    public PreguntaPostgreSQLDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Pregunta> buscarPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(Pregunta.class, id));
    }

    @Override
    public boolean existePorFactorYNombre(Long factorId, String nombre) {
        Long count = entityManager.createQuery("""
                SELECT COUNT(p) FROM Pregunta p
                WHERE p.factor.id = :factorId
                  AND lower(p.nombre) = lower(:nombre)
                """, Long.class)
                .setParameter("factorId", factorId)
                .setParameter("nombre", nombre.strip())
                .getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existePorFactorYEnunciado(Long factorId, String enunciado) {
        Long count = entityManager.createQuery("""
                SELECT COUNT(p) FROM Pregunta p
                WHERE p.factor.id = :factorId
                  AND lower(p.enunciado) = lower(:enunciado)
                """, Long.class)
                .setParameter("factorId", factorId)
                .setParameter("enunciado", enunciado.strip())
                .getSingleResult();
        return count > 0;
    }

    @Override
    public List<Pregunta> buscarConFiltros(
            Long competenciaId,
            Long factorId,
            String nombre,
            String enunciado,
            Boolean activa,
            Integer offset,
            Integer cantidad
    ) {
        int primerResultado = (offset != null && offset >= 0) ? offset : 0;
        int maxResultados = (cantidad != null && cantidad > 0) ? cantidad : 50;

        return entityManager.createQuery("""
                SELECT p FROM Pregunta p
                JOIN FETCH p.factor f
                JOIN FETCH f.competencia c
                JOIN FETCH p.opcionRespuesta o
                WHERE p.eliminada = false
                  AND (:competenciaId IS NULL OR c.id = :competenciaId)
                  AND (:factorId IS NULL OR f.id = :factorId)
                  AND (:nombre IS NULL OR lower(p.nombre) LIKE :nombre ESCAPE '!')
                  AND (:enunciado IS NULL OR lower(p.enunciado) LIKE :enunciado ESCAPE '!')
                  AND (:activa IS NULL OR p.activa = :activa)
                ORDER BY p.nombre ASC, p.id ASC
                """, Pregunta.class)
                .setParameter("competenciaId", competenciaId)
                .setParameter("factorId", factorId)
                .setParameter("nombre", patronDeBusqueda(nombre))
                .setParameter("enunciado", patronDeBusqueda(enunciado))
                .setParameter("activa", activa)
                .setFirstResult(primerResultado)
                .setMaxResults(maxResultados)
                .getResultList();
    }

    @Override
    @Transactional
    public Pregunta guardar(Pregunta pregunta) {
        if (pregunta.getId() == null) {
            entityManager.persist(pregunta);
            entityManager.flush();
            return pregunta;
        }
        Pregunta mergeada = entityManager.merge(pregunta);
        entityManager.flush();
        return mergeada;
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