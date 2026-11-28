package com.deso.api.dao;

import com.deso.api.model.entity.RegistroAuditoria;

/**
 * Contrato de acceso a datos para persistir Registros de Auditoría ante bajas de catálogo.
 */
public interface AuditoriaDAO {

    /**
     * Persiste un Registro de Auditoría y devuelve la entidad con su identificador asignado.
     */
    RegistroAuditoria guardar(RegistroAuditoria registro);
}
