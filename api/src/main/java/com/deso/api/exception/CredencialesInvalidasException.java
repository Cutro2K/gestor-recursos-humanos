package com.deso.api.exception;

/**
 * Excepción lanzada durante la autenticación (CU01) cuando las credenciales proporcionadas
 * (usuario/contraseña de consultor o documento/clave de candidato) no coinciden o la cuenta no está activa.
 * Se traduce en una respuesta HTTP 401 Unauthorized.
 */
public class CredencialesInvalidasException extends RuntimeException {
    
    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}