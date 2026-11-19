package com.deso.api.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.deso.api.dto.FactorDTO;
import com.deso.api.dto.ListadoGestorFactoresDTO;
import com.deso.api.service.GestorFactores;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PutMapping;



/**
 * Controlador REST para la gestión del catálogo de Factores (/api/factores).
 *
 * Endpoints expuestos:
 * - POST   /api/factores                          (CU17): Da de alta un nuevo factor validando unicidad y devuelve 201 Created.
 * - PUT    /api/factores/{id}                     (CU18): Modifica los datos de un factor existente y devuelve 200 OK.
 * - DELETE /api/factores/{id}                     (CU19): Realiza la baja lógica de un factor y devuelve 204 No Content.
 * - GET    /api/factores                          (CU16 / CU20): Busca factores activos filtrando por nombre de competencia y/o nombre de factor (paginado).
 * - GET    /api/factores/competencia/{competenciaId} (CU21 / CU22): Lista todos los factores activos asociados a una competencia, ordenados por orden.
 * - GET    /api/factores/{id}                     (CU18): Obtiene el detalle de un factor activo por su ID.
 */
@RestController 
@RequestMapping("/api/factores")
public class FactorController {
    
    private final GestorFactores gestorFactores;

    public FactorController(GestorFactores gestorFactores) {
        this.gestorFactores = gestorFactores;
    }
    
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FactorDTO> darDeAlta(@Valid @RequestBody FactorDTO request) {
        FactorDTO creado = gestorFactores.darDeAlta(request);
        URI location = URI.create("/api/factores/" + creado.getId());
        return ResponseEntity.created(location).body(creado);
    }
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FactorDTO> actualizarFactor(
            @PathVariable @Positive(message = "El ID del factor debe ser positivo.") Long id,
            @Valid @RequestBody FactorDTO request
        ) {
        request.setId(id);
        FactorDTO actualizado = gestorFactores.actualizar(request);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable @Positive(message = "El ID del factor debe ser positivo.") Long id) {
        gestorFactores.eliminarLogicamente(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping
    public List<ListadoGestorFactoresDTO> buscarPorNombres(
            @RequestParam(required = false)
            @Size(max = 75, message = "El nombre de competencia no puede superar los 75 caracteres.")
            String nombreCompetencia,
            @RequestParam(required = false)
            @Size(max = 50, message = "El nombre de factor no puede superar los 50 caracteres.")
            String nombreFactor,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "El offset no puede ser negativo.")
            Integer offset,
            @RequestParam(defaultValue = "50")
            @Min(value = 1, message = "La cantidad mínima es 1.")
            @Max(value = 100, message = "La cantidad máxima es 100.")
            Integer cantidad) {
        return gestorFactores.buscarPorNombres(nombreCompetencia, nombreFactor, offset, cantidad);
    }

    @GetMapping("/competencia/{competenciaId}")
    public List<FactorDTO> listarPorCompetencia(
            @PathVariable @Positive(message = "El ID de competencia debe ser positivo.") Long competenciaId) {
        return gestorFactores.listarPorCompetencia(competenciaId);
    }
    
    @GetMapping("/{id}")
    public FactorDTO obtenerPorId(
            @PathVariable @Positive(message = "El ID del factor debe ser positivo.") Long id) {
        return gestorFactores.buscarPorId(id);
    }
}
