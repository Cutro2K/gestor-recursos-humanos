package com.deso.api.dao.impl;

import com.deso.api.dao.ConsultorDAO;
import com.deso.api.model.entity.Consultor;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class ConsultorPostgreSQLDAO implements ConsultorDAO {

    private final EntityManager entityManager;
    
    public ConsultorPostgreSQLDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Consultor> buscarPorUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        return entityManager.createQuery("""
                SELECT c FROM Consultor c
                WHERE c.username = :username
                  AND c.activo = true
                """, Consultor.class)
                .setParameter("username", username.strip())
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public Optional<Consultor> buscarPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(Consultor.class, id));
    }

    @Override
    public Optional<Consultor> buscarPorEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return entityManager.createQuery("""
                SELECT c FROM Consultor c
                WHERE lower(c.email) = lower(:email)
                  AND c.activo = true
                """, Consultor.class)
                .setParameter("email", email.strip())
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    @Transactional
    public Consultor guardar(Consultor consultor) {
        if (consultor.getId() == null) {
            entityManager.persist(consultor);
            return consultor;
        } else {
            return entityManager.merge(consultor);
        }
    }
}