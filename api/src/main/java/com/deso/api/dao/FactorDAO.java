package com.deso.api.dao;

import com.deso.api.model.entity.Factor;
import java.util.List;
import java.util.Optional;

public interface FactorDAO {
    
    Optional<Factor> buscarPorId(Long id);
    
    boolean existePorCodigo(String codigo);
    
    boolean existePorNombre(String nombre);
    
    boolean existePorCompetenciaYOrden(Long competenciaId, Integer orden);
    
    List<Factor> listarActivosPorCompetencia(Long competenciaId);

    List<Factor> buscarPorNombres(String nombreCompetencia, String nombreFactor, Integer offset, Integer cantidad);

    Factor guardar(Factor factor);
}