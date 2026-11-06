package com.deso.api.service;

import com.deso.api.dao.ConsultorDAO;
import com.deso.api.dao.CuestionarioDAO;
import com.deso.api.dao.DirectorioConsultores;
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

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Gestor responsable de los flujos de autenticación de usuarios (CU 01).
 * Soporta la autenticación tanto de Consultores como de Candidatos.
 */
@Service
@Transactional(readOnly = true)
public class GestorAutenticacion {

    /** Mismo mensaje para todos los rechazos: no revela si el usuario existe o está inactivo. */
    private static final String CREDENCIALES_INVALIDAS = "El usuario o la contraseña no son válidos";

    /** Mensaje único que exige el CU 01 (3.A.2.2) para cualquier dato inválido del candidato. */
    private static final String DATOS_CANDIDATO_INVALIDOS =
            "Los datos ingresados no son válidos o no existe un cuestionario para el Candidato";

    /** Solo lo ve quien ya acertó documento y clave, así que no le revela nada a un extraño. */
    private static final String ACCESOS_SUPERADOS =
            "Superó la cantidad máxima de accesos permitidos: el cuestionario quedó incompleto";

    private final ConsultorDAO consultorDAO;
    private final CuestionarioDAO cuestionarioDAO;
    private final DirectorioConsultores directorioConsultores;

    public GestorAutenticacion(ConsultorDAO consultorDAO,
                               CuestionarioDAO cuestionarioDAO,
                               DirectorioConsultores directorioConsultores) {
        this.consultorDAO = consultorDAO;
        this.cuestionarioDAO = cuestionarioDAO;
        this.directorioConsultores = directorioConsultores;
    }

    /**
     * Autentica a un consultor (CU 01, flujo Consultor).
     * Primero verifica en la base que exista y esté activo, y recién entonces valida la
     * contraseña contra el directorio LDAP, para no consultarlo por usuarios ya rechazados.
     */
    public SesionDTO autenticarConsultor(LoginConsultorDTO consultorDTO) {
        if (consultorDTO == null || consultorDTO.getUsuario() == null || consultorDTO.getContrasena() == null) {
            throw new CredencialesInvalidasException(CREDENCIALES_INVALIDAS);
        }

        // CU 01: el identificador de usuario es siempre en mayúsculas.
        Consultor consultor = consultorDAO.buscarPorUsername(consultorDTO.getUsuario().strip().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new CredencialesInvalidasException(CREDENCIALES_INVALIDAS));

        if (!Boolean.TRUE.equals(consultor.getActivo())) {
            throw new CredencialesInvalidasException(CREDENCIALES_INVALIDAS);
        }

        // username de la base = uid en LDAP: así se vinculan las dos fuentes.
        if (!directorioConsultores.credencialesValidas(consultor.getUsername(), consultorDTO.getContrasena())) {
            throw new CredencialesInvalidasException(CREDENCIALES_INVALIDAS);
        }

        return new SesionDTO(consultor.getId(), "CONSULTOR");
    }

    /**
     * Autentica a un candidato (CU 01, flujo Candidato).
     * Registra el acceso recién cuando pasaron todas las validaciones, así un error de tipeo no
     * consume accesos. Si ya no quedan, el cuestionario pasa a INCOMPLETO (CU 26): noRollbackFor
     * hace que ese cambio se guarde aunque el login termine rechazado con una excepción.
     */
    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public SesionDTO autenticarCandidato(LoginCandidatoDTO loginCandidatoDTO) {
        if (loginCandidatoDTO == null || loginCandidatoDTO.getTipoDocumento() == null
                || loginCandidatoDTO.getNroDocumento() == null || loginCandidatoDTO.getClave() == null) {
            throw new CredencialesInvalidasException(DATOS_CANDIDATO_INVALIDOS);
        }

        Cuestionario cuestionario = cuestionarioDAO.buscarPorClave(loginCandidatoDTO.getClave().strip())
                .orElseThrow(() -> new CredencialesInvalidasException(DATOS_CANDIDATO_INVALIDOS));

        boolean estadoValido = cuestionario.getEstado() == EstadoCuestionario.ACTIVO
                || cuestionario.getEstado() == EstadoCuestionario.EN_PROCESO;
        Candidato candidato = cuestionario.getCandidato();
        if (!estadoValido || candidato == null || Boolean.TRUE.equals(candidato.getEliminado())) {
            throw new CredencialesInvalidasException(DATOS_CANDIDATO_INVALIDOS);
        }

        boolean coincideTipoDoc = candidato.getTipoDocumento() == loginCandidatoDTO.getTipoDocumento();
        boolean coincideNroDoc = candidato.getNroDocumento().equalsIgnoreCase(loginCandidatoDTO.getNroDocumento().strip());
        if (!coincideTipoDoc || !coincideNroDoc) {
            throw new CredencialesInvalidasException(DATOS_CANDIDATO_INVALIDOS);
        }

        LocalDateTime ahora = LocalDateTime.now();
        if (!cuestionarioDAO.registrarAcceso(cuestionario.getId(), ahora)) {
            cuestionarioDAO.marcarIncompleto(cuestionario.getId(), ahora);
            throw new CredencialesInvalidasException(ACCESOS_SUPERADOS);
        }

        return new SesionDTO(candidato.getId(), "CANDIDATO");
    }
}
