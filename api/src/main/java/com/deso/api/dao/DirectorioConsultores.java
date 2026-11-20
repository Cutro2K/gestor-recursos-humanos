package com.deso.api.dao;

/**
 * Directorio donde viven las credenciales de los consultores (CU 01).
 * La base de datos no guarda contraseñas: las valida este directorio.
 */
public interface DirectorioConsultores {

    /**
     * Indica si la contraseña es la correcta para el consultor con ese nombre de usuario.
     * Devuelve false si el usuario no existe en el directorio o si algún dato está vacío.
     */
    boolean credencialesValidas(String username, String contrasena);
}
