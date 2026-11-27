package com.deso.api.dao.impl;

import com.deso.api.dao.OpcionRespuestaDAO;
import com.deso.api.model.entity.OpcionRespuesta;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class OpcionRespuestaPostgreSQLDAO implements OpcionRespuestaDAO {

    private final EntityManager entityManager;

    public OpcionRespuestaPostgreSQLDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<OpcionRespuesta> buscarPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return entityManager.createQuery("""
                SELECT o FROM OpcionRespuesta o
                LEFT JOIN FETCH o.items
                WHERE o.id = :id
                """, OpcionRespuesta.class)
                .setParameter("id", id)
                .getResultList()
                .stream()
                .findFirst();
    }
}