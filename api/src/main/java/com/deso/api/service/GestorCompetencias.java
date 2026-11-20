package com.deso.api.service;

import com.deso.api.dao.CompetenciaDAO;
import com.deso.api.dto.CompetenciaDTO;
import com.deso.api.dto.TotalesCompetenciasDTO;
import com.deso.api.exception.RecursoDuplicadoException;
import com.deso.api.exception.RecursoNoEncontradoException;
import com.deso.api.model.entity.Competencia;
import com.deso.api.model.enums.TipoCompetencia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Gestor del caso de uso CU 13: Dar de alta Competencia (+ Listado de Competencias).
 */
@Validated
@Service
public class GestorCompetencias {

    private final CompetenciaDAO competenciaDAO;

    public GestorCompetencias(CompetenciaDAO competenciaDAO) {
        this.competenciaDAO = competenciaDAO;
    }

    /**
     * Da de alta una nueva competencia verificando unicidad de código y nombre.
     * Las restricciones de campos obligatorios y longitudes son validadas mediante Bean Validation (@Valid).
     *
     * @param datos DTO con los datos de la competencia a registrar.
     * @return DTO de la competencia persistida con su ID generado.
     * @throws RecursoDuplicadoException Si ya existe una competencia con el mismo código o nombre.
     */
    @Transactional
    public CompetenciaDTO darDeAlta(@NotNull @Valid CompetenciaDTO datos) {
        if (datos == null) {
            throw new IllegalArgumentException("Los datos de la competencia no pueden ser nulos.");
        }

        // Reglas de negocio backend: normalización y mayúsculas en código
        String codigo = datos.getCodigo() != null ? datos.getCodigo().strip().toUpperCase(Locale.ROOT) : null;
        String nombre = datos.getNombre() != null ? datos.getNombre().strip() : null;
        String descripcion = normalizarTexto(datos.getDescripcion());

        // Validación de unicidad de código
        if (codigo != null && competenciaDAO.existePorCodigo(codigo)) {
            throw new RecursoDuplicadoException("codigo", "Ya existe una competencia registrada con el código '" + codigo + "'.");
        }

        // Validación de unicidad de nombre
        if (nombre != null && competenciaDAO.existePorNombre(nombre)) {
            throw new RecursoDuplicadoException("nombre", "Ya existe una competencia registrada con el nombre '" + nombre + "'.");
        }

        Competencia entidad = new Competencia();
        entidad.setCodigo(codigo);
        entidad.setNombre(nombre);
        entidad.setTipo(datos.getTipo());
        entidad.setDescripcion(descripcion);
        entidad.setEliminada(false);

        Competencia guardada = competenciaDAO.guardar(entidad);
        return mapearADTO(guardada);
    }

    /**
     * Lista competencias activas con filtros opcionales de nombre, código y tipo.
     */
    @Transactional(readOnly = true)
    public List<CompetenciaDTO> listar(String nombre, String codigo, TipoCompetencia tipo) {
        return competenciaDAO.listarActivas(nombre, codigo, tipo)
                .stream()
                .map(this::mapearADTO)
                .toList();
    }

    /**
     * Totales del catálogo de competencias activas, independientes de los filtros del listado.
     */
    @Transactional(readOnly = true)
    public TotalesCompetenciasDTO obtenerTotales() {
        Map<TipoCompetencia, Long> porTipo = competenciaDAO.contarActivasPorTipo();
        long tecnicas = porTipo.getOrDefault(TipoCompetencia.TECNICA, 0L);
        long actitudinales = porTipo.getOrDefault(TipoCompetencia.ACTITUDINAL, 0L);
        return new TotalesCompetenciasDTO(tecnicas + actitudinales, tecnicas, actitudinales);
    }

    /**
     * Obtiene una competencia por su identificador primario.
     */
    @Transactional(readOnly = true)
    public CompetenciaDTO buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la competencia no puede ser nulo.");
        }
        return competenciaDAO.buscarPorId(id)
                .map(this::mapearADTO)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la competencia con ID " + id + "."));
    }

    private CompetenciaDTO mapearADTO(Competencia entidad) {
        return CompetenciaDTO.builder()
                .id(entidad.getId())
                .codigo(entidad.getCodigo())
                .nombre(entidad.getNombre())
                .tipo(entidad.getTipo())
                .descripcion(entidad.getDescripcion())
                .eliminada(Objects.requireNonNullElse(entidad.getEliminada(), false))
                .build();
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.strip();
        return recortado.isEmpty() ? null : recortado;
    }
}
