package com.deso.api.dao.impl;

import com.deso.api.dao.OpcionRespuestaDAO;
import com.deso.api.model.entity.OpcionRespuesta;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
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
        // Busca por clave primaria sin filtrar por baja lógica: lo decide el gestor.
        if (id == null) {
            return Optional.empty();
        }
        List<OpcionRespuesta> resultados = entityManager.createQuery("""
                SELECT o FROM OpcionRespuesta o
                LEFT JOIN FETCH o.items i
                WHERE o.id = :id
                ORDER BY i.orden ASC
                """, OpcionRespuesta.class)
                .setParameter("id", id)
                .getResultList();
        return resultados.stream().findFirst();
    }

    @Override
    public List<OpcionRespuesta> buscarActivasPorNombre(String nombre, Integer offset, Integer cantidad) {
        String patronNombre = patronDeBusqueda(nombre);
        int primerResultado = (offset != null && offset >= 0) ? offset : 0;
        int maxResultados = (cantidad != null && cantidad > 0) ? cantidad : 50;

        return entityManager.createQuery("""
                SELECT o FROM OpcionRespuesta o
                WHERE o.eliminada = false
                AND (:nombre IS NULL OR lower(o.nombre) LIKE :nombre ESCAPE '!')
                ORDER BY o.nombre ASC, o.id ASC
                """, OpcionRespuesta.class)
                .setParameter("nombre", patronNombre)
                .setFirstResult(primerResultado)
                .setMaxResults(maxResultados)
                .getResultList();
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

    @Override
    @Transactional
    public OpcionRespuesta guardar(OpcionRespuesta opcionRespuesta) {
        if (opcionRespuesta.getId() == null) {
            entityManager.persist(opcionRespuesta);
            return opcionRespuesta;
        } else {
            return entityManager.merge(opcionRespuesta);
        }
    }
}
