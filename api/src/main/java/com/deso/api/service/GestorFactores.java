package com.deso.api.service;

import com.deso.api.dao.CompetenciaDAO;
import com.deso.api.dao.FactorDAO;
import com.deso.api.dto.FactorDTO;
import com.deso.api.dto.ListadoGestorFactoresDTO;
import com.deso.api.exception.RecursoActualmenteEliminadoException;
import com.deso.api.exception.RecursoDuplicadoException;
import com.deso.api.exception.RecursoNoEncontradoException;
import com.deso.api.model.entity.Competencia;
import com.deso.api.model.entity.Factor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public FactorDTO darDeAlta(
        FactorDTO datos
    ) {
        // 1. Validar existencia y estado de la competencia padre
        Competencia competencia = competenciaDAO.buscarPorId(datos.getCompetenciaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La competencia padre especificada no existe."));

        if (Boolean.TRUE.equals(competencia.getEliminada())) {
            throw new RecursoActualmenteEliminadoException("No se pueden agregar factores a una competencia eliminada.");
        }

        // 2. Validar unicidad de código y nombre (Globales según DER)
        if (factorDAO.existePorCodigo(datos.getCodigo())) {
            throw new RecursoDuplicadoException("El código del factor ya se encuentra registrado.");
        }

        if (factorDAO.existePorNombre(datos.getNombre())) {
            throw new RecursoDuplicadoException("El nombre del factor ya se encuentra registrado.");
        }

        // 3. Validar unicidad del orden dentro de la misma competencia (UQ en DER)
        if (factorDAO.existePorCompetenciaYOrden(competencia.getId(), datos.getOrden())) {
            throw new RecursoDuplicadoException("El orden " + datos.getOrden() + " ya está en uso para esta competencia.");
        }

        // 4. Mapear y persistir la entidad
        Factor nuevoFactor = new Factor();
        nuevoFactor.setCodigo(datos.getCodigo().strip());
        nuevoFactor.setNombre(datos.getNombre().strip());
        nuevoFactor.setOrden(datos.getOrden());
        nuevoFactor.setDescripcion(datos.getDescripcion() != null ? datos.getDescripcion().strip() : null);
        nuevoFactor.setCompetencia(competencia);
        nuevoFactor.setEliminado(false);

        Factor guardado = factorDAO.guardar(nuevoFactor);
        return mapearAFactorDTO(guardado);
    }

    @Transactional 
    public void eliminarLogicamente(
        Long id
    ) {
        Factor factor = factorDAO.buscarPorId(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("El factor a eliminar no existe."));
        if (Boolean.TRUE.equals(factor.getEliminado())) {
            throw new RecursoActualmenteEliminadoException("El factor ya se encuentra eliminado.");
        }
        factorDAO.eliminarLogicamente(id);
    }

    @Transactional 
    public FactorDTO actualizar(FactorDTO newFactor) {
        Factor factor = factorDAO.buscarPorId(newFactor.getId())
            .orElseThrow(() -> new RecursoNoEncontradoException("El factor a actualizar no existe."));
        
        if (Boolean.TRUE.equals(factor.getEliminado())) {
            throw new RecursoActualmenteEliminadoException("El factor a actualizar ha sido eliminado.");
        }

        Competencia competenciaRelacionada = competenciaDAO.buscarPorId(newFactor.getCompetenciaId())
            .orElseThrow(() -> new RecursoNoEncontradoException("La competencia ingresada no existe."));

        if (Boolean.TRUE.equals(competenciaRelacionada.getEliminada())) {
            throw new RecursoActualmenteEliminadoException("La competencia ingresada ha sido eliminada.");
        }

        // Chequeo de unicidad de Codigo no hace falta chequear ya que el atributo no es modificable.

        // Chequeo de unicidad por nombre: 
        if (Boolean.TRUE.equals(
            (!factor.getNombre().equalsIgnoreCase(newFactor.getNombre().strip()))
            && factorDAO.existePorNombre(newFactor.getNombre()) 
        )) {
            throw new RecursoDuplicadoException("Ya existe un factor con ese nombre.");
        }

        // Chequeo de unicidad por competencia y orden: 
        if (Boolean.TRUE.equals(
            factorDAO.existePorCompetenciaYOrden(newFactor.getCompetenciaId(), newFactor.getOrden())
            && (!factor.getCompetencia().getId().equals(newFactor.getCompetenciaId()) || !factor.getOrden().equals(newFactor.getOrden()))
        )) {    
            throw new RecursoDuplicadoException("Ya existe un factor con ese orden relacionado a esa competencia.");
        }

        // Actualizar valores de la entidad
        factor.setCompetencia(competenciaRelacionada);
        factor.setDescripcion(newFactor.getDescripcion() != null ? newFactor.getDescripcion().strip() : null);
        factor.setNombre(newFactor.getNombre().strip());
        factor.setOrden(newFactor.getOrden());

        FactorDTO guardado = mapearAFactorDTO(factorDAO.guardar(factor));
        return guardado;
    }

    public List<ListadoGestorFactoresDTO> buscarPorNombres(
            String nombreCompetencia, 
            String nombreFactor, 
            Integer offset, 
            Integer cantidad
    ) {
        return factorDAO.buscarPorNombres(nombreCompetencia, nombreFactor, offset, cantidad)
                .stream()
                .map(this::mapearAListadoGestorDTO)
                .toList();
    }

    public List<FactorDTO> listarPorCompetencia(
        Long competenciaId
    ) {
        Competencia competencia = competenciaDAO.buscarPorId(competenciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encuentran factores para esta competencia."));

        if (Boolean.TRUE.equals(competencia.getEliminada())) {
            throw new RecursoNoEncontradoException("No se encuentran factores para esta competencia.");
        }

        return factorDAO.listarActivosPorCompetencia(competenciaId)
                .stream()
                .map(this::mapearAFactorDTO)
                .toList();
    }

    public FactorDTO buscarPorId(
        Long id
    ) {
        Factor factor = factorDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el factor con ID " + id + "."));

        if (Boolean.TRUE.equals(factor.getEliminado())) {
            throw new RecursoActualmenteEliminadoException("El factor con ID " + id + " se encuentra eliminado.");
        }

        return mapearAFactorDTO(factor);
    }

    private FactorDTO mapearAFactorDTO(
        Factor factor
    ) {
        return new FactorDTO(
                factor.getId(),
                factor.getCompetencia().getId(),
                factor.getCodigo(),
                factor.getNombre(),
                factor.getOrden(),
                factor.getDescripcion()
        );
    }

    private ListadoGestorFactoresDTO mapearAListadoGestorDTO(
        Factor factor
    ) {
        return new ListadoGestorFactoresDTO(
                factor.getId(),
                factor.getCodigo(),
                factor.getNombre(),
                factor.getCompetencia().getNombre(),
                factor.getOrden(),
                factor.getDescripcion()
        );
    }
}
