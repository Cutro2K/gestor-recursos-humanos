package com.deso.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Parámetros de sesión y tokens del CU 01, leídos de {@code auth.*} en application.properties.
 */
@ConfigurationProperties("auth")
public record PropiedadesAuth(Jwt jwt, Sesion sesion, Cookies cookies) {

    /**
     * @param clavePrivada   Ruta al archivo PEM (PKCS#8) con la clave privada RSA.
     * @param emisor         Valor del claim {@code iss}; se verifica al leer un token.
     * @param duracionAccess Vida del access token.
     */
    public record Jwt(String clavePrivada, String emisor, Duration duracionAccess) {}

    /**
     * Vida total de la sesión desde el login. Refrescar no la extiende.
     */
    public record Sesion(Duration duracionConsultor, Duration duracionCandidato) {}

    /**
     * @param secure true en producción (HTTPS): el navegador solo manda la cookie cifrada.
     */
    public record Cookies(boolean secure) {}
}
