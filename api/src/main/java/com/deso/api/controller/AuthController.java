package com.deso.api.controller;

import com.deso.api.dto.LoginCandidatoDTO;
import com.deso.api.dto.LoginConsultorDTO;
import com.deso.api.dto.SesionDTO;
import com.deso.api.service.GestorAutenticacion;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el CU 01 - Autenticar Usuario.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final GestorAutenticacion gestorAutenticacion;

    public AuthController(GestorAutenticacion gestorAutenticacion) {
        this.gestorAutenticacion = gestorAutenticacion;
    }

    /**
     * POST /api/auth/consultor
     * Inicia sesión para un Consultor con usuario y contraseña.
     */
    @PostMapping("/consultor")
    public ResponseEntity<SesionDTO> loginConsultor(@Valid @RequestBody LoginConsultorDTO loginConsultorDTO) {
        SesionDTO sesion = gestorAutenticacion.autenticarConsultor(loginConsultorDTO);
        return ResponseEntity.ok(sesion);
    }

    /**
     * POST /api/auth/candidato
     * Inicia sesión para un Candidato con tipo/nro de documento y clave de cuestionario.
     */
    @PostMapping("/candidato")
    public ResponseEntity<SesionDTO> loginCandidato(@Valid @RequestBody LoginCandidatoDTO loginCandidatoDTO) {
        SesionDTO sesion = gestorAutenticacion.autenticarCandidato(loginCandidatoDTO);
        return ResponseEntity.ok(sesion);
    }
}
