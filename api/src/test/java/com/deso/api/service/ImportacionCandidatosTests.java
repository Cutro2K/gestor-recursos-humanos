package com.deso.api.service;

import com.deso.api.dao.CandidatoDAO;
import com.deso.api.model.entity.Candidato;
import com.deso.api.dto.PrevisualizacionCandidatosDTO.Accion;
import com.deso.api.model.enums.TipoDocumento;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ImportacionCandidatosTests {

	private static final String ENCABEZADO = "Nro candidato;Apellido;Nombre;Nro documento;Tipo documento;"
			+ "Fecha de nacimiento;Género;Nacionalidad;Email;Escolaridad;Eliminado\n";
	private static ValidatorFactory validatorFactory;
	private CandidatoDAO dao;
	private GestorCandidatos gestor;

	@BeforeAll
	static void abrirValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
	}

	@AfterAll
	static void cerrarValidator() {
		validatorFactory.close();
	}

	@BeforeEach
	void preparar() {
		dao = mock(CandidatoDAO.class);
		gestor = new GestorCandidatos(validatorFactory.getValidator(), dao);
	}

	@Test
	void previsualizaLasTresOperacionesYLosErroresSinModificarNiGuardarEntidades() throws IOException {
		Candidato actualizado = existente(2L);
		Candidato eliminado = existente(3L);
		when(dao.buscarPorNumeroCandidato(2L)).thenReturn(Optional.of(actualizado));
		when(dao.buscarPorNumeroCandidato(3L)).thenReturn(Optional.of(eliminado));

		var previa = gestor.previsualizarCSV(archivo(fila(1, false), fila(2, false), fila(3, true),
				fila(4, false).replace("1996-02-29", "fecha incorrecta")));

		assertThat(previa.filas()).extracting(f -> f.accion()).containsExactly(
				Accion.CREAR, Accion.ACTUALIZAR, Accion.ELIMINAR, Accion.RECHAZAR);
		assertThat(previa.aCrear()).isEqualTo(1);
		assertThat(previa.aActualizar()).isEqualTo(1);
		assertThat(previa.aEliminar()).isEqualTo(1);
		assertThat(previa.rechazadas()).isEqualTo(1);
		assertThat(previa.resumenErrores().filasPorCampo()).containsEntry("Fecha de nacimiento", 1L);
		assertThat(previa.filas().getLast().valores()).containsEntry("Fecha de nacimiento", "fecha incorrecta");
		assertThat(actualizado.getNombre()).isEqualTo("Nombre original");
		assertThat(eliminado.getEliminado()).isFalse();
		verify(dao, never()).guardarLote(anyList());
	}

	@Test
	void confirmaSoloLasFilasValidasConBajaLogicaYCuentaLasRechazadas() throws IOException {
		Candidato actualizado = existente(2L);
		Candidato eliminado = existente(3L);
		when(dao.buscarPorNumeroCandidato(2L)).thenReturn(Optional.of(actualizado));
		when(dao.buscarPorNumeroCandidato(3L)).thenReturn(Optional.of(eliminado));

		var resultado = gestor.importarCSV(archivo(fila(1, false), fila(2, false), fila(3, true),
				fila(4, false).replace("ana@example.test", "correo incorrecto")));

		assertThat(resultado.creados()).isEqualTo(1);
		assertThat(resultado.actualizados()).isEqualTo(1);
		assertThat(resultado.eliminados()).isEqualTo(1);
		assertThat(resultado.rechazados()).isEqualTo(1);
		assertThat(resultado.errores()).singleElement().satisfies(error -> {
			assertThat(error.fila()).isEqualTo(5);
			assertThat(error.campo()).isEqualTo("Email");
		});
		ArgumentCaptor<List<Candidato>> lote = ArgumentCaptor.captor();
		verify(dao).guardarLote(lote.capture());
		assertThat(lote.getValue()).hasSize(3);
		assertThat(lote.getValue().getFirst().getId()).isNull();
		assertThat(lote.getValue().get(1).getId()).isEqualTo(actualizado.getId());
		assertThat(lote.getValue().get(1).getNombre()).isEqualTo("Ana");
		assertThat(lote.getValue().getLast().getEliminado()).isTrue();
		assertThat(lote.getValue().getLast().getNombre()).isEqualTo("Nombre original");
		assertThat(lote.getValue().getLast().getNroDocumento()).isEqualTo(eliminado.getNroDocumento());
		assertThat(actualizado.getNombre()).isEqualTo("Nombre original");
		assertThat(eliminado.getEliminado()).isFalse();
	}

	@Test
	void consultaOtraVezLaBaseAlConfirmarAunqueLaPrevisualizacionIndicaraUnAlta() throws IOException {
		Candidato creadoPorOtro = existente(1L);
		when(dao.buscarPorNumeroCandidato(1L)).thenReturn(Optional.empty(), Optional.of(creadoPorOtro));
		assertThat(gestor.previsualizarCSV(archivo(fila(1, false))).aCrear()).isEqualTo(1);

		var resultado = gestor.importarCSV(archivo(fila(1, false)));
		assertThat(resultado.creados()).isZero();
		assertThat(resultado.actualizados()).isEqualTo(1);
		ArgumentCaptor<List<Candidato>> lote = ArgumentCaptor.captor();
		verify(dao).guardarLote(lote.capture());
		assertThat(lote.getValue().getFirst().getId()).isEqualTo(creadoPorOtro.getId());
	}

	@Test
	void rechazaTodasLasAparicionesDeUnNumeroDuplicado() throws IOException {
		var resultado = gestor.importarCSV(archivo(fila(1, false), fila(1, true)));
		assertThat(resultado.rechazados()).isEqualTo(2);
		assertThat(resultado.resumenErrores().filasPorCampo()).containsEntry("Nro candidato", 2L);
		verifyNoInteractions(dao);
	}

	@Test
	void rechazaTodasLasAparicionesDeUnDocumentoDuplicadoEnAltasOActualizaciones() throws IOException {
		var previa = gestor.previsualizarCSV(archivo(fila(1, false), fila(2, false).replace("DOC2", "DOC1")));
		assertThat(previa.rechazadas()).isEqualTo(2);
		assertThat(previa.resumenErrores().filasPorCampo()).containsEntry("Nro documento", 2L);
		verifyNoInteractions(dao);
	}

	@Test
	void rechazaUnDocumentoAsignadoAOtroNumeroSinUsarloParaIdentificarAlCandidato() throws IOException {
		when(dao.buscarPorDocumento(TipoDocumento.DNI, "DOC1")).thenReturn(Optional.of(existente(99L)));
		var resultado = gestor.importarCSV(archivo(fila(1, false)));
		assertThat(resultado.rechazados()).isEqualTo(1);
		assertThat(resultado.creados()).isZero();
		assertThat(resultado.errores().getFirst().mensaje()).contains("otro número de candidato");
		verify(dao, never()).guardarLote(anyList());
	}

	@Test
	void rechazaUnaBajaDeUnNumeroInexistente() throws IOException {
		var resultado = gestor.importarCSV(archivo(fila(1, true)));
		assertThat(resultado.eliminados()).isZero();
		assertThat(resultado.rechazados()).isEqualTo(1);
		assertThat(resultado.errores().getFirst().mensaje()).contains("No existe");
		verify(dao, never()).guardarLote(anyList());
	}

	@Test
	void actualizaYReactivaUnCandidatoCuandoElCsvIndicaFalse() throws IOException {
		Candidato eliminado = existente(1L);
		eliminado.setEliminado(true);
		when(dao.buscarPorNumeroCandidato(1L)).thenReturn(Optional.of(eliminado));
		var resultado = gestor.importarCSV(archivo(fila(1, false)));
		assertThat(resultado.actualizados()).isEqualTo(1);
		ArgumentCaptor<List<Candidato>> lote = ArgumentCaptor.captor();
		verify(dao).guardarLote(lote.capture());
		assertThat(lote.getValue().getFirst().getEliminado()).isFalse();
	}

	@Test
	void unArchivoSinFilasDevuelveCeroOperaciones() throws IOException {
		var resultado = gestor.importarCSV(archivo());
		assertThat(resultado.creados() + resultado.actualizados() + resultado.eliminados() + resultado.rechazados()).isZero();
		verifyNoInteractions(dao);
	}

	private String fila(long numero, boolean eliminado) {
		return numero + ";Pérez;Ana;DOC" + numero + ";DNI;1996-02-29;M;Argentina;ana@example.test;Universitario;" + eliminado;
	}

	private ByteArrayInputStream archivo(String... filas) {
		return new ByteArrayInputStream((ENCABEZADO + String.join("\n", filas)).getBytes(StandardCharsets.UTF_8));
	}

	private Candidato existente(long numero) {
		return new Candidato(numero + 100, numero, "Apellido original", "Nombre original", TipoDocumento.DNI,
				"ORIGINAL" + numero, LocalDate.of(1990, 1, 1), "H", "Argentina", "anterior@example.test", "Secundario", false);
	}
}
