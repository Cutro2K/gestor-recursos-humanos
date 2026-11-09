package com.deso.api.controller;

import com.deso.api.dto.CompetenciaDTO;
import com.deso.api.dto.ErrorResponseDTO;
import com.deso.api.exception.RecursoDuplicadoException;
import com.deso.api.exception.RecursoNoEncontradoException;
import com.deso.api.model.enums.TipoCompetencia;
import com.deso.api.service.GestorCompetencias;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST para el catálogo de Competencias (CU 13).
 */
@RestController
@RequestMapping(value = "/api/competencias", produces = MediaType.APPLICATION_JSON_VALUE)
public class CompetenciaController {

    private final GestorCompetencias gestorCompetencias;

    public CompetenciaController(GestorCompetencias gestorCompetencias) {
        this.gestorCompetencias = gestorCompetencias;
    }

    /**
     * Consulta el listado de competencias activas con filtros opcionales.
     *
     * @param nombre Filtro parcial por nombre.
     * @param codigo Filtro parcial por código.
     * @param tipo   Filtro por tipo (TECNICA o ACTITUDINAL).
     * @return Lista de competencias encontradas.
     */
    @GetMapping
    public List<CompetenciaDTO> listar(
            @RequestParam(required = false) @Size(max = 75, message = "Máximo 75 caracteres.") String nombre,
            @RequestParam(required = false) @Size(max = 20, message = "Máximo 20 caracteres.") String codigo,
            @RequestParam(required = false) TipoCompetencia tipo) {
        return gestorCompetencias.listar(nombre, codigo, tipo);
    }

    /**
     * Obtiene el detalle de una competencia por su ID.
     */
    @GetMapping("/{id}")
    public CompetenciaDTO obtenerPorId(@PathVariable Long id) {
        return gestorCompetencias.buscarPorId(id);
    }

    /**
     * Da de alta una nueva competencia en el sistema (CU 13).
     *
     * @param request Datos de la competencia a registrar.
     * @return Competencia creada con código HTTP 201 Created.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompetenciaDTO> darDeAlta(@Valid @RequestBody CompetenciaDTO request) {
        CompetenciaDTO creada = gestorCompetencias.darDeAlta(request);
        URI location = URI.create("/api/competencias/" + creada.getId());
        return ResponseEntity.created(location).body(creada);
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorResponseDTO> handleRecursoDuplicado(
            RecursoDuplicadoException ex, HttpServletRequest request) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.CONFLICT.value(),
                "Recurso Duplicado",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponseDTO> handleRecursoNoEncontrado(
            RecursoNoEncontradoException ex, HttpServletRequest request) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.NOT_FOUND.value(),
                "Recurso No Encontrado",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Petición Inválida",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
