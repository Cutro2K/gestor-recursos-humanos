package com.deso.api.controller;

import com.deso.api.dto.LoginCandidatoDTO;
import com.deso.api.dto.LoginConsultorDTO;
import com.deso.api.dto.SesionDTO;
import com.deso.api.dto.SesionEmitida;
import com.deso.api.security.ClaveFirma;
import com.deso.api.security.PropiedadesAuth;
import com.deso.api.service.GestorAutenticacion;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Controlador REST para el CU 01 - Autenticar Usuario.
 * Los tokens viajan solo en cookies HttpOnly: el JavaScript del front nunca los ve.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String COOKIE_ACCESS = "access_token";
    public static final String COOKIE_REFRESH = "refresh_token";
    /** El refresh token solo se manda a /api/auth (refresco y logout), no en cada request. */
    private static final String PATH_REFRESH = "/api/auth";

    private final GestorAutenticacion gestorAutenticacion;
    private final ClaveFirma claveFirma;
    private final boolean cookiesSeguras;

    public AuthController(GestorAutenticacion gestorAutenticacion, ClaveFirma claveFirma, PropiedadesAuth propiedades) {
        this.gestorAutenticacion = gestorAutenticacion;
        this.claveFirma = claveFirma;
        this.cookiesSeguras = propiedades.cookies().secure();
    }

    /**
     * POST /api/auth/consultor
     * Inicia sesión para un Consultor con usuario y contraseña.
     */
    @PostMapping("/consultor")
    public ResponseEntity<SesionDTO> loginConsultor(@Valid @RequestBody LoginConsultorDTO loginConsultorDTO) {
        return conCookies(gestorAutenticacion.autenticarConsultor(loginConsultorDTO));
    }

    /**
     * POST /api/auth/candidato
     * Inicia sesión para un Candidato con tipo/nro de documento y clave de cuestionario.
     */
    @PostMapping("/candidato")
    public ResponseEntity<SesionDTO> loginCandidato(@Valid @RequestBody LoginCandidatoDTO loginCandidatoDTO) {
        return conCookies(gestorAutenticacion.autenticarCandidato(loginCandidatoDTO));
    }

    /**
     * POST /api/auth/refresco
     * Canjea la cookie de refresco por tokens nuevos. Si no es válida responde 401 y el
     * front manda al usuario a /login.
     */
    @PostMapping("/refresco")
    public ResponseEntity<SesionDTO> refrescar(@CookieValue(name = COOKIE_REFRESH, required = false) String refreshToken) {
        return conCookies(gestorAutenticacion.refrescar(refreshToken));
    }

    /**
     * POST /api/auth/logout
     * Cierra la sesión en el servidor y borra las cookies. Responde 204 aunque no hubiera sesión.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = COOKIE_REFRESH, required = false) String refreshToken) {
        gestorAutenticacion.cerrarSesion(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie(COOKIE_ACCESS, "", "/", Duration.ZERO).toString())
                .header(HttpHeaders.SET_COOKIE, cookie(COOKIE_REFRESH, "", PATH_REFRESH, Duration.ZERO).toString())
                .build();
    }

    /**
     * GET /api/auth/jwks.json
     * Clave pública con la que el front (proxy.ts) verifica la firma de los access tokens.
     */
    @GetMapping("/jwks.json")
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(claveFirma.jwksPublico());
    }

    private ResponseEntity<SesionDTO> conCookies(SesionEmitida emitida) {
        // Las dos cookies viven lo que la sesión. La de acceso dura más que su JWT a propósito:
        // así proxy.ts distingue "token vencido, hay que refrescar" de "no hay sesión".
        Duration vida = Duration.between(Instant.now(), emitida.sesionExpira());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(COOKIE_ACCESS, emitida.accessToken(), "/", vida).toString())
                .header(HttpHeaders.SET_COOKIE, cookie(COOKIE_REFRESH, emitida.refreshToken(), PATH_REFRESH, vida).toString())
                .cacheControl(CacheControl.noStore())
                .body(emitida.sesion());
    }

    private ResponseCookie cookie(String nombre, String valor, String path, Duration vida) {
        return ResponseCookie.from(nombre, valor)
                .httpOnly(true)
                .secure(cookiesSeguras)
                .sameSite("Lax")
                .path(path)
                .maxAge(vida)
                .build();
    }
}
