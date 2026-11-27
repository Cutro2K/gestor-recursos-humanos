package com.deso.api.dao;

import com.deso.api.model.entity.OpcionRespuesta;

import java.util.Optional;

public interface OpcionRespuestaDAO {

    Optional<OpcionRespuesta> buscarPorId(Long id);
}