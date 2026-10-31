package com.deso.api.dao;

import com.deso.api.model.entity.Cuestionario;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la entidad Cuestionario.
 */
public interface CuestionarioDAO {

    /**
     * Busca un cuestionario por su clave única de ingreso.
     */
    Optional<Cuestionario> buscarPorClave(String claveIngreso);

    /**
     * Busca un cuestionario activo o en proceso para un candidato específico.
     * Utilizado en la autenticación del candidato (CU 01).
     */
    Optional<Cuestionario> buscarActivoOEnProcesoPorCandidato(Long candidatoId);

    /**
     * Busca un cuestionario por su identificador primario.
     */
    Optional<Cuestionario> buscarPorId(Long id);

    /**
     * Busca todos los cuestionarios asociados a una lista de IDs de evaluaciones.
     */
    List<Cuestionario> buscarPorEvaluaciones(List<Long> evaluacionIds);

    /**
     * Guarda o actualiza un cuestionario en la base de datos.
     */
    Cuestionario guardar(Cuestionario cuestionario);
}
