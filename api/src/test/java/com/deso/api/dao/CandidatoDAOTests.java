package com.deso.api.dao;

import com.deso.api.model.entity.Candidato;
import com.deso.api.model.enums.TipoDocumento;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false, properties = {
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"logging.file.name=target/candidato-dao-tests.log"
})
@Import({CandidatoDAO.class, PersistenceExceptionTranslationPostProcessor.class})
class CandidatoDAOTests {

	@Autowired
	private CandidatoDAO candidatoDAO;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void springReutilizaLaMismaInstanciaDelDAO() {
		String[] nombres = applicationContext.getBeanNamesForType(CandidatoDAO.class);
		assertThat(nombres).hasSize(1);
		assertThat(applicationContext.isSingleton(nombres[0])).isTrue();
		assertThat(applicationContext.getBean(nombres[0])).isSameAs(candidatoDAO);
	}

	@Test
	void buscaPorTipoYNumeroDeDocumento() {
		Candidato dni = candidato(1L, "12345678", "ana@example.test");
		Candidato pasaporte = candidato(2L, "12345678", "luis@example.test");
		pasaporte.setTipoDocumento(TipoDocumento.PP);
		candidatoDAO.guardarLote(List.of(dni, pasaporte));
		entityManager.clear();

		assertThat(candidatoDAO.buscarPorDocumento(TipoDocumento.DNI, "12345678"))
				.get().extracting(Candidato::getId).isEqualTo(dni.getId());
		assertThat(candidatoDAO.buscarPorDocumento(TipoDocumento.PP, "12345678"))
				.get().extracting(Candidato::getId).isEqualTo(pasaporte.getId());
	}

	@Test
	void devuelveVacioCuandoNoExisteElDocumento() {
		assertThat(candidatoDAO.buscarPorDocumento(TipoDocumento.DNI, "99999999")).isEmpty();
	}

	@Test
	void buscaPorNumeroDeCandidatoIncluyendoBajas() {
		Candidato eliminado = candidato(25L, "12345678", "ana@example.test");
		eliminado.setEliminado(true);
		candidatoDAO.guardarLote(List.of(eliminado));
		entityManager.clear();
		assertThat(candidatoDAO.buscarPorNumeroCandidato(25L)).get()
				.satisfies(c -> assertThat(c.getEliminado()).isTrue());
		assertThat(candidatoDAO.buscarPorNumeroCandidato(99L)).isEmpty();
	}

	@Test
	void buscaTodosLosEmailsCoincidentesSinDistinguirMayusculas() {
		Candidato primero = candidato(1L, "12345678", "Compartido@Example.test");
		Candidato segundo = candidato(2L, "23456789", "compartido@example.test");
		Candidato otro = candidato(3L, "34567890", "otro@example.test");
		candidatoDAO.guardarLote(List.of(primero, segundo, otro));
		entityManager.clear();

		assertThat(candidatoDAO.buscarPorEmail("COMPARTIDO@EXAMPLE.TEST"))
				.extracting(Candidato::getId).containsExactly(primero.getId(), segundo.getId());
		assertThat(candidatoDAO.buscarPorEmail("inexistente@example.test")).isEmpty();
	}

	@Test
	void encuentraLasIdentidadesDadasDeBajaSinReactivarlas() {
		Candidato eliminado = candidato(1L, "12345678", "ana@example.test");
		eliminado.setEliminado(true);
		candidatoDAO.guardarLote(List.of(eliminado));
		entityManager.clear();

		assertThat(candidatoDAO.buscarPorDocumento(TipoDocumento.DNI, "12345678"))
				.get().extracting(Candidato::getEliminado).isEqualTo(true);
		assertThat(candidatoDAO.buscarPorEmail("ana@example.test"))
				.extracting(Candidato::getEliminado).containsExactly(true);
	}

	@Test
	void persisteUnLoteDeCandidatosNuevos() {
		List<Candidato> guardados = candidatoDAO.guardarLote(List.of(
				candidato(1L, "12345678", "ana@example.test"),
				candidato(2L, "23456789", "luis@example.test")));
		entityManager.clear();

		assertThat(guardados).hasSize(2).allSatisfy(c -> assertThat(c.getId()).isNotNull());
		assertThat(entityManager.find(Candidato.class, guardados.getFirst().getId()).getNroCandidato())
				.isEqualTo(1L);
		assertThat(entityManager.find(Candidato.class, guardados.getLast().getId()).getNroCandidato())
				.isEqualTo(2L);
	}

	@Test
	void actualizaUnaEntidadSeparadaDelContextoJuntoConUnaNueva() {
		Candidato existente = candidato(1L, "12345678", "anterior@example.test");
		candidatoDAO.guardarLote(List.of(existente));
		Long idOriginal = existente.getId();
		entityManager.clear();
		existente.setEmail("nuevo@example.test");

		List<Candidato> guardados = candidatoDAO.guardarLote(List.of(
				existente, candidato(2L, "23456789", "luis@example.test")));
		entityManager.clear();

		assertThat(guardados.getFirst()).isNotSameAs(existente);
		assertThat(guardados.getFirst().getId()).isEqualTo(idOriginal);
		assertThat(entityManager.find(Candidato.class, idOriginal).getEmail()).isEqualTo("nuevo@example.test");
		assertThat(entityManager.createQuery("select count(c) from Candidato c", Long.class).getSingleResult())
				.isEqualTo(2L);
	}

	@Test
	void aceptaUnLoteVacio() {
		assertThat(candidatoDAO.guardarLote(List.of())).isEmpty();
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void revierteTodoElLoteSiUnDocumentoEstaDuplicado() {
		Candidato primero = candidato(100L, "99999999", "uno@example.test");
		Candidato duplicado = candidato(101L, "99999999", "dos@example.test");

		assertThatThrownBy(() -> candidatoDAO.guardarLote(List.of(primero, duplicado)))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThat(candidatoDAO.buscarPorDocumento(TipoDocumento.DNI, "99999999")).isEmpty();
		assertThat(candidatoDAO.buscarPorEmail("uno@example.test")).isEmpty();
		assertThat(candidatoDAO.buscarPorEmail("dos@example.test")).isEmpty();
	}

	private Candidato candidato(Long numero, String documento, String email) {
		Candidato candidato = new Candidato();
		candidato.setNroCandidato(numero);
		candidato.setApellido("Pérez");
		candidato.setNombre("Ana");
		candidato.setTipoDocumento(TipoDocumento.DNI);
		candidato.setNroDocumento(documento);
		candidato.setFechaNacimiento(LocalDate.of(1995, 5, 20));
		candidato.setGenero("M");
		candidato.setNacionalidad("Argentina");
		candidato.setEmail(email);
		candidato.setEscolaridad("Universitario");
		return candidato;
	}
}
