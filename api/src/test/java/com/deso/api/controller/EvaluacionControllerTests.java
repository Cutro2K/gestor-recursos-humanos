package com.deso.api.controller;

import com.deso.api.dao.ConsultorDAO;
import com.deso.api.model.entity.Consultor;
import com.deso.api.security.EmisorTokens;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de punta a punta del CU 25 por HTTP: sesión de consultor, verificación de candidatos,
 * competencias del puesto, generación de la evaluación con su modelo y exportación a Excel.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:evaluacion-controller-tests",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "logging.file.name=target/evaluacion-controller-tests.log"
})
class EvaluacionControllerTests {

    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @LocalServerPort
    private int puerto;

    @Autowired
    private ConsultorDAO consultorDAO;

    @Autowired
    private EmisorTokens emisorTokens;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JsonMapper json;

    private String token;

    @BeforeEach
    void setUp() {
        // puesto_competencia todavía no tiene entidad, así que Hibernate no la crea en H2.
        jdbc.execute("""
                create table if not exists puesto_competencia (
                    puesto_id bigint not null,
                    competencia_id bigint not null,
                    ponderacion_minima decimal(4, 2) not null,
                    primary key (puesto_id, competencia_id))
                """);
        limpiarBase();

        Consultor consultor = consultorDAO.guardar(new Consultor(
                null, "consultor_cu25", "Ana", "Gómez", "cu25@capitalhumano.test", true,
                LocalDateTime.of(2026, 4, 1, 9, 0)));
        token = emisorTokens.emitirAccess(String.valueOf(consultor.getId()), "CONSULTOR", Instant.now().plusSeconds(300));

        jdbc.update("insert into puesto (id, codigo, nombre, descripcion, empresa, eliminado) values (1, 'PUESTO-01', 'Desarrollador Java Senior', null, 'Acme', false)");
        jdbc.update("insert into competencia (id, codigo, nombre, descripcion, tipo, eliminada) values (1, 'COMP-01', 'Gestión de Proyectos', null, 'TECNICA', false)");
        jdbc.update("insert into puesto_competencia (puesto_id, competencia_id, ponderacion_minima) values (1, 1, 7.00)");
        jdbc.update("insert into factor (id, competencia_id, codigo, nombre, orden, descripcion, eliminado) values (1, 1, 'FAC-01', 'Gestión del alcance', 1, null, false)");
        jdbc.update("insert into opcion_respuesta (id, nombre, descripcion, eliminada) values (1, 'Verdadero/Falso', 'VF', false)");
        jdbc.update("insert into item_opcion (id, opcion_respuesta_id, orden, descripcion) values (1, 1, 1, 'Verdadero'), (2, 1, 2, 'Falso')");
        insertarPregunta(1, 1, "10", "0");
        insertarPregunta(2, 1, "8", "2");

        insertarCandidato(10, "Pérez", "Juan");
        insertarCandidato(20, "López", "María");
    }

    @AfterEach
    void limpiarBase() {
        for (String tabla : List.of("modelo_opcion", "modelo_pregunta", "modelo_factor", "modelo_competencia",
                "modelo_evaluacion", "cuestionario", "evaluacion", "pregunta_item_opcion", "pregunta", "item_opcion",
                "opcion_respuesta", "puesto_competencia", "factor", "competencia", "puesto", "candidato", "consultor")) {
            jdbc.update("delete from " + tabla);
        }
    }

