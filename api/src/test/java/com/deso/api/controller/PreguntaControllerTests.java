package com.deso.api.controller;

import com.deso.api.dao.CompetenciaDAO;
import com.deso.api.dao.FactorDAO;
import com.deso.api.model.entity.Competencia;
import com.deso.api.model.entity.Factor;
import com.deso.api.model.enums.TipoCompetencia;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:preguntas-controller-tests",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "logging.file.name=target/preguntas-controller-tests.log"
})
class PreguntaControllerTests {

    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @LocalServerPort
    private int puerto;

    @Autowired
    private CompetenciaDAO competenciaDAO;

    @Autowired
    private FactorDAO factorDAO;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JsonMapper json;

    private Competencia competencia;
    private Factor factor;
    private Long opcionRespuestaId;
    private Long itemVerdaderoId;
    private Long itemFalsoId;

    @BeforeEach
    void setUp() {
        limpiarBase();
        competencia = competenciaDAO.guardar(
                new Competencia(null, "COMP-01", "Gestión de proyectos", null, TipoCompetencia.TECNICA, false));
        factor = factorDAO.guardar(
                new Factor(null, competencia, "FAC-01", "Alcance", 1, null, false));

        jdbc.update("INSERT INTO opcion_respuesta (nombre, descripcion, eliminada) VALUES ('VERDADERO_FALSO', 'V/F', FALSE)");
        opcionRespuestaId = jdbc.queryForObject("SELECT id FROM opcion_respuesta WHERE nombre = 'VERDADERO_FALSO'", Long.class);

        jdbc.update("INSERT INTO item_opcion (opcion_respuesta_id, orden, descripcion) VALUES (?, 1, 'Verdadero')", opcionRespuestaId);
        jdbc.update("INSERT INTO item_opcion (opcion_respuesta_id, orden, descripcion) VALUES (?, 2, 'Falso')", opcionRespuestaId);
        itemVerdaderoId = jdbc.queryForObject("SELECT id FROM item_opcion WHERE descripcion = 'Verdadero'", Long.class);
        itemFalsoId = jdbc.queryForObject("SELECT id FROM item_opcion WHERE descripcion = 'Falso'", Long.class);
    }

    @AfterEach
    void limpiarBase() {
        jdbc.update("delete from pregunta_item_opcion");
        jdbc.update("delete from pregunta");
        jdbc.update("delete from item_opcion");
        jdbc.update("delete from opcion_respuesta");
        jdbc.update("delete from factor");
        jdbc.update("delete from competencia");
    }

    @Test
    void postCreaPreguntaConPonderacionesYDevuelve201ConLocation() throws Exception {
        HttpResponse<String> response = enviar("POST", "", payloadValido("WBS y Alcance", "¿La WBS define el alcance?"));

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode nodo = json.readTree(response.body());
        long id = nodo.path("id").asLong();
        assertThat(id).isPositive();
        assertThat(response.headers().firstValue("Location")).hasValue("/api/preguntas/" + id);
        assertThat(nodo.path("nombre").asString()).isEqualTo("WBS y Alcance");
        assertThat(nodo.path("activa").asBoolean()).isTrue();

        Long cantPonderaciones = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pregunta_item_opcion WHERE pregunta_id = ?", Long.class, id);
        assertThat(cantPonderaciones).isEqualTo(2L);
    }

