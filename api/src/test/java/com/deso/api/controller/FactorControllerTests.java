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
        "spring.datasource.url=jdbc:h2:mem:factores-controller-tests",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "logging.file.name=target/factores-controller-tests.log"
})
class FactorControllerTests {

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

    private Competencia gestion;

    @BeforeEach
    void setUp() {
        limpiarBase();
        gestion = competenciaDAO.guardar(
                new Competencia(null, "COMP-01", "Gestión de proyectos", null, TipoCompetencia.TECNICA, false));
    }

    @AfterEach
    void limpiarBase() {
        jdbc.update("delete from factor");
        jdbc.update("delete from competencia");
    }

    @Test
    void postCreaElFactorYDevuelve201ConLocation() throws Exception {
        HttpResponse<String> response = enviar("POST", "", """
                {"competenciaId": %d, "codigo": " FAC-01 ", "nombre": "Análisis", "orden": 1, "descripcion": "Lógica"}
                """.formatted(gestion.getId()));

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode nodo = json.readTree(response.body());
        long id = nodo.path("id").asLong();
        assertThat(id).isPositive();
        assertThat(response.headers().firstValue("Location")).hasValue("/api/factores/" + id);
        assertThat(nodo.path("competenciaId").asLong()).isEqualTo(gestion.getId());
        assertThat(nodo.path("codigo").asString()).isEqualTo("FAC-01");
    }

    @Test
    void postDevuelve400SiFaltanCamposObligatorios() throws Exception {
        HttpResponse<String> response = enviar("POST", "", """
                {"competenciaId": null, "codigo": "", "nombre": "", "orden": 0}
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        JsonNode errores = json.readTree(response.body()).path("erroresValidacion");
        assertThat(errores.has("competenciaId")).isTrue();
        assertThat(errores.has("codigo")).isTrue();
        assertThat(errores.has("nombre")).isTrue();
        assertThat(errores.has("orden")).isTrue();
    }

    @Test
    void postDevuelve409SiElOrdenYaEstaEnUso() throws Exception {
        guardar("FAC-01", "Análisis", 1, false);

        HttpResponse<String> response = enviar("POST", "", """
                {"competenciaId": %d, "codigo": "FAC-02", "nombre": "Detalle", "orden": 1}
                """.formatted(gestion.getId()));

        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    void postDevuelve404SiLaCompetenciaNoExiste() throws Exception {
        HttpResponse<String> response = enviar("POST", "", """
                {"competenciaId": 9999, "codigo": "FAC-01", "nombre": "Análisis", "orden": 1}
                """);

        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void putModificaElFactor() throws Exception {
        Factor factor = guardar("FAC-01", "Análisis", 1, false);

        HttpResponse<String> response = enviar("PUT", "/" + factor.getId(), """
                {"competenciaId": %d, "codigo": "FAC-01", "nombre": "Análisis crítico", "orden": 2}
                """.formatted(gestion.getId()));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode nodo = json.readTree(response.body());
        assertThat(nodo.path("nombre").asString()).isEqualTo("Análisis crítico");
        assertThat(nodo.path("orden").asInt()).isEqualTo(2);
    }

    @Test
    void deleteDaDeBajaYDespuesElFactorNoSeEncuentra() throws Exception {
        Factor factor = guardar("FAC-01", "Análisis", 1, false);

        assertThat(enviar("DELETE", "/" + factor.getId(), null).statusCode()).isEqualTo(204);
        assertThat(enviar("GET", "/" + factor.getId(), null).statusCode()).isEqualTo(404);
        assertThat(enviar("DELETE", "/" + factor.getId(), null).statusCode()).isEqualTo(404);
    }

    @Test
    void getPorIdDevuelveElFactorO404() throws Exception {
        Factor factor = guardar("FAC-01", "Análisis", 1, false);

        HttpResponse<String> ok = enviar("GET", "/" + factor.getId(), null);
        assertThat(ok.statusCode()).isEqualTo(200);
        assertThat(json.readTree(ok.body()).path("codigo").asString()).isEqualTo("FAC-01");

        assertThat(enviar("GET", "/9999", null).statusCode()).isEqualTo(404);
    }

    @Test
    void getListadoFiltraYDevuelveLaCompetencia() throws Exception {
        guardar("FAC-01", "Capacidad de análisis", 1, false);
        guardar("FAC-02", "Atención al detalle", 2, false);

        HttpResponse<String> response = enviar("GET", "?nombreCompetencia=gesti%C3%B3n&nombreFactor=an%C3%A1lisis", null);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode nodo = json.readTree(response.body());
        assertThat(nodo.isArray()).isTrue();
        assertThat(nodo.size()).isEqualTo(1);
        assertThat(nodo.get(0).path("codigo").asString()).isEqualTo("FAC-01");
        assertThat(nodo.get(0).path("nombreFactor").asString()).isEqualTo("Capacidad de análisis");
        assertThat(nodo.get(0).path("competenciaId").asLong()).isEqualTo(gestion.getId());
        assertThat(nodo.get(0).path("nombreCompetencia").asString()).isEqualTo("Gestión de proyectos");
    }

    @Test
    void getListadoDevuelve400SiLaPaginacionEsInvalida() throws Exception {
        assertThat(enviar("GET", "?cantidad=0", null).statusCode()).isEqualTo(400);
        assertThat(enviar("GET", "?cantidad=101", null).statusCode()).isEqualTo(400);
        assertThat(enviar("GET", "?offset=-1", null).statusCode()).isEqualTo(400);
    }

    @Test
    void getPorCompetenciaDevuelveLosActivosOrdenados() throws Exception {
        guardar("FAC-02", "Segundo", 2, false);
        guardar("FAC-01", "Primero", 1, false);
        guardar("FAC-03", "Baja", 3, true);

        HttpResponse<String> response = enviar("GET", "/competencia/" + gestion.getId(), null);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode nodo = json.readTree(response.body());
        assertThat(nodo.size()).isEqualTo(2);
        assertThat(nodo.get(0).path("codigo").asString()).isEqualTo("FAC-01");
        assertThat(nodo.get(1).path("codigo").asString()).isEqualTo("FAC-02");
    }

    private Factor guardar(String codigo, String nombre, int orden, boolean eliminado) {
        return factorDAO.guardar(new Factor(null, gestion, codigo, nombre, orden, null, eliminado));
    }

    private HttpResponse<String> enviar(String metodo, String sufijo, String cuerpo) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + puerto + "/api/factores" + sufijo))
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
