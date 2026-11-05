package com.deso.api.controller;

import com.deso.api.dao.CompetenciaDAO;
import com.deso.api.model.entity.Competencia;
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
        "spring.datasource.url=jdbc:h2:mem:cu13-controller-tests",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "logging.file.name=target/cu13-controller-tests.log"
})
class CompetenciaControllerTests {

    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @LocalServerPort
    private int puerto;

    @Autowired
    private CompetenciaDAO dao;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JsonMapper json;

    @BeforeEach
    @AfterEach
    void limpiarBase() {
        jdbc.update("delete from competencia");
    }

    @Test
    void postCreaCompetenciaYRetorna201() throws Exception {
        String body = """
                {
                    "codigo": "COMP-01",
                    "nombre": "Gestión de Proyectos",
                    "tipo": "TECNICA",
                    "descripcion": "Capacidad de planificar y gestionar proyectos."
                }
                """;

        HttpRequest request = request("")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = CLIENTE.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Location")).isPresent();

        JsonNode nodo = json.readTree(response.body());
        assertThat(nodo.path("id").asLong()).isPositive();
        assertThat(nodo.path("codigo").asString()).isEqualTo("COMP-01");
        assertThat(nodo.path("nombre").asString()).isEqualTo("Gestión de Proyectos");
        assertThat(nodo.path("tipo").asString()).isEqualTo("TECNICA");
        assertThat(nodo.path("eliminada").asBoolean()).isFalse();
    }

    @Test
    void postFallaCon409SiCodigoYaExiste() throws Exception {
        dao.guardar(new Competencia(null, "COMP-01", "Nombre Original", "Desc", TipoCompetencia.TECNICA, false));

        String body = """
                {
                    "codigo": "COMP-01",
                    "nombre": "Otro Nombre",
                    "tipo": "ACTITUDINAL"
                }
                """;

        HttpRequest request = request("")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = CLIENTE.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(409);

        JsonNode nodo = json.readTree(response.body());
        assertThat(nodo.path("status").asInt()).isEqualTo(409);
        assertThat(nodo.path("message").asString()).contains("COMP-01");
    }

    @Test
    void postFallaCon409SiNombreYaExiste() throws Exception {
        dao.guardar(new Competencia(null, "COMP-01", "Liderazgo", "Desc", TipoCompetencia.ACTITUDINAL, false));

        String body = """
                {
                    "codigo": "COMP-02",
                    "nombre": "Liderazgo",
                    "tipo": "ACTITUDINAL"
                }
                """;

        HttpRequest request = request("")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = CLIENTE.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(409);

        JsonNode nodo = json.readTree(response.body());
        assertThat(nodo.path("status").asInt()).isEqualTo(409);
        assertThat(nodo.path("message").asString()).contains("Liderazgo");
    }

    @Test
    void postFallaCon400SiDatosSonInvalidos() throws Exception {
        String body = """
                {
                    "codigo": "",
                    "nombre": "",
                    "tipo": null
                }
                """;

        HttpRequest request = request("")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = CLIENTE.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(400);

        JsonNode nodo = json.readTree(response.body());
        assertThat(nodo.path("status").asInt()).isEqualTo(400);
    }

    @Test
    void getListarRetornaCompetenciasActivasConFiltros() throws Exception {
        dao.guardar(new Competencia(null, "COMP-01", "Gestión de Proyectos", "Desc", TipoCompetencia.TECNICA, false));
        dao.guardar(new Competencia(null, "COMP-02", "Ingeniería Backend", "Desc", TipoCompetencia.TECNICA, false));
        dao.guardar(new Competencia(null, "COMP-03", "Lealtad", "Desc", TipoCompetencia.ACTITUDINAL, false));

        HttpRequest request = request("?tipo=TECNICA").GET().build();
        HttpResponse<String> response = CLIENTE.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode nodo = json.readTree(response.body());
        assertThat(nodo.isArray()).isTrue();
        assertThat(nodo.size()).isEqualTo(2);

        HttpRequest requestNombre = request("?nombre=backend").GET().build();
        HttpResponse<String> responseNombre = CLIENTE.send(requestNombre, HttpResponse.BodyHandlers.ofString());
        JsonNode nodoNombre = json.readTree(responseNombre.body());
        assertThat(nodoNombre.size()).isEqualTo(1);
        assertThat(nodoNombre.get(0).path("codigo").asString()).isEqualTo("COMP-02");
    }

    @Test
    void getPorIdRetornaCompetenciaO404() throws Exception {
        Competencia guardada = dao.guardar(new Competencia(null, "COMP-10", "Innovación", "Desc", TipoCompetencia.ACTITUDINAL, false));

        HttpRequest requestOk = request("/" + guardada.getId()).GET().build();
        HttpResponse<String> responseOk = CLIENTE.send(requestOk, HttpResponse.BodyHandlers.ofString());
        assertThat(responseOk.statusCode()).isEqualTo(200);

        HttpRequest request404 = request("/99999").GET().build();
        HttpResponse<String> response404 = CLIENTE.send(request404, HttpResponse.BodyHandlers.ofString());
        assertThat(response404.statusCode()).isEqualTo(404);
    }

    private HttpRequest.Builder request(String sufijo) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/competencias" + sufijo))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json");
    }
}
