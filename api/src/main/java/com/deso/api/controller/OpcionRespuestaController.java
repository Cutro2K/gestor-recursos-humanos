package com.deso.api.controller;

import com.deso.api.dto.ListadoOpcionRespuestaDTO;
import com.deso.api.dto.OpcionRespuestaDTO;
import com.deso.api.service.GestorOpcionesRespuesta;
import com.deso.api.validation.OnAlta;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
 * Controlador REST para la gestión del catálogo de Opciones de Respuesta (/api/opciones-respuesta).
 *
 * Endpoints expuestos:
 * - POST /api/opciones-respuesta     (CU 29): Da de alta una nueva opción de respuesta con sus ítems de opción.
 * - GET /api/opciones-respuesta      (CU 24): Busca opciones de respuesta activas filtrando por nombre (paginado).
 * - GET /api/opciones-respuesta/{id} (CU 21 / CU 22 / CU 30): Obtiene el detalle de una opción de respuesta activa con sus ítems ordenados.
 */
@RestController
@RequestMapping(value = "/api/opciones-respuesta", produces = MediaType.APPLICATION_JSON_VALUE)
public class OpcionRespuestaController {

    private final GestorOpcionesRespuesta gestorOpcionesRespuesta;

    public OpcionRespuestaController(GestorOpcionesRespuesta gestorOpcionesRespuesta) {
        this.gestorOpcionesRespuesta = gestorOpcionesRespuesta;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OpcionRespuestaDTO> darDeAlta(
            @Validated(OnAlta.class) @RequestBody OpcionRespuestaDTO request) {
        OpcionRespuestaDTO creada = gestorOpcionesRespuesta.darDeAlta(request);
        URI location = URI.create("/api/opciones-respuesta/" + creada.getId());
        return ResponseEntity.created(location).body(creada);
    }

    @GetMapping
    public List<ListadoOpcionRespuestaDTO> buscarPorNombre(
            @RequestParam(required = false)
            @Size(max = 50, message = "El nombre no puede superar los 50 caracteres.")
            String nombre,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "El offset no puede ser negativo.")
            Integer offset,
            @RequestParam(defaultValue = "50")
            @Min(value = 1, message = "La cantidad mínima es 1.")
            @Max(value = 100, message = "La cantidad máxima es 100.")
            Integer cantidad) {
        return gestorOpcionesRespuesta.buscarPorNombre(nombre, offset, cantidad);
    }

    @GetMapping("/{id}")
    public OpcionRespuestaDTO obtenerPorId(
            @PathVariable @Positive(message = "El ID de la opción de respuesta debe ser positivo.") Long id) {
        return gestorOpcionesRespuesta.buscarPorId(id);
    }
}
