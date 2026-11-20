package com.deso.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginConsultorDTO {
    // Límites del LDAP según el CU 01. Mismo mensaje que el rechazo de credenciales.
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 15, message = "El usuario o la contraseña no son válidos")
    private String usuario;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 20, message = "El usuario o la contraseña no son válidos")
    private String contrasena;
}