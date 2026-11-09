package com.deso.api.service;

import com.deso.api.dao.CompetenciaDAO;
import com.deso.api.dao.FactorDAO;
import com.deso.api.dto.FactorDTO;
import com.deso.api.model.entity.Competencia;
import com.deso.api.model.entity.Factor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GestorFactores {

    private final FactorDAO factorDAO;
    private final CompetenciaDAO competenciaDAO;

    public GestorFactores(FactorDAO factorDAO, CompetenciaDAO competenciaDAO) {
        this.factorDAO = factorDAO;
        this.competenciaDAO = competenciaDAO;
    }

    @Transactional
    public void darDeAlta(FactorDTO datos) {
        // 1. Validar existencia de la competencia padre
        Competencia competencia = competenciaDAO.buscarPorId(datos.getCompetenciaId())
                .orElseThrow(() -> new IllegalArgumentException("La competencia padre especificada no existe."));

        if (Boolean.TRUE.equals(competencia.getEliminada())) {
            throw new IllegalArgumentException("No se pueden agregar factores a una competencia eliminada.");
        }

        // 2. Validar unicidad de código y nombre (Globales según DER)
        if (factorDAO.existePorCodigo(datos.getCodigo())) {
            throw new IllegalArgumentException("El código del factor ya se encuentra registrado.");
        }
        
        if (factorDAO.existePorNombre(datos.getNombre())) {
            throw new IllegalArgumentException("El nombre del factor ya se encuentra registrado.");
        }

        // 3. Validar unicidad del orden dentro de la misma competencia (UQ en DER)
        if (factorDAO.existePorCompetenciaYOrden(competencia.getId(), datos.getOrden())) {
            throw new IllegalArgumentException("El orden " + datos.getOrden() + " ya está en uso para esta competencia.");
        }

        // 4. Mapear y persistir la entidad
        Factor nuevoFactor = new Factor();
        nuevoFactor.setCodigo(datos.getCodigo().strip());
        nuevoFactor.setNombre(datos.getNombre().strip());
        nuevoFactor.setOrden(datos.getOrden());
        nuevoFactor.setDescripcion(datos.getDescripcion() != null ? datos.getDescripcion().strip() : null);
        nuevoFactor.setCompetencia(competencia);
        nuevoFactor.setEliminado(false); 

        factorDAO.guardar(nuevoFactor);
    }
}