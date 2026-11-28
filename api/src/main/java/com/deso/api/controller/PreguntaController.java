package com.deso.api.controller;

import com.deso.api.dto.ListadoPreguntaDTO;
import com.deso.api.dto.PreguntaDTO;
import com.deso.api.service.GestorPreguntas;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST para la gestión del banco de Preguntas (CU 20 y CU 21).
 * GET /api/preguntas (CU 20)
 * Lista y filtra preguntas activas (no eliminadas) con paginación.
 * 
 * GET /api/preguntas/{id}
 * Obtiene el detalle de una pregunta activa por su ID.
 * 
 * `POST /api/preguntas`
 */
@RestController
@RequestMapping(value = "/api/preguntas", produces = MediaType.APPLICATION_JSON_VALUE)
public class PreguntaController {

    private final GestorPreguntas gestorPreguntas;

    public PreguntaController(GestorPreguntas gestorPreguntas) {
        this.gestorPreguntas = gestorPreguntas;
    }

    @GetMapping
    public List<ListadoPreguntaDTO> listar(
            @RequestParam(required = false)
            @Positive(message = "El ID de competencia debe ser positivo.")
            Long competenciaId,
            @RequestParam(required = false)
            @Positive(message = "El ID de factor debe ser positivo.")
            Long factorId,
            @RequestParam(required = false)
            @Size(max = 50, message = "El nombre no puede superar los 50 caracteres.")
            String nombre,
            @RequestParam(required = false)
            @Size(max = 255, message = "El enunciado no puede superar los 255 caracteres.")
            String enunciado,
            @RequestParam(required = false)
            Boolean activa,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "El offset no puede ser negativo.")
            Integer offset,
            @RequestParam(defaultValue = "50")
            @Min(value = 1, message = "La cantidad mínima es 1.")
            @Max(value = 100, message = "La cantidad máxima es 100.")
            Integer cantidad) {
        return gestorPreguntas.buscarConFiltros(
                competenciaId, factorId, nombre, enunciado, activa, offset, cantidad);
    }

    @GetMapping("/{id}")
    public PreguntaDTO obtenerPorId(
            @PathVariable @Positive(message = "El ID de la pregunta debe ser positivo.") Long id) {
        return gestorPreguntas.buscarPorId(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PreguntaDTO> darDeAlta(@Valid @RequestBody PreguntaDTO request) {
        PreguntaDTO creada = gestorPreguntas.darDeAlta(request);
        URI location = URI.create("/api/preguntas/" + creada.getId());
        return ResponseEntity.created(location).body(creada);
    }
}
