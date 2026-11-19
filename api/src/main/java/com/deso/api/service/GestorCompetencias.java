package com.deso.api.service;

import com.deso.api.dao.CompetenciaDAO;
import com.deso.api.dto.CompetenciaDTO;
import com.deso.api.exception.RecursoDuplicadoException;
import com.deso.api.exception.RecursoNoEncontradoException;
import com.deso.api.model.entity.Competencia;
import com.deso.api.model.enums.TipoCompetencia;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Gestor del caso de uso CU 13: Dar de alta Competencia (+ Listado de Competencias).
 */
@Service
public class GestorCompetencias {

    private final CompetenciaDAO competenciaDAO;

    public GestorCompetencias(CompetenciaDAO competenciaDAO) {
        this.competenciaDAO = competenciaDAO;
    }

    /**
     * Da de alta una nueva competencia verificando unicidad de código y nombre.
     *
     * @param datos DTO con los datos de la competencia a registrar.
     * @return DTO de la competencia persistida con su ID generado.
     * @throws RecursoDuplicadoException Si ya existe una competencia con el mismo código o nombre.
     * @throws IllegalArgumentException  Si alguno de los campos obligatorios es inválido.
     */
    @Transactional
    public CompetenciaDTO darDeAlta(CompetenciaDTO datos) {
        if (datos == null) {
            throw new IllegalArgumentException("Los datos de la competencia no pueden ser nulos.");
        }

        String codigo = normalizarTexto(datos.getCodigo());
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código de la competencia es obligatorio.");
        }
        if (codigo.length() > 20) {
            throw new IllegalArgumentException("El código no puede superar los 20 caracteres.");
        }

        String nombre = normalizarTexto(datos.getNombre());
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la competencia es obligatorio.");
        }
        if (nombre.length() > 75) {
            throw new IllegalArgumentException("El nombre no puede superar los 75 caracteres.");
        }

        if (datos.getTipo() == null) {
            throw new IllegalArgumentException("El tipo de competencia es obligatorio (TECNICA o ACTITUDINAL).");
        }

        String descripcion = normalizarTexto(datos.getDescripcion());
        if (descripcion != null && descripcion.length() > 1000) {
            throw new IllegalArgumentException("La descripción no puede superar los 1000 caracteres.");
        }

        // Validación de unicidad de código
        if (competenciaDAO.existePorCodigo(codigo)) {
            throw new RecursoDuplicadoException("Ya existe una competencia registrada con el código '" + codigo + "'.");
        }

        // Validación de unicidad de nombre
        if (competenciaDAO.existePorNombre(nombre)) {
            throw new RecursoDuplicadoException("Ya existe una competencia registrada con el nombre '" + nombre + "'.");
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
     * Búsqueda por nombre conforme a la firma del diagrama de clases.
     */
    @Transactional(readOnly = true)
    public List<CompetenciaDTO> buscar(String nombre) {
        return listar(nombre, null, null);
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

    /**
     * Modifica los datos de una competencia existente.
     */
    @Transactional
    public CompetenciaDTO modificar(CompetenciaDTO datos) {
        if (datos == null || datos.getId() == null) {
            throw new IllegalArgumentException("Debe indicar el ID de la competencia a modificar.");
        }

        Competencia existente = competenciaDAO.buscarPorId(datos.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la competencia con ID " + datos.getId() + "."));

        String codigo = normalizarTexto(datos.getCodigo());
        if (codigo != null && !codigo.isBlank()) {
            if (!codigo.equalsIgnoreCase(existente.getCodigo()) && competenciaDAO.existePorCodigo(codigo)) {
                throw new RecursoDuplicadoException("Ya existe una competencia registrada con el código '" + codigo + "'.");
            }
            existente.setCodigo(codigo);
        }

        String nombre = normalizarTexto(datos.getNombre());
        if (nombre != null && !nombre.isBlank()) {
            if (!nombre.equalsIgnoreCase(existente.getNombre()) && competenciaDAO.existePorNombre(nombre)) {
                throw new RecursoDuplicadoException("Ya existe una competencia registrada con el nombre '" + nombre + "'.");
            }
            existente.setNombre(nombre);
        }

        if (datos.getTipo() != null) {
            existente.setTipo(datos.getTipo());
        }

        existente.setDescripcion(normalizarTexto(datos.getDescripcion()));

        Competencia actualizada = competenciaDAO.guardar(existente);
        return mapearADTO(actualizada);
    }

    /**
     * Realiza la baja lógica de la competencia.
     */
    @Transactional
    public void darDeBaja(Long id, String usuario) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la competencia no puede ser nulo.");
        }
        if (competenciaDAO.buscarPorId(id).isEmpty()) {
            throw new RecursoNoEncontradoException("No se encontró la competencia con ID " + id + " para dar de baja.");
        }
        competenciaDAO.eliminarLogicamente(id);
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
