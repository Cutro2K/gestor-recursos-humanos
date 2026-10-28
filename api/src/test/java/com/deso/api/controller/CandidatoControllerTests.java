package com.deso.api.controller;

import com.deso.api.dao.CandidatoDAO;
import com.deso.api.model.entity.Candidato;
import com.deso.api.model.enums.TipoDocumento;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.datasource.url=jdbc:h2:mem:cu06-controller-tests",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa", "spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
		"logging.file.name=target/cu06-controller-tests.log"
})
class CandidatoControllerTests {

	private static final String ENCABEZADO = "Nro candidato;Apellido;Nombre;Nro documento;Tipo documento;"
			+ "Fecha de nacimiento;Género;Nacionalidad;Email;Escolaridad;Eliminado\n";

	private static final HttpClient CLIENTE = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1)
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	@LocalServerPort
	private int puerto;

	@Autowired
	private CandidatoDAO dao;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private JsonMapper json;

	@BeforeEach
	@AfterEach
	void limpiarBase() {
		jdbc.update("delete from candidato");
	}

	@Test
	void previsualizarDevuelveAccionesYResumenSinPersistir() throws Exception {
		dao.guardarLote(List.of(candidato(2L), candidato(3L)));
		String csv = ENCABEZADO + fila(1, false) + "\n" + fila(2, false) + "\n" + fila(3, true)
				+ "\n" + fila(4, false).replace("1996-02-29", "fecha incorrecta");

		var respuesta = enviarArchivo("/previsualizar", "archivo", "candidatos.csv", bytes(csv));
		JsonNode cuerpo = cuerpoExitoso(respuesta);

		assertThat(cuerpo.path("aCrear").asInt()).isEqualTo(1);
		assertThat(cuerpo.path("aActualizar").asInt()).isEqualTo(1);
		assertThat(cuerpo.path("aEliminar").asInt()).isEqualTo(1);
		assertThat(cuerpo.path("rechazadas").asInt()).isEqualTo(1);
		assertThat(cuerpo.path("filas").get(0).path("accion").asText()).isEqualTo("CREAR");
		assertThat(cuerpo.path("filas").get(1).path("accion").asText()).isEqualTo("ACTUALIZAR");
		assertThat(cuerpo.path("filas").get(2).path("accion").asText()).isEqualTo("ELIMINAR");
		assertThat(cuerpo.path("filas").get(3).path("accion").asText()).isEqualTo("RECHAZAR");
		assertThat(cuerpo.path("resumenErrores").path("filasPorCampo").path("Fecha de nacimiento").asInt())
				.isEqualTo(1);
		assertThat(dao.buscarPorNumeroCandidato(1L)).isEmpty();
		assertThat(dao.buscarPorNumeroCandidato(2L).orElseThrow().getNombre()).isEqualTo("Original");
		assertThat(dao.buscarPorNumeroCandidato(3L).orElseThrow().getEliminado()).isFalse();
	}

	@Test
	void importarGuardaSoloLasFilasValidasYLuegoSePuedenListar() throws Exception {
		dao.guardarLote(List.of(candidato(2L), candidato(3L), candidato(99L)));
		String csv = ENCABEZADO + fila(1, false) + "\n" + fila(2, false) + "\n" + fila(3, true)
				+ "\n" + fila(4, false).replace("ana@example.test", "email incorrecto");

		var respuesta = enviarArchivo("/importar", "archivo", "candidatos.csv", bytes(csv));
		JsonNode cuerpo = cuerpoExitoso(respuesta);

		assertThat(cuerpo.path("creados").asInt()).isEqualTo(1);
		assertThat(cuerpo.path("actualizados").asInt()).isEqualTo(1);
		assertThat(cuerpo.path("eliminados").asInt()).isEqualTo(1);
		assertThat(cuerpo.path("rechazados").asInt()).isEqualTo(1);
		assertThat(cuerpo.path("errores").get(0).path("campo").asText()).isEqualTo("Email");
		assertThat(dao.buscarPorNumeroCandidato(2L).orElseThrow().getNombre()).isEqualTo("Ana");
		assertThat(dao.buscarPorNumeroCandidato(3L).orElseThrow().getEliminado()).isTrue();
		assertThat(dao.buscarPorNumeroCandidato(4L)).isEmpty();
		assertThat(dao.buscarPorNumeroCandidato(99L).orElseThrow().getNombre()).isEqualTo("Original");

		JsonNode listado = cuerpoExitoso(get(""));
		assertThat(listado.path("candidatos").size()).isEqualTo(3);
		assertThat(listado.path("siguienteCursor").isNull()).isTrue();
	}

	@Test
	void permiteReenviarElCsvSinCrearCandidatosDuplicados() throws Exception {
		byte[] csv = bytes(ENCABEZADO + fila(1, false));

		assertThat(cuerpoExitoso(enviarArchivo("/importar", "archivo", "candidatos.CSV", csv))
				.path("creados").asInt()).isEqualTo(1);
		assertThat(cuerpoExitoso(enviarArchivo("/importar", "archivo", "candidatos.CSV", csv))
				.path("actualizados").asInt()).isEqualTo(1);
		assertThat(jdbc.queryForObject("select count(*) from candidato", Long.class)).isEqualTo(1L);
	}

	@Test
	void unArchivoConTodasLasFilasRechazadasDevuelveElReporteSinGuardar() throws Exception {
		byte[] csv = bytes(ENCABEZADO + fila(1, false).replace("1996-02-29", "fecha incorrecta"));
		JsonNode cuerpo = cuerpoExitoso(enviarArchivo("/importar", "archivo", "candidatos.csv", csv));

		assertThat(cuerpo.path("creados").asInt()).isZero();
		assertThat(cuerpo.path("rechazados").asInt()).isEqualTo(1);
		assertThat(jdbc.queryForObject("select count(*) from candidato", Long.class)).isZero();
	}

	@Test
	void paginaLosActivosPorIdSinRepetirFilasYOmiteLasBajas() throws Exception {
		Candidato eliminado = candidato(40L);
		eliminado.setEliminado(true);
		dao.guardarLote(List.of(candidato(20L), eliminado, candidato(10L), candidato(30L)));

		JsonNode primera = cuerpoExitoso(get("?limite=2"));
		assertThat(primera.path("candidatos").size()).isEqualTo(2);
		assertThat(primera.path("candidatos").get(0).path("nroCandidato").asLong()).isEqualTo(20);
		assertThat(primera.path("candidatos").get(1).path("nroCandidato").asLong()).isEqualTo(10);
		long cursor = primera.path("siguienteCursor").asLong();

		JsonNode segunda = cuerpoExitoso(get("?limite=2&cursor=" + cursor));
		assertThat(segunda.path("candidatos").size()).isEqualTo(1);
		assertThat(segunda.path("candidatos").get(0).path("nroCandidato").asLong()).isEqualTo(30);
		assertThat(segunda.path("siguienteCursor").isNull()).isTrue();
		assertThat(segunda.path("candidatos").get(0).path("id").asLong()).isGreaterThan(cursor);
	}

	@Test
	void aplicaLosFiltrosCombinadosYSinDistinguirMayusculas() throws Exception {
		Candidato ana = candidato(1L);
		ana.setNombre("Ana");
		dao.guardarLote(List.of(ana, candidato(2L)));

		JsonNode resultado = cuerpoExitoso(get("?apellido=p%C3%89r&nombre=ANA&nroCandidato=1"));

		assertThat(resultado.path("candidatos").size()).isEqualTo(1);
		JsonNode candidato = resultado.path("candidatos").get(0);
		assertThat(candidato.path("nroCandidato").asLong()).isEqualTo(1);
		assertThat(candidato.path("tipoDocumento").asText()).isEqualTo("DNI");
		assertThat(candidato.path("nroDocumento").asText()).isEqualTo("DOC1");
		assertThat(candidato.path("email").asText()).isEqualTo("original@example.test");
	}

	@Test
	void losFiltrosTratanPorcentajeGuionBajoYEscapeComoTextoLiteral() throws Exception {
		Candidato literal = candidato(1L);
		literal.setApellido("Pérez_100%!");
		dao.guardarLote(List.of(literal, candidato(2L)));

		assertThat(cuerpoExitoso(get("?apellido=_100%25!")).path("candidatos").size()).isEqualTo(1);
	}

	@Test
	void unListadoVacioOUnCursorSinResultadosDevuelveUnaPaginaVacia() throws Exception {
		JsonNode vacio = cuerpoExitoso(get(""));
		assertThat(vacio.path("candidatos").isArray()).isTrue();
		assertThat(vacio.path("candidatos").size()).isZero();
		assertThat(vacio.path("siguienteCursor").isNull()).isTrue();

		dao.guardarLote(List.of(candidato(1L)));
		assertThat(cuerpoExitoso(get("?cursor=9223372036854775807")).path("candidatos").size()).isZero();
	}

	@Test
	void elListadoUsaUnaPaginaDeCincuentaConUnMaximoDeCien() throws Exception {
		for (long numero = 1; numero <= 101; numero++) {
			dao.guardarLote(List.of(candidato(numero)));
		}

		assertThat(cuerpoExitoso(get("")).path("candidatos").size()).isEqualTo(50);
		assertThat(cuerpoExitoso(get("?limite=100")).path("candidatos").size()).isEqualTo(100);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"limite=0", "limite=101", "limite=-1", "limite=abc",
			"cursor=0", "cursor=-1", "cursor=abc", "nroCandidato=0", "nroCandidato=-1"
	})
	void rechazaParametrosInvalidosConCuatrocientos(String consulta) throws Exception {
		comprobarError(get("?" + consulta), 400);
	}

	@Test
	void rechazaFiltrosDeTextoMayoresACincuentaCaracteres() throws Exception {
		JsonNode error = comprobarError(get("?nombre=" + "a".repeat(51)), 400);
		assertThat(error.path("erroresValidacion").path("nombre").asText()).isNotBlank();
	}

	@ParameterizedTest
	@ValueSource(strings = {"/previsualizar", "/importar"})
	void rechazaArchivosVaciosYSinElCampoArchivo(String ruta) throws Exception {
		comprobarError(enviarArchivo(ruta, "archivo", "candidatos.csv", new byte[0]), 400);
		comprobarError(enviarArchivo(ruta, "otro", "candidatos.csv", bytes(ENCABEZADO)), 400);
	}

	@ParameterizedTest
	@ValueSource(strings = {"/previsualizar", "/importar"})
	void rechazaExtensionEncabezadoSintaxisOCodificacionInvalidos(String ruta) throws Exception {
		comprobarError(enviarArchivo(ruta, "archivo", "candidatos.txt", bytes(ENCABEZADO)), 400);
		comprobarError(enviarArchivo(ruta, "archivo", "candidatos.csv", bytes("encabezado incorrecto")), 400);
		comprobarError(enviarArchivo(ruta, "archivo", "candidatos.csv", bytes(ENCABEZADO + "1;\"sin cerrar")), 400);
		comprobarError(enviarArchivo(ruta, "archivo", "candidatos.csv", new byte[] {(byte) 0xFF}), 400);
		assertThat(jdbc.queryForObject("select count(*) from candidato", Long.class)).isZero();
	}

	@Test
	void rechazaUnArchivoMayorADosMegabytesEnElServidorReal() throws Exception {
		byte[] grande = new byte[2 * 1024 * 1024 + 1];
		comprobarError(enviarArchivo("/importar", "archivo", "candidatos.csv", grande), 413);
		assertThat(jdbc.queryForObject("select count(*) from candidato", Long.class)).isZero();
	}

	@Test
	void rechazaJsonEnLosEndpointsMultipart() throws Exception {
		HttpRequest request = request("/importar")
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{}"))
				.build();

		comprobarError(CLIENTE.send(request, HttpResponse.BodyHandlers.ofString()), 415);
	}

	@Test
	void respondeCuatrocientosSeisCuandoElClienteNoAceptaJson() throws Exception {
		HttpRequest request = HttpRequest.newBuilder(uri(""))
				.header("Accept", "text/html")
				.GET().build();

		comprobarError(CLIENTE.send(request, HttpResponse.BodyHandlers.ofString()), 406);
	}

	private JsonNode cuerpoExitoso(HttpResponse<String> respuesta) {
		assertThat(respuesta.statusCode()).as(respuesta.body()).isEqualTo(200);
		assertThat(respuesta.headers().firstValue("Content-Type").orElse("")).contains("application/json");

		return json.readTree(respuesta.body());
	}

	private JsonNode comprobarError(HttpResponse<String> respuesta, int status) {
		assertThat(respuesta.statusCode()).as(respuesta.body()).isEqualTo(status);
		JsonNode error = json.readTree(respuesta.body());

		assertThat(error.path("status").asInt()).isEqualTo(status);
		assertThat(error.path("message").asText()).isNotBlank();
		assertThat(error.path("path").asText()).startsWith("/api/candidatos");
		assertThat(error.path("timestamp").asText()).isNotBlank();

		return error;
	}

	private HttpResponse<String> get(String consulta) throws Exception {
		return CLIENTE.send(request(consulta).GET().build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> enviarArchivo(String ruta, String campo, String nombre, byte[] contenido)
			throws Exception {

		String boundary = "cu06-test-boundary";
		ByteArrayOutputStream cuerpo = new ByteArrayOutputStream();
		cuerpo.writeBytes(bytes("--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"" + campo + "\"; filename=\"" + nombre + "\"\r\n"
				+ "Content-Type: application/octet-stream\r\n\r\n"));
		cuerpo.writeBytes(contenido);
		cuerpo.writeBytes(bytes("\r\n--" + boundary + "--\r\n"));

		HttpRequest request = request(ruta)
				.header("Content-Type", "multipart/form-data; boundary=" + boundary)
				.POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo.toByteArray()))
				.build();

		return CLIENTE.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private HttpRequest.Builder request(String sufijo) {
		return HttpRequest.newBuilder(uri(sufijo))
				.timeout(Duration.ofSeconds(10))
				.header("Accept", "application/json");
	}

	private URI uri(String sufijo) {
		return URI.create("http://localhost:" + puerto + "/api/candidatos" + sufijo);
	}

	private byte[] bytes(String contenido) {
		return contenido.getBytes(StandardCharsets.UTF_8);
	}

	private String fila(long numero, boolean eliminado) {
		return numero + ";Pérez;Ana;DOC" + numero
				+ ";DNI;1996-02-29;M;Argentina;ana@example.test;Universitario;" + eliminado;
	}

	private Candidato candidato(long numero) {
		return new Candidato(null, numero, "Pérez", "Original", TipoDocumento.DNI, "DOC" + numero,
				LocalDate.of(1990, 1, 1), "M", "Argentina", "original@example.test", "Universitario", false);
	}
}
