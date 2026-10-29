package com.deso.api.service;

import com.deso.api.dao.ConsultorDAO;
import com.deso.api.dao.CuestionarioDAO;
import com.deso.api.dto.LoginCandidatoDTO;
import com.deso.api.dto.LoginConsultorDTO;
import com.deso.api.dto.SesionDTO;
import com.deso.api.exception.CredencialesInvalidasException;
import com.deso.api.model.entity.Candidato;
import com.deso.api.model.entity.Consultor;
import com.deso.api.model.entity.Cuestionario;
import com.deso.api.model.enums.EstadoCuestionario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestor responsable de los flujos de autenticación de usuarios (CU 01).
 * Soporta la autenticación tanto de Consultores como de Candidatos.
 */
@Service
@Transactional(readOnly = true)
public class GestorAutenticacion {

    private final ConsultorDAO consultorDAO;
    private final CuestionarioDAO cuestionarioDAO;

    public GestorAutenticacion(ConsultorDAO consultorDAO,
                               CuestionarioDAO cuestionarioDAO) {
        this.consultorDAO = consultorDAO;
        this.cuestionarioDAO = cuestionarioDAO;
    }

    /**
     * Autentica a un consultor validando su nombre de usuario y contraseña.
     * Corresponde a CU 01 (Flujo Consultor).
     */
    public SesionDTO autenticarConsultor(LoginConsultorDTO consultorDTO) {
        if (consultorDTO == null || consultorDTO.getUsuario() == null || consultorDTO.getContrasena() == null) {
            throw new CredencialesInvalidasException("El usuario o la contraseña no son válidos");
        }

        Consultor consultor = consultorDAO.buscarPorUsername(consultorDTO.getUsuario().strip())
                .orElseThrow(() -> new CredencialesInvalidasException("El usuario o la contraseña no son válidos"));

        if (consultor.getActivo() == null || !consultor.getActivo()) {
            throw new CredencialesInvalidasException("El consultor se encuentra inactivo");
        }

        // Pendiente de refactor con hash BCrypt / Spring Security
        if (!consultor.getPassword().equals(consultorDTO.getContrasena())) {
            throw new CredencialesInvalidasException("El usuario o la contraseña no son válidos");
        }

        return new SesionDTO(consultor.getId(), "CONSULTOR");
    }

    public SesionDTO autenticarCandidato(LoginCandidatoDTO loginCandidatoDTO) {
        if (loginCandidatoDTO == null || loginCandidatoDTO.getTipoDocumento() == null 
                || loginCandidatoDTO.getNroDocumento() == null || loginCandidatoDTO.getClave() == null) {
            throw new CredencialesInvalidasException("Los datos ingresados no son válidos o no existe un cuestionario para el Candidato");
        }

        String clave = loginCandidatoDTO.getClave().strip();

        // 1. Buscamos el cuestionario por su clave única indexada
        Cuestionario cuestionario = cuestionarioDAO.buscarPorClave(clave)
                .orElseThrow(() -> new CredencialesInvalidasException("No existe el cuestionario con la clave ingresada"));

        // 2. Verificamos que el cuestionario esté disponible para responder
        boolean estadoValido = cuestionario.getEstado() == EstadoCuestionario.ACTIVO 
                || cuestionario.getEstado() == EstadoCuestionario.EN_PROCESO;
        if (!estadoValido) {
            throw new CredencialesInvalidasException("El cuestionario ya cerro o no se encuentra activo");
        }

        // 3 Validamos que el candidato asignado al cuestionario coincida con los datos ingresados
        Candidato candidato = cuestionario.getCandidato();
        if (candidato == null || Boolean.TRUE.equals(candidato.getEliminado())) {
            throw new CredencialesInvalidasException("Los datos ingresados no son válidos o no existe un cuestionario para el Candidato");
        }

        boolean coincideTipoDoc = candidato.getTipoDocumento() == loginCandidatoDTO.getTipoDocumento();
        boolean coincideNroDoc = candidato.getNroDocumento().equalsIgnoreCase(loginCandidatoDTO.getNroDocumento().strip());

        if (!coincideTipoDoc || !coincideNroDoc) {
            throw new CredencialesInvalidasException("Los datos ingresados son invalidos");
        }

        // 4. Verificamos que no haya superado el límite de accesos permitidos
        if (cuestionario.getCantidadAccesos() != null && cuestionario.getCantidadMaximaAccesos() != null
                && cuestionario.getCantidadAccesos() >= cuestionario.getCantidadMaximaAccesos()) {
            throw new CredencialesInvalidasException("El cuestionario ha superado la cantidad máxima de accesos permitidos");
        }

        return new SesionDTO(candidato.getId(), "CANDIDATO");
    }
}