    @Test
    void generaLaEvaluacionConSuModeloYCuestionariosActivosYExportaLasClaves() throws Exception {
        assertThat(post("/verificacion-candidatos", "{\"candidatoIds\":[10,20]}", null).statusCode()).isEqualTo(204);

        JsonNode competencias = json.readTree(get("/puestos/1/competencias").body());
        assertThat(competencias).hasSize(1);
        assertThat(competencias.get(0).path("nombre").asString()).isEqualTo("Gestión de Proyectos");
        assertThat(competencias.get(0).path("evaluable").asBoolean()).isTrue();

        HttpResponse<String> creada = post("", "{\"puestoId\":1,\"candidatoIds\":[10,20,10]}", token);
        assertThat(creada.statusCode()).isEqualTo(201);
        JsonNode resultado = json.readTree(creada.body());
        long evaluacionId = resultado.path("evaluacionId").asLong();
        assertThat(creada.headers().firstValue("Location")).hasValue("/api/evaluaciones/" + evaluacionId);
        assertThat(resultado.path("nombreArchivoExcel").asString()).endsWith("-DesarrolladorJavaSenior.xls");
        JsonNode credenciales = resultado.path("credenciales");
        assertThat(credenciales).hasSize(2);
        assertThat(credenciales.get(0).path("apellido").asString()).isEqualTo("López");
        assertThat(credenciales.get(0).path("tipoDocumento").asString()).isEqualTo("DNI");
        assertThat(credenciales.get(0).path("claveIngreso").asString()).matches("[A-Z2-9]{8}");

        // Modelo congelado completo y cuestionarios ACTIVO sin preguntas todavía (las sortea el CU 26).
        assertThat(contar("modelo_evaluacion")).isEqualTo(1);
        assertThat(contar("modelo_competencia")).isEqualTo(1);
        assertThat(contar("modelo_factor")).isEqualTo(1);
        assertThat(contar("modelo_pregunta")).isEqualTo(2);
        assertThat(jdbc.queryForList("select ponderacion_snapshot from modelo_opcion order by id", BigDecimal.class))
                .extracting(BigDecimal::toPlainString)
                .containsExactly("10.00", "0.00", "8.00", "2.00");
        assertThat(jdbc.queryForList("select estado from cuestionario", String.class)).containsExactly("ACTIVO", "ACTIVO");
        assertThat(contar("bloque_cuestionario")).isZero();

        // Flujo 6.A: ahora los dos tienen un cuestionario activo.
        HttpResponse<String> repetida = post("/verificacion-candidatos", "{\"candidatoIds\":[10,20]}", null);
        assertThat(repetida.statusCode()).isEqualTo(409);
        assertThat(json.readTree(repetida.body()).path("message").asString())
                .contains("López, María").contains("Pérez, Juan");
        assertThat(post("", "{\"puestoId\":1,\"candidatoIds\":[10]}", token).statusCode()).isEqualTo(409);

        HttpResponse<byte[]> excel = getBytes("/" + evaluacionId + "/claves", token);
        assertThat(excel.statusCode()).isEqualTo(200);
        assertThat(excel.headers().firstValue("Content-Type")).hasValueSatisfying(v -> assertThat(v).startsWith("application/vnd.ms-excel"));
        assertThat(excel.headers().firstValue("Content-Disposition"))
                .hasValueSatisfying(v -> assertThat(v).contains("attachment").contains("DesarrolladorJavaSenior.xls"));
        try (HSSFWorkbook libro = new HSSFWorkbook(new ByteArrayInputStream(excel.body()))) {
            Sheet hoja = libro.getSheetAt(0);
            assertThat(hoja.getRow(4).getCell(0).getStringCellValue()).isEqualTo("López");
            assertThat(hoja.getRow(4).getCell(4).getStringCellValue())
                    .isEqualTo(credenciales.get(0).path("claveIngreso").asString());
            assertThat(hoja.getRow(5).getCell(0).getStringCellValue()).isEqualTo("Pérez");
        }
        assertThat(getBytes("/" + evaluacionId + "/claves", null).statusCode()).isEqualTo(401);
    }

    @Test
    void conUnaCompetenciaSinFactorEvaluable_laInformaYNoGeneraLaEvaluacion() throws Exception {
        jdbc.update("insert into competencia (id, codigo, nombre, descripcion, tipo, eliminada) values (3, 'COMP-03', 'Lealtad', null, 'ACTITUDINAL', false)");
        jdbc.update("insert into puesto_competencia (puesto_id, competencia_id, ponderacion_minima) values (1, 3, 6.00)");
        jdbc.update("insert into factor (id, competencia_id, codigo, nombre, orden, descripcion, eliminado) values (3, 3, 'FAC-03', 'Credibilidad', 1, null, false)");
        insertarPregunta(3, 3, "10", "0");

        JsonNode competencias = json.readTree(get("/puestos/1/competencias").body());
        assertThat(competencias).hasSize(2);
        assertThat(competencias.get(1).path("nombre").asString()).isEqualTo("Lealtad");
        assertThat(competencias.get(1).path("evaluable").asBoolean()).isFalse();

        HttpResponse<String> respuesta = post("", "{\"puestoId\":1,\"candidatoIds\":[10]}", token);
        assertThat(respuesta.statusCode()).isEqualTo(409);
        assertThat(json.readTree(respuesta.body()).path("message").asString())
                .startsWith("Las siguientes competencias no pueden ser evaluadas: Lealtad.");
        assertThat(contar("evaluacion")).isZero();
    }

