package com.deso.api.dao;

import com.deso.api.model.entity.Consultor;
import java.util.Optional;

public interface ConsultorDAO {
    
    /**
     * Busca un consultor activo por su nombre de usuario (para inicio de sesión en CU 01).
     */
    Optional<Consultor> buscarPorUsername(String username);
    /**
     * Busca un consultor por su identificador primario (ID).
     */
    Optional<Consultor> buscarPorId(Long id);
    /**
     * Busca un consultor por su dirección de email (útil para validar unicidad).
     */
    Optional<Consultor> buscarPorEmail(String email);
    /**
     * Inserta un nuevo consultor o actualiza uno existente de forma transaccional.
     */
    Consultor guardar(Consultor consultor);
}