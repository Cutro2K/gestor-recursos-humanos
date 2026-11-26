package com.deso.api.dao.impl;

import com.deso.api.dao.AuditoriaDAO;
import com.deso.api.model.entity.RegistroAuditoria;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class AuditoriaPostgreSQLDAO implements AuditoriaDAO {

    private final EntityManager entityManager;

    public AuditoriaPostgreSQLDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public RegistroAuditoria guardar(RegistroAuditoria registro) {
        if (registro.getId() == null) {
            entityManager.persist(registro);
            return registro;
        } else {
            return entityManager.merge(registro);
        }
    }
}
