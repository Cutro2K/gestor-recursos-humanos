package com.deso.api.dao;

import com.deso.api.model.entity.Pregunta;

import java.util.List;
import java.util.Optional;

public interface PreguntaDAO {

    Optional<Pregunta> buscarPorId(Long id);

    boolean existePorFactorYNombre(Long factorId, String nombre);

    boolean existePorFactorYEnunciado(Long factorId, String enunciado);

    List<Pregunta> buscarConFiltros(
            Long competenciaId,
            Long factorId,
            String nombre,
            String enunciado,
            Boolean activa,
            Integer offset,
            Integer cantidad
    );

    Pregunta guardar(Pregunta pregunta);
}