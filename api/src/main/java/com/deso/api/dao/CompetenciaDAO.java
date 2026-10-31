package com.deso.api.dao;

import com.deso.api.model.entity.Competencia;
import com.deso.api.model.enums.TipoCompetencia;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la entidad Competencia (CU 13).
 */
public interface CompetenciaDAO {

    /**
     * Lista competencias activas con filtros opcionales de nombre, código y tipo.
     */
    List<Competencia> listarActivas(String nombre, String codigo, TipoCompetencia tipo);

    /**
     * Búsqueda por nombre (compatibilidad con especificación del diagrama de clases).
     */
    List<Competencia> buscar(String nombre);

    /**
     * Busca una competencia por su clave primaria.
     */
    Optional<Competencia> buscarPorId(Long id);

    /**
     * Busca una competencia por su código único (sin distinguir mayúsculas).
     */
    Optional<Competencia> buscarPorCodigo(String codigo);

    /**
     * Busca una competencia por su nombre único (sin distinguir mayúsculas).
     */
    Optional<Competencia> buscarPorNombre(String nombre);

    /**
     * Determina si ya existe una competencia con el código especificado.
     */
    boolean existePorCodigo(String codigo);

    /**
     * Determina si ya existe una competencia con el nombre especificado.
     */
    boolean existePorNombre(String nombre);

    /**
     * Inserta una nueva competencia o actualiza una existente.
     */
    Competencia guardar(Competencia competencia);

    /**
     * Realiza la baja lógica marcando el registro como eliminado.
     */
    void eliminarLogicamente(Long id);
}
