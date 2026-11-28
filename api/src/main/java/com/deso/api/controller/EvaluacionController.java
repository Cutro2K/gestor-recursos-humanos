package com.deso.api.controller;

import com.deso.api.dto.ArchivoDTO;
import com.deso.api.dto.CandidatosAEvaluarDTO;
import com.deso.api.dto.CompetenciaPuestoDTO;
import com.deso.api.dto.EvaluacionDTO;
import com.deso.api.dto.EvaluacionResultadoDTO;
import com.deso.api.dto.PuestoDTO;
import com.deso.api.exception.CredencialesInvalidasException;
import com.deso.api.security.EmisorTokens;
import com.deso.api.service.GestorAutenticacion;
import com.deso.api.service.GestorEvaluacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controlador REST del CU 25: Evaluar a Candidatos (/api/evaluaciones).
 *
 * Endpoints expuestos:
 * - GET  /api/evaluaciones/puestos                       (paso 8): puestos que se pueden elegir.
 * - GET  /api/evaluaciones/puestos/{id}/competencias     (paso 9 / 9.A): competencias, ponderación y si son evaluables.
 * - POST /api/evaluaciones/verificacion-candidatos       (paso 6 / 6.A): 204 o 409 si alguno tiene un cuestionario abierto.
 * - POST /api/evaluaciones                               (pasos 12–13): genera la evaluación y las claves de ingreso.
 * - GET  /api/evaluaciones/{id}/claves                   (pasos 15–17): exporta las claves a Excel (.xls).
 *
 * Generar la evaluación y exportar las claves requieren una sesión de consultor.
 */
@RestController
@RequestMapping(value = "/api/evaluaciones", produces = MediaType.APPLICATION_JSON_VALUE)
public class EvaluacionController {

    private final GestorEvaluacion gestorEvaluacion;
    private final EmisorTokens emisorTokens;

    public EvaluacionController(GestorEvaluacion gestorEvaluacion, EmisorTokens emisorTokens) {
        this.gestorEvaluacion = gestorEvaluacion;
        this.emisorTokens = emisorTokens;
    }

    @GetMapping("/puestos")
    public List<PuestoDTO> listarPuestos() {
        return gestorEvaluacion.listarPuestosActivos();
    }

    @GetMapping("/puestos/{puestoId}/competencias")
    public List<CompetenciaPuestoDTO> consultarCompetenciasPuesto(
            @PathVariable @Positive(message = "El ID del puesto debe ser positivo.") Long puestoId) {
        return gestorEvaluacion.consultarCompetenciasPuesto(puestoId);
    }

    @PostMapping(value = "/verificacion-candidatos", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> verificarCandidatos(@Valid @RequestBody CandidatosAEvaluarDTO datos) {
        gestorEvaluacion.verificarCandidatos(datos.candidatoIds());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EvaluacionResultadoDTO> evaluarCandidatos(
            @Valid @RequestBody EvaluacionDTO datos,
            @CookieValue(name = AuthController.COOKIE_ACCESS, required = false) String accessToken) {
        EvaluacionResultadoDTO resultado = gestorEvaluacion.evaluarCandidatos(datos, consultorEnSesion(accessToken));
        URI location = URI.create("/api/evaluaciones/" + resultado.evaluacionId());
        return ResponseEntity.created(location).body(resultado);
    }

    @GetMapping(value = "/{evaluacionId}/claves", produces = {"application/vnd.ms-excel", MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<byte[]> exportarClaves(
            @PathVariable @Positive(message = "El ID de la evaluación debe ser positivo.") Long evaluacionId,
            @CookieValue(name = AuthController.COOKIE_ACCESS, required = false) String accessToken) {
        consultorEnSesion(accessToken);
        ArchivoDTO archivo = gestorEvaluacion.exportarClavesExcel(evaluacionId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archivo.nombre(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(archivo.contenido());
    }

    private Long consultorEnSesion(String accessToken) {
        return emisorTokens.leerAccess(accessToken)
                .filter(d -> GestorAutenticacion.ROL_CONSULTOR.equals(d.rol()))
                .map(EmisorTokens.DatosAccess::sujetoId)
                .orElseThrow(() -> new CredencialesInvalidasException("La sesión no es válida o venció"));
    }
}
