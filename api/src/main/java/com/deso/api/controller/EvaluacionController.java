package com.deso.api.controller;

import com.deso.api.dto.EvaluacionRequestDTO;
import com.deso.api.dto.EvaluacionResponseDTO;
import com.deso.api.dto.PuestoDTO;
import com.deso.api.service.GestorEvaluacion;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST para la gestión de Evaluaciones y generación de cuestionarios (CU 25).
 */
@RestController
@RequestMapping(value = "/api/evaluaciones", produces = MediaType.APPLICATION_JSON_VALUE)
public class EvaluacionController {

    private final GestorEvaluacion gestorEvaluacion;

    public EvaluacionController(GestorEvaluacion gestorEvaluacion) {
        this.gestorEvaluacion = gestorEvaluacion;
    }

    /**
     * Lista los puestos activos disponibles para iniciar una evaluación.
     */
    @GetMapping("/puestos")
    public List<PuestoDTO> listarPuestos() {
        return gestorEvaluacion.listarPuestosActivos();
    }

    /**
     * Genera una nueva evaluación y sus cuestionarios para los candidatos seleccionados (CU 25).
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EvaluacionResponseDTO> generarEvaluacion(
            @Valid @RequestBody EvaluacionRequestDTO request) {
        EvaluacionResponseDTO creada = gestorEvaluacion.generarEvaluacion(request);
        URI location = URI.create("/api/evaluaciones/" + creada.getEvaluacionId());
        return ResponseEntity.created(location).body(creada);
    }
}
