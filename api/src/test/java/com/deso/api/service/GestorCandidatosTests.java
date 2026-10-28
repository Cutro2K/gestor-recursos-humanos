package com.deso.api.service;

import com.deso.api.dao.DAOCandidato;
import com.deso.api.dto.PrevisualizacionCandidatosDTO.ErrorFila;
import com.deso.api.dto.PrevisualizacionCandidatosDTO.ResumenErrores;
import com.deso.api.exception.FormatoCsvException;
import com.deso.api.model.enums.TipoDocumento;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class GestorCandidatosTests {

	private static final String ENCABEZADO = "Nro candidato;Apellido;Nombre;Nro documento;Tipo documento;"
			+ "Fecha de nacimiento;Género;Nacionalidad;Email;Escolaridad;Eliminado";
	private static final String FILA = "1;Pérez;Ana;00123456;DNI;1996-02-29;M;Argentina;ana@example.test;Universitario;False";
	private static ValidatorFactory validatorFactory;
	private static GestorCandidatos gestor;

	@BeforeAll
	static void preparar() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		gestor = new GestorCandidatos(validatorFactory.getValidator(), mock(DAOCandidato.class));
	}

	@AfterAll
	static void cerrar() {
		validatorFactory.close();
	}

	@Test
	void convierteTodosLosCamposDelArchivoDeEjemplo() throws IOException {
		var resultado = gestor.parsearCSV(getClass().getResourceAsStream("/candidatos/valido.csv"));

		assertThat(resultado.errores()).isEmpty();
		assertThat(resultado.candidatos()).hasSize(2);
		var ana = resultado.candidatos().getFirst();
		assertThat(ana.fila()).isEqualTo(2);
		assertThat(ana.nroCandidato()).isEqualTo(1L);
		assertThat(ana.apellido()).isEqualTo("Pérez");
		assertThat(ana.nombre()).isEqualTo("Ana");
		assertThat(ana.nroDocumento()).isEqualTo("00123456");
		assertThat(ana.tipoDocumento()).isEqualTo(TipoDocumento.DNI);
		assertThat(ana.fechaNacimiento()).isEqualTo(LocalDate.of(1996, 2, 29));
		assertThat(ana.genero()).isEqualTo("M");
		assertThat(ana.nacionalidad()).isEqualTo("Argentina");
		assertThat(ana.email()).isEqualTo("ana@example.test");
		assertThat(ana.escolaridad()).isEqualTo("Universitario");
		assertThat(ana.eliminado()).isFalse();
		var luis = resultado.candidatos().getLast();
		assertThat(luis.nroDocumento()).isEqualTo("AB001234");
		assertThat(luis.tipoDocumento()).isEqualTo(TipoDocumento.PP);
		assertThat(luis.genero()).isEqualTo("H");
		assertThat(luis.eliminado()).isTrue();
	}

	@Test
	void admiteBomCrlfEspaciosYEncabezadosSinTildesNiMayusculas() throws IOException {
		String encabezado = ENCABEZADO.replace("Género", "Genero").toLowerCase(java.util.Locale.ROOT);
		var resultado = gestor.parsearCSV(archivo("\uFEFF" + encabezado + "\r\n"
				+ FILA.replace(";Ana;", "; Ana ;").replace(";DNI;", ";dni;").replace(";M;", ";m;")
				+ "\r\n\r\n"));

		assertThat(resultado.errores()).isEmpty();
		assertThat(resultado.candidatos()).singleElement().satisfies(c -> {
			assertThat(c.nombre()).isEqualTo("Ana");
			assertThat(c.tipoDocumento()).isEqualTo(TipoDocumento.DNI);
			assertThat(c.genero()).isEqualTo("M");
		});
	}

	@Test
	void usaLosNombresDelEncabezadoAunqueLasColumnasCambienDeOrden() throws IOException {
		List<String> encabezado = new ArrayList<>(List.of(ENCABEZADO.split(";")));
		List<String> fila = new ArrayList<>(List.of(FILA.split(";")));
		Collections.reverse(encabezado);
		Collections.reverse(fila);
		var resultado = gestor.parsearCSV(archivo(String.join(";", encabezado) + "\n" + String.join(";", fila)));

		assertThat(resultado.errores()).isEmpty();
		assertThat(resultado.candidatos().getFirst().nroCandidato()).isEqualTo(1L);
		assertThat(resultado.candidatos().getFirst().eliminado()).isFalse();
	}

	@Test
	void interpretaPuntoYComaComillasEscapadasYSaltosDeLineaEnCamposEntrecomillados() throws IOException {
		String fila = FILA.replace("Pérez", "\"Pérez; \"\"López\"\"\"")
				.replace("Universitario", "\"Universitario\ncompleto\"");
		var resultado = gestor.parsearCSV(archivo(ENCABEZADO + "\n" + fila + "\n\n"
				+ FILA.replace("1996-02-29", "1995-02-29")));

		assertThat(resultado.candidatos()).singleElement().satisfies(c -> {
			assertThat(c.apellido()).isEqualTo("Pérez; \"López\"");
			assertThat(c.escolaridad()).isEqualTo("Universitario\ncompleto");
		});
		assertThat(resultado.errores()).singleElement().satisfies(e -> {
			assertThat(e.fila()).isEqualTo(5);
			assertThat(e.campo()).isEqualTo("Fecha de nacimiento");
		});
	}

	@ParameterizedTest
	@CsvSource({
			"0,abc,Nro candidato", "0,0,Nro candidato", "0,-1,Nro candidato",
			"0,9223372036854775808,Nro candidato", "0,1.5,Nro candidato",
			"4,CI,Tipo documento", "5,1995-02-29,Fecha de nacimiento",
			"5,1996-13-01,Fecha de nacimiento", "5,1996-2-29,Fecha de nacimiento",
			"5,29/02/1996,Fecha de nacimiento", "6,X,Género",
			"8,sin-arroba,Email", "8,ana@@example.test,Email",
			"10,1,Eliminado", "10,si,Eliminado"
	})
	void reportaCampoInvalidoSinDescartarLasDemasFilas(int columna, String valor, String campo) throws IOException {
		var resultado = gestor.parsearCSV(archivo(ENCABEZADO + "\n" + reemplazar(columna, valor) + "\n" + FILA));

		assertThat(resultado.candidatos()).singleElement().satisfies(c -> assertThat(c.fila()).isEqualTo(3));
		assertThat(resultado.errores()).singleElement().satisfies(e -> {
			assertThat(e.fila()).isEqualTo(2);
			assertThat(e.campo()).isEqualTo(campo);
			assertThat(e.mensaje()).isNotBlank();
		});
	}

	@Test
	void reportaTodosLosCamposObligatoriosInclusoElUltimoVacio() throws IOException {
		var resultado = gestor.parsearCSV(archivo(ENCABEZADO + "\n;;;;;;;;;;"));

		assertThat(resultado.candidatos()).isEmpty();
		assertThat(resultado.errores()).hasSize(11)
				.allSatisfy(e -> assertThat(e.mensaje()).isEqualTo("El campo es obligatorio."));
		assertThat(resultado.errores()).extracting(ErrorFila::campo)
				.containsExactly(ENCABEZADO.split(";"));
	}

	@ParameterizedTest
	@CsvSource({"1,51", "2,51", "3,21", "7,51", "8,256", "9,51"})
	void validaLosLimitesDeTextoDelModelo(int columna, int longitud) throws IOException {
		String valor = columna == 8 ? "a".repeat(longitud - 13) + "@example.test" : "a".repeat(longitud);
		var resultado = gestor.parsearCSV(archivo(ENCABEZADO + "\n" + reemplazar(columna, valor)));

		assertThat(resultado.candidatos()).isEmpty();
		assertThat(resultado.errores()).anySatisfy(e -> {
			assertThat(e.campo()).isEqualTo(ENCABEZADO.split(";")[columna]);
			assertThat(e.mensaje()).contains("como máximo");
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {"DNI", "LE", "LC", "PP"})
	void aceptaLosCuatroTiposDeDocumento(String tipo) throws IOException {
		var resultado = gestor.parsearCSV(archivo(ENCABEZADO + "\n" + reemplazar(4, tipo)));
		assertThat(resultado.errores()).isEmpty();
		assertThat(resultado.candidatos().getFirst().tipoDocumento()).isEqualTo(TipoDocumento.valueOf(tipo));
	}

	@ParameterizedTest
	@ValueSource(strings = {"True", "False", "true", "FALSE"})
	void convierteSoloBooleanosExplicitos(String valor) throws IOException {
		var resultado = gestor.parsearCSV(archivo(ENCABEZADO + "\n" + reemplazar(10, valor)));
		assertThat(resultado.errores()).isEmpty();
		assertThat(resultado.candidatos().getFirst().eliminado()).isEqualTo(Boolean.valueOf(valor));
	}

	@Test
	void reportaColumnasDeMasYDeMenosYContinua() throws IOException {
		var resultado = gestor.parsearCSV(archivo(ENCABEZADO + "\n1;Pérez\n" + FILA + ";extra\n" + FILA));
		assertThat(resultado.candidatos()).hasSize(1);
		assertThat(resultado.errores()).extracting(ErrorFila::fila).containsExactly(2L, 3L);
		assertThat(resultado.errores()).extracting(ErrorFila::campo).containsOnly("registro");
	}

	@Test
	void rechazaEncabezadosIncompletosDuplicadosDesconocidosYSeparadorIncorrecto() {
		List<String> encabezados = List.of(
				ENCABEZADO.replace(";Eliminado", ""),
				ENCABEZADO.replace("Eliminado", "Apellido"),
				ENCABEZADO.replace("Eliminado", "Otra columna"),
				ENCABEZADO + ";extra", ENCABEZADO.replace(';', ','));
		for (String encabezado : encabezados) {
			assertThatThrownBy(() -> gestor.parsearCSV(archivo(encabezado + "\n" + FILA)))
					.isInstanceOf(FormatoCsvException.class).hasMessageContaining("encabezado");
		}
	}

	@Test
	void rechazaArchivoVacioYSinEncabezado() {
		for (String contenido : List.of("", "\uFEFF", FILA)) {
			assertThatThrownBy(() -> gestor.parsearCSV(archivo(contenido)))
					.isInstanceOf(FormatoCsvException.class);
		}
	}

	@Test
	void admiteEncabezadoSinRegistros() throws IOException {
		var resultado = gestor.parsearCSV(archivo(ENCABEZADO));
		assertThat(resultado.candidatos()).isEmpty();
		assertThat(resultado.errores()).isEmpty();
	}

	@Test
	void rechazaCsvConComillasSinCerrarSinDevolverResultadosParciales() {
		assertThatThrownBy(() -> gestor.parsearCSV(archivo(ENCABEZADO + "\n" + FILA + "\n2;\"sin cerrar")))
				.isInstanceOf(FormatoCsvException.class).hasMessageContaining("CSV");
	}

	@Test
	void rechazaUtf8Invalido() {
		byte[] encabezado = (ENCABEZADO + "\n").getBytes(StandardCharsets.UTF_8);
		byte[] contenido = Arrays.copyOf(encabezado, encabezado.length + 1);
		contenido[encabezado.length] = (byte) 0xFF;
		assertThatThrownBy(() -> gestor.parsearCSV(new ByteArrayInputStream(contenido)))
				.isInstanceOfAny(IOException.class, FormatoCsvException.class);
	}

	@Test
	void conservaLosValoresDeLasFilasInvalidasParaLaPrevisualizacion() throws IOException {
		String invalida = reemplazar(5, "fecha incorrecta").replace(";Ana;", "; Ana ;");
		var resultado = gestor.parsearCSV(archivo(ENCABEZADO + "\n" + invalida + "\n" + FILA));

		assertThat(resultado.filas()).hasSize(2);
		var filaInvalida = resultado.filas().getFirst();
		assertThat(filaInvalida.fila()).isEqualTo(2);
		assertThat(filaInvalida.valores()).containsEntry("Fecha de nacimiento", "fecha incorrecta")
				.containsEntry("Nombre", " Ana ");
		assertThat(filaInvalida.errores()).extracting(ErrorFila::campo)
				.containsExactly("Fecha de nacimiento");
		assertThat(resultado.filas().getLast().errores()).isEmpty();
		assertThat(resultado.candidatos()).hasSize(1);
	}

	@Test
	void conservaColumnasConocidasDeUnaFilaIncompletaYLaIncluyeEnElResumen() throws IOException {
		var resultado = gestor.parsearCSV(archivo(ENCABEZADO + "\n1;Pérez\n" + FILA));

		assertThat(resultado.filas().getFirst().valores()).containsEntry("Apellido", "Pérez")
				.containsEntry("Eliminado", "");
		var resumen = ResumenErrores.desdeErrores(resultado.errores());

		assertThat(resumen.filasConErrores()).isEqualTo(1);
		assertThat(resumen.filasPorCampo()).containsOnlyKeys("registro");
	}

	@Test
	void cuentaFilasConProblemasPorCampoSinSumarDosVecesUnaMismaFila() throws IOException {
		String dosProblemas = reemplazar(5, "fecha incorrecta").replace("ana@example.test", "email incorrecto");
		var resultado = gestor.parsearCSV(archivo(ENCABEZADO + "\n" + dosProblemas
				+ "\n" + reemplazar(8, "otro email incorrecto") + "\n" + FILA));

		var resumen = ResumenErrores.desdeErrores(resultado.errores());

		assertThat(resumen.filasConErrores()).isEqualTo(2);
		assertThat(resumen.filasPorCampo()).containsEntry("Email", 2L)
				.containsEntry("Fecha de nacimiento", 1L).hasSize(2);

		var conErroresRepetidos = ResumenErrores.desdeErrores(List.of(
				new ErrorFila(2, "Email", "Error de formato"),
				new ErrorFila(2, "Email", "Error de longitud")));

		assertThat(conErroresRepetidos.filasPorCampo()).containsEntry("Email", 1L);
	}

	private String reemplazar(int columna, String valor) {
		String[] valores = FILA.split(";", -1);
		valores[columna] = valor;
		return String.join(";", valores);
	}

	private ByteArrayInputStream archivo(String contenido) {
		return new ByteArrayInputStream(contenido.getBytes(StandardCharsets.UTF_8));
	}
}
