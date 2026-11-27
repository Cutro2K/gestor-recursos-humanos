package com.deso.api.dao.impl;

import com.deso.api.dao.ConfiguracionCuestionarioDAO;
import com.deso.api.model.entity.ConfiguracionCuestionario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ConfiguracionCuestionarioPostgreSQLDAO implements ConfiguracionCuestionarioDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<ConfiguracionCuestionario> buscarVigente() {
        return entityManager.createQuery(
                "SELECT c FROM ConfiguracionCuestionario c WHERE c.activa = true ORDER BY c.vigenciaDesde DESC",
                ConfiguracionCuestionario.class
        ).setMaxResults(1)
         .getResultStream()
         .findFirst();
    }
}
