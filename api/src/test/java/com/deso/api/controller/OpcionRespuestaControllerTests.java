package com.deso.api.controller;

import com.deso.api.dao.OpcionRespuestaDAO;
import com.deso.api.model.entity.ItemOpcion;
import com.deso.api.model.entity.OpcionRespuesta;
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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:opciones-respuesta-controller-tests",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "logging.file.name=target/opciones-respuesta-controller-tests.log"
})
class OpcionRespuestaControllerTests {

    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @LocalServerPort
    private int puerto;

    @Autowired
    private OpcionRespuestaDAO opcionRespuestaDAO;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JsonMapper json;

    @BeforeEach
    void setUp() {
        limpiarBase();
    }

    @AfterEach
    void limpiarBase() {
        jdbc.update("delete from item_opcion");
        jdbc.update("delete from opcion_respuesta");
    }

    @Test
    void getListadoDevuelveSoloActivasOrdenadasSinItemsYSoportaFiltroVacio() throws Exception {
        guardar("VERDADERO_FALSO", "Opciones dicotómicas", false,
                item(1, "Verdadero"), item(2, "Falso"));
        guardar("ESCALA_DESEMPENO", "Cinco niveles valorativos", false,
                item(1, "Malo"), item(2, "Excelente"));
        guardar("ESCALA_ELIMINADA", "Dada de baja", true,
                item(1, "Sí"), item(2, "No"));

        HttpResponse<String> response = enviarGet("");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode lista = json.readTree(response.body());
        assertThat(lista.isArray()).isTrue();
        assertThat(lista.size()).isEqualTo(2);
        assertThat(lista.get(0).path("nombre").asString()).isEqualTo("ESCALA_DESEMPENO");
        assertThat(lista.get(0).path("descripcion").asString()).isEqualTo("Cinco niveles valorativos");
        assertThat(lista.get(0).has("items")).isFalse();
        assertThat(lista.get(1).path("nombre").asString()).isEqualTo("VERDADERO_FALSO");

        HttpResponse<String> filtroVacio = enviarGet("?nombre=" + codificar("   "));
        assertThat(filtroVacio.statusCode()).isEqualTo(200);
        assertThat(json.readTree(filtroVacio.body()).size()).isEqualTo(2);
    }

    @Test
    void getListadoFiltraPorNombreIgnorandoMayusculasYEscapandoComodinesYPagina() throws Exception {
        guardar("CUMPLIMIENTO 100%", "Porcentaje completo", false,
                item(1, "Sí"), item(2, "No"));
        guardar("CUMPLIMIENTO 1000", "Mil puntos", false,
                item(1, "Sí"), item(2, "No"));
        guardar("OPCION_A", "Con guion bajo", false,
                item(1, "Sí"), item(2, "No"));
        guardar("OPCIONXA", "Sin guion bajo", false,
                item(1, "Sí"), item(2, "No"));
        guardar("NIVEL!1", "Con signo de exclamación", false,
                item(1, "Sí"), item(2, "No"));
        guardar("NIVEL1", "Sin signo de exclamación", false,
                item(1, "Sí"), item(2, "No"));

        HttpResponse<String> porPorcentaje = enviarGet("?nombre=" + codificar("cumplimiento 100%"));
        assertThat(porPorcentaje.statusCode()).isEqualTo(200);
        JsonNode resPorcentaje = json.readTree(porPorcentaje.body());
        assertThat(resPorcentaje.size()).isEqualTo(1);
        assertThat(resPorcentaje.get(0).path("nombre").asString()).isEqualTo("CUMPLIMIENTO 100%");

        HttpResponse<String> porGuion = enviarGet("?nombre=" + codificar("opcion_a"));
        assertThat(json.readTree(porGuion.body()).size()).isEqualTo(1);
        assertThat(json.readTree(porGuion.body()).get(0).path("nombre").asString()).isEqualTo("OPCION_A");

        HttpResponse<String> porExclamacion = enviarGet("?nombre=" + codificar("nivel!1"));
        assertThat(json.readTree(porExclamacion.body()).size()).isEqualTo(1);
        assertThat(json.readTree(porExclamacion.body()).get(0).path("nombre").asString()).isEqualTo("NIVEL!1");

        HttpResponse<String> pagina = enviarGet("?offset=1&cantidad=2");
        assertThat(pagina.statusCode()).isEqualTo(200);
        JsonNode resPagina = json.readTree(pagina.body());
        assertThat(resPagina.size()).isEqualTo(2);
        assertThat(resPagina.get(0).path("nombre").asString()).isEqualTo("CUMPLIMIENTO 1000");
        assertThat(resPagina.get(1).path("nombre").asString()).isEqualTo("NIVEL!1");
    }

