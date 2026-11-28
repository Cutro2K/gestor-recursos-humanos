package com.deso.api.dao;

import com.deso.api.model.entity.OpcionRespuesta;

import java.util.List;
import java.util.Optional;

public interface OpcionRespuestaDAO {

    Optional<OpcionRespuesta> buscarPorId(Long id);

    boolean existeActivaPorNombre(String nombre);

    List<OpcionRespuesta> buscarActivasPorNombre(String nombre, Integer offset, Integer cantidad);

    boolean estaEnUso(Long id);

    OpcionRespuesta guardar(OpcionRespuesta opcionRespuesta);

    void eliminarLogicamente(Long id);

    /** Sincroniza de inmediato los cambios pendientes con la base de datos. */
    void sincronizar();
}
