package com.deso.api.service;

import com.deso.api.dao.ConsultorDAO;
import com.deso.api.dao.CuestionarioDAO;
import com.deso.api.dao.DirectorioConsultores;
import com.deso.api.dao.SesionDAO;
import com.deso.api.dto.LoginCandidatoDTO;
import com.deso.api.dto.LoginConsultorDTO;
import com.deso.api.dto.SesionDTO;
import com.deso.api.exception.CredencialesInvalidasException;
import com.deso.api.model.entity.Candidato;
import com.deso.api.model.entity.Consultor;
import com.deso.api.model.entity.Cuestionario;
import com.deso.api.model.enums.EstadoCuestionario;
import com.deso.api.model.enums.TipoDocumento;
import com.deso.api.model.entity.Sesion;
import com.deso.api.security.EmisorTokens;
import com.deso.api.security.PropiedadesAuth;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GestorAutenticacionTests {

	private static final String CLAVE = "A1B2C3D4";
	private static final String MENSAJE_CANDIDATO =
			"Los datos ingresados no son válidos o no existe un cuestionario para el Candidato";

	private ConsultorDAO consultorDAO;
	private CuestionarioDAO cuestionarioDAO;
	private DirectorioConsultores directorio;
	private SesionDAO sesionDAO;
	private GestorAutenticacion gestor;

	@BeforeEach
	void preparar() {
		consultorDAO = mock(ConsultorDAO.class);
		cuestionarioDAO = mock(CuestionarioDAO.class);
		directorio = mock(DirectorioConsultores.class);
		sesionDAO = mock(SesionDAO.class);
		EmisorTokens emisor = mock(EmisorTokens.class);
		PropiedadesAuth propiedades = new PropiedadesAuth(
				new PropiedadesAuth.Jwt("sin-uso.pem", "deso-api", Duration.ofMinutes(10)),
				new PropiedadesAuth.Sesion(Duration.ofHours(8), Duration.ofHours(2)),
				new PropiedadesAuth.Cookies(false));

		when(sesionDAO.crear(any(Sesion.class))).thenAnswer(invocacion -> {
			Sesion sesion = invocacion.getArgument(0);
			sesion.setId(99L);
			return sesion;
		});
		when(emisor.emitirAccess(anyString(), anyString(), any())).thenReturn("access");
		when(emisor.emitirRefresh(anyLong(), anyLong(), anyString(), anyString(), any())).thenReturn("refresh");

		gestor = new GestorAutenticacion(consultorDAO, cuestionarioDAO, directorio, sesionDAO, emisor, propiedades);
	}

	@Test
	void candidatoValidoRegistraElAccesoYDevuelveSuSesion() {
		when(cuestionarioDAO.buscarPorClave(CLAVE)).thenReturn(Optional.of(cuestionario(EstadoCuestionario.ACTIVO)));
		when(cuestionarioDAO.registrarAcceso(eq(10L), any(LocalDateTime.class))).thenReturn(true);

		SesionDTO sesion = gestor.autenticarCandidato(login("40123456")).sesion();

		// La sesión del candidato se identifica por su cuestionario.
		assertThat(sesion.getSujetoId()).isEqualTo(10L);
		assertThat(sesion.getRol()).isEqualTo("CANDIDATO");
		verify(cuestionarioDAO, never()).marcarIncompleto(any(), any());
		verify(sesionDAO).borrarPorCuestionario(10L);
	}

	@Test
	void claveInexistenteUsaElMensajeUnicoDelCasoDeUso() {
		when(cuestionarioDAO.buscarPorClave(CLAVE)).thenReturn(Optional.empty());

		rechazaConMensaje(login("40123456"), MENSAJE_CANDIDATO);
	}

	@ParameterizedTest
	@EnumSource(value = EstadoCuestionario.class, names = {"COMPLETO", "INCOMPLETO", "SIN_CONTESTAR"})
	void cuestionarioCerradoUsaElMensajeUnicoYNoConsumeAccesos(EstadoCuestionario estado) {
		when(cuestionarioDAO.buscarPorClave(CLAVE)).thenReturn(Optional.of(cuestionario(estado)));

		rechazaConMensaje(login("40123456"), MENSAJE_CANDIDATO);
		verify(cuestionarioDAO, never()).registrarAcceso(any(), any());
	}

	@Test
	void documentoEquivocadoNoConsumeAccesos() {
		when(cuestionarioDAO.buscarPorClave(CLAVE)).thenReturn(Optional.of(cuestionario(EstadoCuestionario.ACTIVO)));

		rechazaConMensaje(login("99999999"), MENSAJE_CANDIDATO);
		verify(cuestionarioDAO, never()).registrarAcceso(any(), any());
	}

	@Test
	void candidatoEliminadoUsaElMensajeUnico() {
		Cuestionario cuestionario = cuestionario(EstadoCuestionario.ACTIVO);
		cuestionario.getCandidato().setEliminado(true);
		when(cuestionarioDAO.buscarPorClave(CLAVE)).thenReturn(Optional.of(cuestionario));

		rechazaConMensaje(login("40123456"), MENSAJE_CANDIDATO);
	}

	@Test
	void sinAccesosDisponiblesElCuestionarioQuedaIncompleto() {
		when(cuestionarioDAO.buscarPorClave(CLAVE)).thenReturn(Optional.of(cuestionario(EstadoCuestionario.EN_PROCESO)));
		when(cuestionarioDAO.registrarAcceso(eq(10L), any(LocalDateTime.class))).thenReturn(false);

		assertThatThrownBy(() -> gestor.autenticarCandidato(login("40123456")))
				.isInstanceOf(CredencialesInvalidasException.class)
				.hasMessageContaining("cantidad máxima de accesos");
		verify(cuestionarioDAO).marcarIncompleto(eq(10L), any(LocalDateTime.class));
	}

	@Test
	void consultorSeBuscaEnMayusculas() {
		Consultor consultor = new Consultor();
		consultor.setId(1L);
		consultor.setUsername("ADMIN");
		consultor.setActivo(true);
		when(consultorDAO.buscarPorUsername("ADMIN")).thenReturn(Optional.of(consultor));
		when(directorio.credencialesValidas("ADMIN", "admin123")).thenReturn(true);

		SesionDTO sesion = gestor.autenticarConsultor(new LoginConsultorDTO(" admin ", "admin123")).sesion();

		assertThat(sesion.getRol()).isEqualTo("CONSULTOR");
	}

	@Test
	void loginConsultorRespetaLosLimitesDelLdap() {
		try (ValidatorFactory fabrica = Validation.buildDefaultValidatorFactory()) {
			var validador = fabrica.getValidator();

			assertThat(validador.validate(new LoginConsultorDTO("A".repeat(15), "x".repeat(20)))).isEmpty();
			assertThat(validador.validate(new LoginConsultorDTO("A".repeat(16), "x"))).hasSize(1);
			assertThat(validador.validate(new LoginConsultorDTO("ADMIN", "x".repeat(21)))).hasSize(1);
		}
	}

	private void rechazaConMensaje(LoginCandidatoDTO login, String mensaje) {
		assertThatThrownBy(() -> gestor.autenticarCandidato(login))
				.isInstanceOf(CredencialesInvalidasException.class)
				.hasMessage(mensaje);
	}

	private LoginCandidatoDTO login(String documento) {
		return new LoginCandidatoDTO(TipoDocumento.DNI, documento, CLAVE);
	}

	private Cuestionario cuestionario(EstadoCuestionario estado) {
		Candidato candidato = new Candidato();
		candidato.setId(7L);
		candidato.setTipoDocumento(TipoDocumento.DNI);
		candidato.setNroDocumento("40123456");
		candidato.setEliminado(false);

		Cuestionario cuestionario = new Cuestionario();
		cuestionario.setId(10L);
		cuestionario.setEstado(estado);
		cuestionario.setCandidato(candidato);
		return cuestionario;
	}
}
