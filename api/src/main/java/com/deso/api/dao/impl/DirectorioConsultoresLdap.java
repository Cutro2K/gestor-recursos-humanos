package com.deso.api.dao.impl;

import com.deso.api.dao.DirectorioConsultores;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.stereotype.Repository;

import javax.naming.Name;

/**
 * Valida las contraseñas de los consultores contra un servidor LDAP.
 * Busca la entrada cuyo uid es el username y luego hace un bind con su DN y la contraseña:
 * la comparación la hace el servidor, la API nunca lee la contraseña guardada.
 */
@Repository
public class DirectorioConsultoresLdap implements DirectorioConsultores {

    private final LdapTemplate ldapTemplate;
    private final Name ramaConsultores;

    public DirectorioConsultoresLdap(LdapTemplate ldapTemplate,
                                     @Value("${ldap.consultores.ou}") String ramaConsultores) {
        this.ldapTemplate = ldapTemplate;
        this.ramaConsultores = LdapUtils.newLdapName(ramaConsultores);
    }

    @Override
    public boolean credencialesValidas(String username, String contrasena) {
        // Un bind con contraseña vacía es un bind anónimo y el servidor lo aceptaría.
        if (username == null || username.isBlank() || contrasena == null || contrasena.isEmpty()) {
            return false;
        }

        // EqualsFilter escapa caracteres especiales (*, (, ), \) para evitar inyección en el filtro.
        String filtro = new EqualsFilter("uid", username.strip()).encode();
        return ldapTemplate.authenticate(ramaConsultores, filtro, contrasena);
    }
}
