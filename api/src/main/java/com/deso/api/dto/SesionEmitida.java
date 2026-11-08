package com.deso.api.dto;

import java.time.Instant;

/**
 * Resultado de un login o refresco: lo que ve el front en el body ({@code sesion}) y los
 * tokens que el controlador pone en cookies HttpOnly, válidas hasta {@code sesionExpira}.
 */
public record SesionEmitida(SesionDTO sesion, String accessToken, String refreshToken, Instant sesionExpira) {}
