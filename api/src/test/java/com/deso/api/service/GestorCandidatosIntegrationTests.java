package com.deso.api.service;

import com.deso.api.dao.CandidatoDAO;
import com.deso.api.model.entity.Candidato;
import com.deso.api.model.enums.TipoDocumento;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:cu06-service-tests",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa", "spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
		"logging.file.name=target/cu06-service-tests.log"
})
@Transactional
class GestorCandidatosIntegrationTests {

	@Autowired
	private GestorCandidatos gestor;

	@Autowired
	private CandidatoDAO dao;

	@Autowired
	private EntityManager entityManager;

	@BeforeEach
	void prepararBase() {
		dao.guardarLote(List.of(existente(2L), existente(3L), existente(99L)));
		entityManager.clear();
	}

	@Test
	void previsualizarNoModificaLaBaseAunqueSeFuerceElFlush() throws IOException {
		var previa = gestor.previsualizarCSV(archivo());
		entityManager.flush();
		entityManager.clear();

		assertThat(previa.aCrear()).isEqualTo(1);
		assertThat(previa.aActualizar()).isEqualTo(1);
		assertThat(previa.aEliminar()).isEqualTo(1);
		assertThat(previa.rechazadas()).isEqualTo(1);
		assertThat(dao.buscarPorNumeroCandidato(1L)).isEmpty();
		assertThat(dao.buscarPorNumeroCandidato(2L)).get()
				.satisfies(c -> assertThat(c.getNombre()).isEqualTo("Original"));
		assertThat(dao.buscarPorNumeroCandidato(3L)).get()
				.satisfies(c -> assertThat(c.getEliminado()).isFalse());
	}

	@Test
	void confirmarPersisteLasFilasValidasYConservaLosCandidatosAusentesDelArchivo() throws IOException {
		Long idActualizado = dao.buscarPorNumeroCandidato(2L).orElseThrow().getId();
		var resultado = gestor.importarCSV(archivo());
		entityManager.clear();

		assertThat(resultado.creados()).isEqualTo(1);
		assertThat(resultado.actualizados()).isEqualTo(1);
		assertThat(resultado.eliminados()).isEqualTo(1);
		assertThat(resultado.rechazados()).isEqualTo(1);
		assertThat(dao.buscarPorNumeroCandidato(1L)).isPresent();
		assertThat(dao.buscarPorNumeroCandidato(2L)).get().satisfies(c -> {
			assertThat(c.getId()).isEqualTo(idActualizado);
			assertThat(c.getNombre()).isEqualTo("Ana");
		});
		assertThat(dao.buscarPorNumeroCandidato(3L)).get().satisfies(c -> {
			assertThat(c.getEliminado()).isTrue();
			assertThat(c.getNombre()).isEqualTo("Original");
		});
		assertThat(dao.buscarPorNumeroCandidato(4L)).isEmpty();
		assertThat(dao.buscarPorNumeroCandidato(99L)).get().satisfies(c -> {
			assertThat(c.getEliminado()).isFalse();
			assertThat(c.getNombre()).isEqualTo("Original");
		});
	}

	private Candidato existente(long numero) {
		return new Candidato(null, numero, "Pérez", "Original", TipoDocumento.DNI, "DOC" + numero,
				LocalDate.of(1990, 1, 1), "M", "Argentina", "original@example.test", "Universitario", false);
	}

	private ByteArrayInputStream archivo() {
		String csv = """
				Nro candidato;Apellido;Nombre;Nro documento;Tipo documento;Fecha de nacimiento;Género;Nacionalidad;Email;Escolaridad;Eliminado
				1;Pérez;Ana;DOC1;DNI;1996-02-29;M;Argentina;ana@example.test;Universitario;False
				2;Pérez;Ana;DOC2;DNI;1996-02-29;M;Argentina;ana@example.test;Universitario;False
				3;Pérez;Ana;DOC3;DNI;1996-02-29;M;Argentina;ana@example.test;Universitario;True
				4;Pérez;Ana;DOC4;DNI;fecha incorrecta;M;Argentina;ana@example.test;Universitario;False
				""";
		return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
	}
}
