package com.deso.api.dao.impl;

import com.deso.api.dao.SesionDAO;
import com.deso.api.model.entity.Sesion;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class SesionPostgreSQLDAO implements SesionDAO {

    private final EntityManager entityManager;

    public SesionPostgreSQLDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Sesion crear(Sesion sesion) {
        entityManager.persist(sesion);
        entityManager.flush();
        return sesion;
    }

    @Override
    public Optional<Sesion> buscarPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        return entityManager.createQuery("""
                SELECT s FROM Sesion s
                LEFT JOIN FETCH s.consultor
                LEFT JOIN FETCH s.cuestionario cu
                LEFT JOIN FETCH cu.candidato
                WHERE s.id = :id
                """, Sesion.class)
                .setParameter("id", id)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    @Transactional
    public boolean rotar(Long id, long seqEsperado) {
        int filas = entityManager.createQuery("""
                UPDATE Sesion s
                SET s.seq = s.seq + 1
                WHERE s.id = :id AND s.seq = :seq
                """)
                .setParameter("id", id)
                .setParameter("seq", seqEsperado)
                .executeUpdate();
        return filas == 1;
    }

    @Override
    @Transactional
    public void borrar(Long id) {
        if (id == null) {
            return;
        }
        entityManager.createQuery("DELETE FROM Sesion s WHERE s.id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    @Transactional
    public void borrarPorCuestionario(Long cuestionarioId) {
        if (cuestionarioId == null) {
            return;
        }
        entityManager.createQuery("DELETE FROM Sesion s WHERE s.cuestionario.id = :cuestionarioId")
                .setParameter("cuestionarioId", cuestionarioId)
                .executeUpdate();
    }
}
