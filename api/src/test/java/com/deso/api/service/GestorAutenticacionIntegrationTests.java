package com.deso.api.service;

import com.deso.api.dto.LoginCandidatoDTO;
import com.deso.api.exception.CredencialesInvalidasException;
import com.deso.api.model.enums.TipoDocumento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sin @Transactional a propósito: verifica lo que realmente queda guardado tras cada login. */
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:cu01-service-tests;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa", "spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
		"logging.file.name=target/cu01-service-tests.log"
})
class GestorAutenticacionIntegrationTests {

	private static final String DOCUMENTO = "40123456";

	@Autowired
	private GestorAutenticacion gestor;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void limpiarBase() {
		// El cuestionario apunta a una evaluación que estas pruebas no necesitan crear.
		jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
		jdbc.update("DELETE FROM cuestionario");
		jdbc.update("DELETE FROM candidato");
	}

	@Test
	void unAccesoValidoSumaUnoYRegistraElIngreso() {
		crearCuestionario("CLAVE001", 0, 3);

		gestor.autenticarCandidato(login("CLAVE001"));

		assertThat(accesos("CLAVE001")).isEqualTo(1);
		assertThat(jdbc.queryForObject(
				"SELECT COUNT(*) FROM cuestionario WHERE clave_ingreso = 'CLAVE001' AND ultimo_ingreso IS NOT NULL",
				Integer.class)).isEqualTo(1);
	}

	@Test
	void superarElLimiteGuardaIncompletoAunqueElLoginSeRechace() {
		crearCuestionario("CLAVE002", 3, 3);

		assertThatThrownBy(() -> gestor.autenticarCandidato(login("CLAVE002")))
				.isInstanceOf(CredencialesInvalidasException.class);

		assertThat(estado("CLAVE002")).isEqualTo("INCOMPLETO");
		assertThat(accesos("CLAVE002")).isEqualTo(3);
	}

	@Test
	void dosLoginsSimultaneosNoPuedenTomarElUltimoAcceso() throws Exception {
		crearCuestionario("CLAVE003", 2, 3);
		CountDownLatch largada = new CountDownLatch(1);
		Callable<Boolean> intento = () -> {
			largada.await();
			gestor.autenticarCandidato(login("CLAVE003"));
			return true;
		};

		try (ExecutorService hilos = Executors.newFixedThreadPool(2)) {
			List<Future<Boolean>> resultados = IntStream.range(0, 2).mapToObj(i -> hilos.submit(intento)).toList();
			largada.countDown();

			long exitosos = resultados.stream().filter(this::tuvoExito).count();
			assertThat(exitosos).isEqualTo(1);
		}
		assertThat(accesos("CLAVE003")).isEqualTo(3);
	}

	private boolean tuvoExito(Future<Boolean> resultado) {
		try {
			return resultado.get();
		} catch (ExecutionException e) {
			assertThat(e.getCause()).isInstanceOf(CredencialesInvalidasException.class);
			return false;
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	private LoginCandidatoDTO login(String clave) {
		return new LoginCandidatoDTO(TipoDocumento.DNI, DOCUMENTO, clave);
	}

	private void crearCuestionario(String clave, int accesos, int maximo) {
		jdbc.update("""
				INSERT INTO candidato (nro_candidato, apellido, nombre, tipo_documento, nro_documento,
				    fecha_nacimiento, genero, nacionalidad, email, escolaridad, eliminado)
				VALUES (1, 'Pérez', 'Juan', 'DNI', ?, DATE '1990-01-01', 'M', 'Argentina', 'j@example.test', 'Universitario', FALSE)
				""", DOCUMENTO);
		Long candidatoId = jdbc.queryForObject("SELECT id FROM candidato WHERE nro_candidato = 1", Long.class);
		jdbc.update("""
				INSERT INTO cuestionario (evaluacion_id, candidato_id, clave_ingreso, estado, fecha_generacion,
				    fecha_cambio_estado, cantidad_accesos, cantidad_maxima_accesos, minutos_maximos_activo,
				    minutos_maximos_resolucion, preguntas_por_bloque)
				VALUES (1, ?, ?, 'EN_PROCESO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, 1440, 60, 2)
				""", candidatoId, clave, accesos, maximo);
	}

	private int accesos(String clave) {
		return jdbc.queryForObject("SELECT cantidad_accesos FROM cuestionario WHERE clave_ingreso = ?", Integer.class, clave);
	}

	private String estado(String clave) {
		return jdbc.queryForObject("SELECT estado FROM cuestionario WHERE clave_ingreso = ?", String.class, clave);
	}
}
