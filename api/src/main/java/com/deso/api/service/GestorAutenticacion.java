package com.deso.api.service;

import com.deso.api.dao.ConsultorDAO;
import com.deso.api.dao.CuestionarioDAO;
import com.deso.api.dao.DirectorioConsultores;
import com.deso.api.dao.SesionDAO;
import com.deso.api.dto.LoginCandidatoDTO;
import com.deso.api.dto.LoginConsultorDTO;
import com.deso.api.dto.SesionDTO;
import com.deso.api.dto.SesionEmitida;
import com.deso.api.exception.CredencialesInvalidasException;
import com.deso.api.model.entity.Candidato;
import com.deso.api.model.entity.Consultor;
import com.deso.api.model.entity.Cuestionario;
import com.deso.api.model.entity.Sesion;
import com.deso.api.model.enums.EstadoCuestionario;
import com.deso.api.security.EmisorTokens;
import com.deso.api.security.PropiedadesAuth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;

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

    /** Mismo mensaje para cualquier refresco rechazado: no revela por qué. */
    private static final String SESION_INVALIDA = "La sesión no es válida o venció";

    public static final String ROL_CONSULTOR = "CONSULTOR";
    public static final String ROL_CANDIDATO = "CANDIDATO";

    private final ConsultorDAO consultorDAO;
    private final CuestionarioDAO cuestionarioDAO;
    private final DirectorioConsultores directorioConsultores;
    private final SesionDAO sesionDAO;
    private final EmisorTokens emisorTokens;
    private final PropiedadesAuth propiedades;

    public GestorAutenticacion(ConsultorDAO consultorDAO,
                               CuestionarioDAO cuestionarioDAO,
                               DirectorioConsultores directorioConsultores,
                               SesionDAO sesionDAO,
                               EmisorTokens emisorTokens,
                               PropiedadesAuth propiedades) {
        this.consultorDAO = consultorDAO;
        this.cuestionarioDAO = cuestionarioDAO;
        this.directorioConsultores = directorioConsultores;
        this.sesionDAO = sesionDAO;
        this.emisorTokens = emisorTokens;
        this.propiedades = propiedades;
    }

    /**
     * Autentica a un consultor (CU 01, flujo Consultor).
     * Primero verifica en la base que exista y esté activo, y recién entonces valida la
     * contraseña contra el directorio LDAP, para no consultarlo por usuarios ya rechazados.
     * Si todo es válido abre una sesión de 8 h (configurable).
     */
    @Transactional
    public SesionEmitida autenticarConsultor(LoginConsultorDTO consultorDTO) {
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

        Sesion sesion = new Sesion();
        sesion.setConsultor(consultor);
        return abrirSesion(sesion, propiedades.sesion().duracionConsultor());
    }

    /**
     * Autentica a un candidato (CU 01, flujo Candidato).
     * Registra el acceso recién cuando pasaron todas las validaciones, así un error de tipeo no
     * consume accesos. Si ya no quedan, el cuestionario pasa a INCOMPLETO (CU 26): noRollbackFor
     * hace que ese cambio se guarde aunque el login termine rechazado con una excepción.
     * Abre una sesión de 2 h (configurable) y cierra la que el cuestionario tuviera abierta.
     */
    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public SesionEmitida autenticarCandidato(LoginCandidatoDTO loginCandidatoDTO) {
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

        // Una sola sesión por cuestionario: el login nuevo reemplaza a la anterior.
        sesionDAO.borrarPorCuestionario(cuestionario.getId());
        Sesion sesion = new Sesion();
        sesion.setCuestionario(cuestionario);
        return abrirSesion(sesion, propiedades.sesion().duracionCandidato());
    }

    /**
     * Canjea un refresh token por tokens nuevos (rotación). Cualquier anomalía borra la
     * sesión: un {@code seq} viejo significa que ese refresh token ya se usó, sea por otra
     * pestaña o por alguien que lo robó, y no hay forma de saber cuál de los dos es.
     * noRollbackFor hace que el borrado se guarde aunque se rechace con una excepción.
     */
    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public SesionEmitida refrescar(String refreshToken) {
        EmisorTokens.DatosRefresh datos = emisorTokens.leerRefresh(refreshToken, false)
                .orElseThrow(() -> new CredencialesInvalidasException(SESION_INVALIDA));

        Sesion sesion = sesionDAO.buscarPorId(datos.sesionId())
                .orElseThrow(() -> new CredencialesInvalidasException(SESION_INVALIDA));

        boolean valida = datos.sujeto().equals(sujeto(sesion))
                && datos.rol().equals(rol(sesion))
                && datos.seq() == sesion.getSeq()
                && sesion.getExpiraEn().isAfter(LocalDateTime.now())
                && titularHabilitado(sesion)
                // Atómico: si dos refrescos traen el mismo seq, solo uno rota.
                && sesionDAO.rotar(sesion.getId(), datos.seq());
        if (!valida) {
            sesionDAO.borrar(sesion.getId());
            throw new CredencialesInvalidasException(SESION_INVALIDA);
        }

        return emitir(sesion, datos.seq() + 1);
    }

    /**
     * Cierra la sesión del refresh token, si es auténtico. Acepta tokens vencidos para poder
     * borrar también la fila de una sesión expirada. Un token inválido no hace nada.
     */
    @Transactional
    public void cerrarSesion(String refreshToken) {
        emisorTokens.leerRefresh(refreshToken, true)
                .ifPresent(datos -> sesionDAO.borrar(datos.sesionId()));
    }

    private SesionEmitida abrirSesion(Sesion sesion, Duration duracion) {
        LocalDateTime ahora = LocalDateTime.now();
        sesion.setInicio(ahora);
        sesion.setExpiraEn(ahora.plus(duracion));
        sesion.setSeq(0L);
        return emitir(sesionDAO.crear(sesion), 0L);
    }

    private SesionEmitida emitir(Sesion sesion, long seq) {
        String sujeto = sujeto(sesion);
        String rol = rol(sesion);
        Instant sesionExpira = sesion.getExpiraEn().atZone(ZoneId.systemDefault()).toInstant();
        // El access token nunca sobrevive a la sesión que lo respalda.
        Instant accessExpira = min(Instant.now().plus(propiedades.jwt().duracionAccess()), sesionExpira);

        return new SesionEmitida(
                new SesionDTO(Long.valueOf(sujeto), rol),
                emisorTokens.emitirAccess(sujeto, rol, accessExpira),
                emisorTokens.emitirRefresh(sesion.getId(), seq, sujeto, rol, sesionExpira),
                sesionExpira);
    }

    /** Consultor: activo. Candidato: cuestionario abierto y candidato no eliminado. */
    private boolean titularHabilitado(Sesion sesion) {
        if (sesion.getConsultor() != null) {
            return Boolean.TRUE.equals(sesion.getConsultor().getActivo());
        }
        Cuestionario cuestionario = sesion.getCuestionario();
        boolean abierto = cuestionario.getEstado() == EstadoCuestionario.ACTIVO
                || cuestionario.getEstado() == EstadoCuestionario.EN_PROCESO;
        Candidato candidato = cuestionario.getCandidato();
        return abierto && candidato != null && !Boolean.TRUE.equals(candidato.getEliminado());
    }

    /** {@code sub} de los tokens: el id del consultor, o el del cuestionario para un candidato. */
    private static String sujeto(Sesion sesion) {
        Long id = sesion.getConsultor() != null ? sesion.getConsultor().getId() : sesion.getCuestionario().getId();
        return String.valueOf(Objects.requireNonNull(id));
    }

    private static String rol(Sesion sesion) {
        return sesion.getConsultor() != null ? ROL_CONSULTOR : ROL_CANDIDATO;
    }

    private static Instant min(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }
}
