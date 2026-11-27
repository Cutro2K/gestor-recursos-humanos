package com.deso.api.dao;

import com.deso.api.model.entity.OpcionRespuesta;

import java.util.List;
import java.util.Optional;

public interface OpcionRespuestaDAO {

    Optional<OpcionRespuesta> buscarPorId(Long id);

    boolean existeActivaPorNombre(String nombre);

    List<OpcionRespuesta> buscarActivasPorNombre(String nombre, Integer offset, Integer cantidad);

    OpcionRespuesta guardar(OpcionRespuesta opcionRespuesta);

    /** Sincroniza de inmediato los cambios pendientes con la base de datos. */
    void sincronizar();
}