    @Test
    void validaLaSesionYLosDatosDeEntrada() throws Exception {
        assertThat(post("", "{\"puestoId\":1,\"candidatoIds\":[10]}", null).statusCode()).isEqualTo(401);
        String tokenCandidato = emisorTokens.emitirAccess("10", "CANDIDATO", Instant.now().plusSeconds(300));
        assertThat(post("", "{\"puestoId\":1,\"candidatoIds\":[10]}", tokenCandidato).statusCode()).isEqualTo(401);

        assertThat(post("", "{\"puestoId\":1,\"candidatoIds\":[]}", token).statusCode()).isEqualTo(400);
        assertThat(post("", "{\"candidatoIds\":[10]}", token).statusCode()).isEqualTo(400);
        assertThat(post("/verificacion-candidatos", "{\"candidatoIds\":[]}", null).statusCode()).isEqualTo(400);
        assertThat(post("", "{\"puestoId\":999,\"candidatoIds\":[10]}", token).statusCode()).isEqualTo(404);
        assertThat(post("", "{\"puestoId\":1,\"candidatoIds\":[10,77]}", token).statusCode()).isEqualTo(404);
        assertThat(get("/puestos/999/competencias").statusCode()).isEqualTo(404);
        assertThat(get("/puestos/0/competencias").statusCode()).isEqualTo(400);
        assertThat(contar("evaluacion")).isZero();

        JsonNode puestos = json.readTree(get("/puestos").body());
        assertThat(puestos).hasSize(1);
        assertThat(puestos.get(0).path("empresa").asString()).isEqualTo("Acme");
    }

    // ---------------------------------------------------------------------------------------

    private void insertarPregunta(long id, long factorId, String ponderacionVerdadero, String ponderacionFalso) {
        jdbc.update("""
                insert into pregunta (id, factor_id, opcion_respuesta_id, nombre, enunciado, descripcion, activa, eliminada)
                values (?, ?, 1, ?, ?, null, true, false)
                """, id, factorId, "P" + id, "Enunciado " + id);
        jdbc.update("insert into pregunta_item_opcion (pregunta_id, item_opcion_id, ponderacion) values (?, 1, ?), (?, 2, ?)",
                id, new BigDecimal(ponderacionVerdadero), id, new BigDecimal(ponderacionFalso));
    }

    private void insertarCandidato(long id, String apellido, String nombre) {
        jdbc.update("""
                insert into candidato (id, nro_candidato, apellido, nombre, tipo_documento, nro_documento,
                    fecha_nacimiento, genero, nacionalidad, email, escolaridad, eliminado)
                values (?, ?, ?, ?, 'DNI', ?, DATE '1990-01-01', 'M', 'Argentina', 'c@test', 'Universitario', false)
                """, id, id, apellido, nombre, "DOC" + id);
    }

    private int contar(String tabla) {
        Integer cantidad = jdbc.queryForObject("select count(*) from " + tabla, Integer.class);
        return cantidad == null ? 0 : cantidad;
    }

    private HttpRequest.Builder request(String sufijo, String accessToken) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + puerto + "/api/evaluaciones" + sufijo))
                .timeout(Duration.ofSeconds(10));
        if (accessToken != null) {
            builder.header("Cookie", AuthController.COOKIE_ACCESS + "=" + accessToken);
        }
        return builder;
    }

    private HttpResponse<String> get(String sufijo) throws Exception {
        return CLIENTE.send(request(sufijo, null).header("Accept", "application/json").GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<byte[]> getBytes(String sufijo, String accessToken) throws Exception {
        return CLIENTE.send(request(sufijo, accessToken).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private HttpResponse<String> post(String sufijo, String cuerpoJson, String accessToken) throws Exception {
        return CLIENTE.send(request(sufijo, accessToken)
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(cuerpoJson, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
