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

    @Test
    void postCreaOpcionRespuestaConItemsOrdenadosNormalizadosYLocationHeader201() throws Exception {
        String cuerpo = """
                {
                  "nombre": "  ESCALA_ACUERDO  ",
                  "descripcion": "  Tres grados de acuerdo  ",
                  "items": [
                    { "orden": 3, "descripcion": "  De acuerdo  " },
                    { "orden": 1, "descripcion": "  En desacuerdo  " },
                    { "orden": 2, "descripcion": "  Neutral  " }
                  ]
                }
                """;

        HttpResponse<String> response = enviarPost(cuerpo);

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode creado = json.readTree(response.body());
        long idCreado = creado.path("id").asLong();
        assertThat(idCreado).isPositive();
        assertThat(response.headers().firstValue("Location"))
                .hasValue("/api/opciones-respuesta/" + idCreado);
        assertThat(creado.path("nombre").asString()).isEqualTo("ESCALA_ACUERDO");
        assertThat(creado.path("descripcion").asString()).isEqualTo("Tres grados de acuerdo");

        JsonNode items = creado.path("items");
        assertThat(items.size()).isEqualTo(3);
        assertThat(items.get(0).path("orden").asInt()).isEqualTo(1);
        assertThat(items.get(0).path("descripcion").asString()).isEqualTo("En desacuerdo");
        assertThat(items.get(1).path("orden").asInt()).isEqualTo(2);
        assertThat(items.get(1).path("descripcion").asString()).isEqualTo("Neutral");
        assertThat(items.get(2).path("orden").asInt()).isEqualTo(3);
        assertThat(items.get(2).path("descripcion").asString()).isEqualTo("De acuerdo");

        HttpResponse<String> consulta = enviarGet("/" + idCreado);
        assertThat(consulta.statusCode()).isEqualTo(200);
        assertThat(json.readTree(consulta.body()).path("nombre").asString()).isEqualTo("ESCALA_ACUERDO");
    }

    @Test
    void postRechaza409SiNombreActivoExistePeroPermite201SiSoloExisteEliminada() throws Exception {
        guardar("VERDADERO_FALSO", "Activa actual", false,
                item(1, "Verdadero"), item(2, "Falso"));
        guardar("ESCALA_HISTORICA", "Dada de baja", true,
                item(1, "Uno"), item(2, "Dos"));

        HttpResponse<String> conflicto = enviarPost("""
                {
                  "nombre": "  verdadero_falso ",
                  "descripcion": "Intento duplicado",
                  "items": [
                    { "orden": 1, "descripcion": "Sí" },
                    { "orden": 2, "descripcion": "No" }
                  ]
                }
                """);
        assertThat(conflicto.statusCode()).isEqualTo(409);
        JsonNode errConflicto = json.readTree(conflicto.body());
        assertThat(errConflicto.path("erroresValidacion").path("nombre").asString()).isNotBlank();

        HttpResponse<String> reutilizada = enviarPost("""
                {
                  "nombre": "ESCALA_HISTORICA",
                  "descripcion": "Nueva versión activa",
                  "items": [
                    { "orden": 1, "descripcion": "Bajo" },
                    { "orden": 2, "descripcion": "Alto" }
                  ]
                }
                """);
        assertThat(reutilizada.statusCode()).isEqualTo(201);
        assertThat(json.readTree(reutilizada.body()).path("nombre").asString()).isEqualTo("ESCALA_HISTORICA");
    }

    @Test
    void postDevuelve400ConErroresValidacionAnteOmisionesOLongitudesExcedidas() throws Exception {
        HttpResponse<String> omisiones = enviarPost("""
                {
                  "nombre": "   ",
                  "descripcion": "",
                  "items": [
                    { "orden": 1, "descripcion": "Único ítem" }
                  ]
                }
                """);
        assertThat(omisiones.statusCode()).isEqualTo(400);
        JsonNode erroresOmision = json.readTree(omisiones.body()).path("erroresValidacion");
        assertThat(erroresOmision.path("nombre").asString()).isNotBlank();
        assertThat(erroresOmision.path("descripcion").asString()).isNotBlank();
        assertThat(erroresOmision.path("items").asString()).isNotBlank();

        String largosExcedidos = """
                {
                  "nombre": "%s",
                  "descripcion": "%s",
                  "items": [
                    { "orden": 0, "descripcion": "   " },
                    { "orden": 2, "descripcion": "%s" }
                  ]
                }
                """.formatted("N".repeat(51), "D".repeat(1001), "I".repeat(151));

        HttpResponse<String> excedidos = enviarPost(largosExcedidos);
        assertThat(excedidos.statusCode()).isEqualTo(400);
        JsonNode erroresLargos = json.readTree(excedidos.body()).path("erroresValidacion");
        assertThat(erroresLargos.path("nombre").asString()).isNotBlank();
        assertThat(erroresLargos.path("descripcion").asString()).isNotBlank();
        assertThat(erroresLargos.path("items[0].orden").asString()).isNotBlank();
        assertThat(erroresLargos.path("items[0].descripcion").asString()).isNotBlank();
        assertThat(erroresLargos.path("items[1].descripcion").asString()).isNotBlank();
    }

    @Test
    void postDevuelve400SiOrdenNoEsConsecutivoDesdeUnoOSiDescripcionesDeItemsSeRepiten() throws Exception {
        HttpResponse<String> empiezaEnDos = enviarPost("""
                {
                  "nombre": "ESCALA_MAL_ORDEN",
                  "descripcion": "Empieza en 2",
                  "items": [
                    { "orden": 2, "descripcion": "A" },
                    { "orden": 3, "descripcion": "B" }
                  ]
                }
                """);
        assertThat(empiezaEnDos.statusCode()).isEqualTo(400);
        assertThat(json.readTree(empiezaEnDos.body()).path("erroresValidacion").path("items").asString()).isNotBlank();

        HttpResponse<String> conSalto = enviarPost("""
                {
                  "nombre": "ESCALA_CON_SALTO",
                  "descripcion": "Salta del 1 al 3",
                  "items": [
                    { "orden": 1, "descripcion": "A" },
                    { "orden": 3, "descripcion": "B" }
                  ]
                }
                """);
        assertThat(conSalto.statusCode()).isEqualTo(400);
        assertThat(json.readTree(conSalto.body()).path("erroresValidacion").path("items").asString()).isNotBlank();

        HttpResponse<String> ordenRepetido = enviarPost("""
                {
                  "nombre": "ESCALA_ORDEN_REPETIDO",
                  "descripcion": "Repite orden 1",
                  "items": [
                    { "orden": 1, "descripcion": "A" },
                    { "orden": 1, "descripcion": "B" }
                  ]
                }
                """);
        assertThat(ordenRepetido.statusCode()).isEqualTo(400);
        assertThat(json.readTree(ordenRepetido.body()).path("erroresValidacion").path("items").asString()).isNotBlank();

        HttpResponse<String> descripcionRepetida = enviarPost("""
                {
                  "nombre": "ESCALA_DESC_REPETIDA",
                  "descripcion": "Descripciones repetidas ignorando mayúsculas y espacios",
                  "items": [
                    { "orden": 1, "descripcion": "Verdadero" },
                    { "orden": 2, "descripcion": "  verdadero  " }
                  ]
                }
                """);
        assertThat(descripcionRepetida.statusCode()).isEqualTo(400);
        assertThat(json.readTree(descripcionRepetida.body()).path("erroresValidacion").path("items").asString()).isNotBlank();
    }

    @Test
    void putActualizaInPlaceConIntercambiosDeOrdenYDescripcionPreservandoIdsYNombre200() throws Exception {
        OpcionRespuesta op = guardar("ESCALA_PUT", "Inicial", false, item(1, "A"), item(2, "B"), item(3, "C"));
        long a = op.getItems().get(0).getId();
        long b = op.getItems().get(1).getId();
        long c = op.getItems().get(2).getId();

        HttpResponse<String> response = enviarPut(op.getId(), """
                { "descripcion": "  Nueva  ", "items": [
                  { "id": %d, "orden": 2, "descripcion": "B" },
                  { "id": %d, "orden": 1, "descripcion": "A" },
                  { "id": %d, "orden": 3, "descripcion": "Cc" } ] }
                """.formatted(a, b, c));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = json.readTree(response.body());
        assertThat(body.path("nombre").asString()).isEqualTo("ESCALA_PUT");
        assertThat(body.path("descripcion").asString()).isEqualTo("Nueva");
        JsonNode items = body.path("items");
        assertThat(items.size()).isEqualTo(3);
        assertThat(items.get(0).path("id").asLong()).isEqualTo(b);
        assertThat(items.get(0).path("descripcion").asString()).isEqualTo("A");
        assertThat(items.get(1).path("id").asLong()).isEqualTo(a);
        assertThat(items.get(1).path("descripcion").asString()).isEqualTo("B");
        assertThat(items.get(2).path("id").asLong()).isEqualTo(c);
        assertThat(items.get(2).path("orden").asInt()).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from item_opcion", Integer.class)).isEqualTo(3);
    }

    @Test
    void putPermiteNombreOmitidoOIgualYRechaza400NombreDistinto() throws Exception {
        OpcionRespuesta op = guardar("ESCALA_NOMBRE", "Inicial", false, item(1, "A"), item(2, "B"));
        long a = op.getItems().get(0).getId();
        long b = op.getItems().get(1).getId();
        String items = "[{\"id\": %d, \"orden\": 1, \"descripcion\": \"A\"}, {\"id\": %d, \"orden\": 2, \"descripcion\": \"B\"}]".formatted(a, b);

        assertThat(enviarPut(op.getId(), "{\"nombre\": \" ESCALA_NOMBRE \", \"descripcion\": \"X\", \"items\": " + items + "}").statusCode()).isEqualTo(200);
        assertThat(enviarPut(op.getId(), "{\"descripcion\": \"Y\", \"items\": " + items + "}").statusCode()).isEqualTo(200);
        assertThat(enviarPut(op.getId(), "{\"nombre\": \"OTRO\", \"descripcion\": \"Z\", \"items\": " + items + "}").statusCode()).isEqualTo(400);
    }

    @Test
    void putDevuelve400SiConjuntoDeIdsDeItemsNoCoincideOFallaValidacion() throws Exception {
        OpcionRespuesta op = guardar("ESCALA_IDS", "Inicial", false, item(1, "A"), item(2, "B"));
        OpcionRespuesta otra = guardar("ESCALA_OTRA", "Otra", false, item(1, "A"), item(2, "B"));
        long a = op.getItems().get(0).getId();
        long ajeno = otra.getItems().get(0).getId();

        assertThat(enviarPut(op.getId(), """
                { "descripcion": "X", "items": [
                  { "id": %d, "orden": 1, "descripcion": "A" },
                  { "id": %d, "orden": 2, "descripcion": "B" } ] }
                """.formatted(a, ajeno)).statusCode()).isEqualTo(400);
        assertThat(enviarPut(op.getId(), """
                { "descripcion": "X", "items": [
                  { "id": %d, "orden": 1, "descripcion": "A" },
                  { "id": %d, "orden": 2, "descripcion": "B" } ] }
                """.formatted(a, a)).statusCode()).isEqualTo(400);
        assertThat(enviarPut(op.getId(), """
                { "descripcion": "X", "items": [
                  { "orden": 1, "descripcion": "A" },
                  { "orden": 2, "descripcion": "B" } ] }
                """).statusCode()).isEqualTo(400);
        assertThat(enviarPut(op.getId(), "{ \"descripcion\": \"\", \"items\": [] }").statusCode()).isEqualTo(400);
        assertThat(enviarPut(0L, "{ \"descripcion\": \"X\", \"items\": [] }").statusCode()).isEqualTo(400);
    }

    @Test
    void putDevuelve404SiNoExisteOEstaEliminada() throws Exception {
        OpcionRespuesta eliminada = guardar("ESCALA_BAJA", "Baja", true, item(1, "A"), item(2, "B"));
        long a = eliminada.getItems().get(0).getId();
        long b = eliminada.getItems().get(1).getId();
        String cuerpo = """
                { "descripcion": "X", "items": [
                  { "id": %d, "orden": 1, "descripcion": "A" },
                  { "id": %d, "orden": 2, "descripcion": "B" } ] }
                """.formatted(a, b);

        assertThat(enviarPut(eliminada.getId(), cuerpo).statusCode()).isEqualTo(404);
        assertThat(enviarPut(999999L, cuerpo).statusCode()).isEqualTo(404);
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

    private HttpResponse<String> enviarPost(String cuerpoJson) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + puerto + "/api/opciones-respuesta"))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpoJson, StandardCharsets.UTF_8))
                .build();
        return CLIENTE.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> enviarPut(Long id, String cuerpoJson) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + puerto + "/api/opciones-respuesta/" + id))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(cuerpoJson, StandardCharsets.UTF_8))
                .build();
        return CLIENTE.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
