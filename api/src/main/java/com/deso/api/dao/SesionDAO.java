package com.deso.api.dao;

import com.deso.api.model.entity.Sesion;

import java.util.Optional;

/**
 * Contrato de acceso a datos para las sesiones de login (CU 01).
 */
public interface SesionDAO {

    /**
     * Inserta una sesión nueva y devuelve la entidad con su id generado.
     */
    Sesion crear(Sesion sesion);

    /**
     * Busca una sesión por id, con su consultor o cuestionario (y candidato) ya cargados.
     */
    Optional<Sesion> buscarPorId(Long id);

    /**
     * Incrementa {@code seq} solo si todavía vale {@code seqEsperado}. Es una única operación
     * atómica: si dos refrescos llegan con el mismo refresh token, gana uno solo.
     *
     * @return true si rotó; false si otro refresco ya la había rotado o la sesión no existe.
     */
    boolean rotar(Long id, long seqEsperado);

    /**
     * Borra la sesión, si existe.
     */
    void borrar(Long id);

    /**
     * Borra todas las sesiones de un cuestionario: un login nuevo del candidato reemplaza la anterior.
     */
    void borrarPorCuestionario(Long cuestionarioId);
}