    @Test
    void postDevuelve400SiFaltanCamposObligatorios() throws Exception {
        HttpResponse<String> response = enviar("POST", "", """
                {"factorId": null, "opcionRespuestaId": null, "nombre": "", "enunciado": "", "ponderaciones": []}
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        JsonNode errores = json.readTree(response.body()).path("erroresValidacion");
        assertThat(errores.has("factorId")).isTrue();
        assertThat(errores.has("opcionRespuestaId")).isTrue();
        assertThat(errores.has("nombre")).isTrue();
        assertThat(errores.has("enunciado")).isTrue();
        assertThat(errores.has("ponderaciones")).isTrue();
    }

    @Test
    void postDevuelve400SiFaltaPonderacionDeAlgunItem() throws Exception {
        String body = """
                {
                    "factorId": %d,
                    "opcionRespuestaId": %d,
                    "nombre": "Pregunta Incompleta",
                    "enunciado": "¿Falta un ítem?",
                    "ponderaciones": [
                        {"itemOpcionId": %d, "ponderacion": 10.0}
                    ]
                }
                """.formatted(factor.getId(), opcionRespuestaId, itemVerdaderoId);

        assertThat(enviar("POST", "", body).statusCode()).isEqualTo(400);
    }

    @Test
    void postDevuelve409SiNombreOEnunciadoYaExistenEnElMismoFactor() throws Exception {
        assertThat(enviar("POST", "", payloadValido("WBS", "¿Qué es WBS?")).statusCode()).isEqualTo(201);

        assertThat(enviar("POST", "", payloadValido("wbs", "¿Otro enunciado?")).statusCode()).isEqualTo(409);
        assertThat(enviar("POST", "", payloadValido("Otro nombre", "¿qué es wbs?")).statusCode()).isEqualTo(409);
    }

    @Test
    void postDevuelve404SiFactorOOpcionNoExisten() throws Exception {
        String sinFactor = payloadValido("P1", "E1").replace("\"factorId\": " + factor.getId(), "\"factorId\": 9999");
        assertThat(enviar("POST", "", sinFactor).statusCode()).isEqualTo(404);

        String sinOpcion = payloadValido("P2", "E2").replace("\"opcionRespuestaId\": " + opcionRespuestaId, "\"opcionRespuestaId\": 9999");
        assertThat(enviar("POST", "", sinOpcion).statusCode()).isEqualTo(404);
    }

    @Test
    void getListarFiltraPorCompetenciaFactorYNombre() throws Exception {
        assertThat(enviar("POST", "", payloadValido("WBS y Alcance", "¿Define alcance?")).statusCode()).isEqualTo(201);
        assertThat(enviar("POST", "", payloadValido("Línea Base", "¿Requiere línea base?")).statusCode()).isEqualTo(201);

        HttpResponse<String> response = enviar(
                "GET", "?competenciaId=" + competencia.getId() + "&factorId=" + factor.getId() + "&nombre=wbs", null);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode nodo = json.readTree(response.body());
        assertThat(nodo.isArray()).isTrue();
        assertThat(nodo.size()).isEqualTo(1);
        assertThat(nodo.get(0).path("nombre").asString()).isEqualTo("WBS y Alcance");
        assertThat(nodo.get(0).path("nombreFactor").asString()).isEqualTo("Alcance");
        assertThat(nodo.get(0).path("nombreCompetencia").asString()).isEqualTo("Gestión de proyectos");
    }

    @Test
    void getPorIdDevuelveLaPreguntaO404() throws Exception {
        HttpResponse<String> creada = enviar("POST", "", payloadValido("WBS", "¿Enunciado?"));
        long id = json.readTree(creada.body()).path("id").asLong();

        HttpResponse<String> ok = enviar("GET", "/" + id, null);
        assertThat(ok.statusCode()).isEqualTo(200);
        assertThat(json.readTree(ok.body()).path("nombre").asString()).isEqualTo("WBS");

        assertThat(enviar("GET", "/9999", null).statusCode()).isEqualTo(404);
    }

    private String payloadValido(String nombre, String enunciado) {
        return """
                {
                    "factorId": %d,
                    "opcionRespuestaId": %d,
                    "nombre": "%s",
                    "enunciado": "%s",
                    "descripcion": "Descripción de prueba",
                    "ponderaciones": [
                        {"itemOpcionId": %d, "ponderacion": 10.0},
                        {"itemOpcionId": %d, "ponderacion": 0.0}
                    ]
                }
                """.formatted(factor.getId(), opcionRespuestaId, nombre, enunciado, itemVerdaderoId, itemFalsoId);
    }

    private HttpResponse<String> enviar(String metodo, String sufijo, String cuerpo) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + puerto + "/api/preguntas" + sufijo))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json");
        if (cuerpo == null) {
            builder.method(metodo, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(metodo, HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8));
        }
        return CLIENTE.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}