    @Test
    void getListadoDevuelve400SiParametrosFueraDeRangoONombreSupera50Caracteres() throws Exception {
        assertThat(enviarGet("?offset=-1").statusCode()).isEqualTo(400);
        assertThat(enviarGet("?cantidad=0").statusCode()).isEqualTo(400);
        assertThat(enviarGet("?cantidad=101").statusCode()).isEqualTo(400);
        assertThat(enviarGet("?nombre=" + "A".repeat(51)).statusCode()).isEqualTo(400);
        assertThat(enviarGet("?nombre=" + "A".repeat(50)).statusCode()).isEqualTo(200);
    }

    @Test
    void getPorIdDevuelveDetalleConItemsOrdenadosOErrores400Y404() throws Exception {
        OpcionRespuesta activa = guardar("ESCALA_DESEMPENO", "Escala de tres niveles", false,
                item(3, "Excelente"),
                item(1, "Malo"),
                item(2, "Regular"));
        OpcionRespuesta eliminada = guardar("OPCION_HISTORICA", "Dada de baja", true,
                item(1, "Sí"),
                item(2, "No"));

        HttpResponse<String> ok = enviarGet("/" + activa.getId());
        assertThat(ok.statusCode()).isEqualTo(200);
        JsonNode nodo = json.readTree(ok.body());
        assertThat(nodo.path("id").asLong()).isEqualTo(activa.getId());
        assertThat(nodo.path("nombre").asString()).isEqualTo("ESCALA_DESEMPENO");
        assertThat(nodo.path("descripcion").asString()).isEqualTo("Escala de tres niveles");
        JsonNode items = nodo.path("items");
        assertThat(items.isArray()).isTrue();
        assertThat(items.size()).isEqualTo(3);
        assertThat(items.get(0).path("orden").asInt()).isEqualTo(1);
        assertThat(items.get(0).path("descripcion").asString()).isEqualTo("Malo");
        assertThat(items.get(1).path("orden").asInt()).isEqualTo(2);
        assertThat(items.get(1).path("descripcion").asString()).isEqualTo("Regular");
        assertThat(items.get(2).path("orden").asInt()).isEqualTo(3);
        assertThat(items.get(2).path("descripcion").asString()).isEqualTo("Excelente");

        assertThat(enviarGet("/0").statusCode()).isEqualTo(400);
        assertThat(enviarGet("/-3").statusCode()).isEqualTo(400);
        assertThat(enviarGet("/abc").statusCode()).isEqualTo(400);
        assertThat(enviarGet("/99999").statusCode()).isEqualTo(404);
        assertThat(enviarGet("/" + eliminada.getId()).statusCode()).isEqualTo(404);
    }

    private OpcionRespuesta guardar(String nombre, String descripcion, boolean eliminada, ItemOpcion... items) {
        OpcionRespuesta opcion = new OpcionRespuesta(null, nombre, descripcion, eliminada, new ArrayList<>());
        for (ItemOpcion item : items) {
            item.setOpcionRespuesta(opcion);
            opcion.getItems().add(item);
        }
        return opcionRespuestaDAO.guardar(opcion);
    }

    private static ItemOpcion item(int orden, String descripcion) {
        return new ItemOpcion(null, null, orden, descripcion);
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private HttpResponse<String> enviarGet(String sufijo) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + puerto + "/api/opciones-respuesta" + sufijo))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();
        return CLIENTE.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
