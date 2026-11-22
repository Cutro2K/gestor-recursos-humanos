package com.deso.api.dao;

import com.deso.api.model.entity.Competencia;
import com.deso.api.model.enums.TipoCompetencia;

import java.util.List;
import java.util.Map;
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
     * Busca una competencia activa por su clave primaria.
     */
    Optional<Competencia> buscarPorId(Long id);

    /**
     * Determina si ya existe una competencia con el código especificado.
     */
    boolean existePorCodigo(String codigo);

    /**
     * Determina si ya existe una competencia con el nombre especificado.
     */
    boolean existePorNombre(String nombre);

    /**
     * Cuenta las competencias activas agrupadas por tipo. Los tipos sin competencias no aparecen.
     */
    Map<TipoCompetencia, Long> contarActivasPorTipo();

    /**
     * Inserta una nueva competencia o actualiza una existente.
     */
    Competencia guardar(Competencia competencia);
}
