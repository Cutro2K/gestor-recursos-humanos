package com.deso.api.service;

import com.deso.api.dao.AuditoriaDAO;
import com.deso.api.dao.ConsultorDAO;
import com.deso.api.dao.OpcionRespuestaDAO;
import com.deso.api.dto.ItemOpcionDTO;
import com.deso.api.dto.ListadoOpcionRespuestaDTO;
import com.deso.api.dto.OpcionRespuestaDTO;
import com.deso.api.exception.CredencialesInvalidasException;
import com.deso.api.exception.RecursoActualmenteEliminadoException;
import com.deso.api.exception.RecursoDuplicadoException;
import com.deso.api.exception.RecursoEnUsoException;
import com.deso.api.exception.RecursoNoEncontradoException;
import com.deso.api.model.entity.Consultor;
import com.deso.api.model.entity.ItemOpcion;
import com.deso.api.model.entity.OpcionRespuesta;
import com.deso.api.model.entity.RegistroAuditoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reglas de negocio de Opciones de Respuesta (CU 24 y consulta de detalle).
 */
class GestorOpcionesRespuestaTests {

    private FakeOpcionRespuestaDAO opcionRespuestaDAO;
    private FakeConsultorDAO consultorDAO;
    private FakeAuditoriaDAO auditoriaDAO;
    private GestorOpcionesRespuesta gestor;

    @BeforeEach
    void setUp() {
        opcionRespuestaDAO = new FakeOpcionRespuestaDAO();
        consultorDAO = new FakeConsultorDAO();
        auditoriaDAO = new FakeAuditoriaDAO();
        gestor = new GestorOpcionesRespuesta(opcionRespuestaDAO, consultorDAO, auditoriaDAO);
    }

    @Test
    void buscarPorNombreDevuelveListadoDeActivasConIdNombreYDescripcion() {
        guardar("VERDADERO_FALSO", "Opciones dicotómicas", false,
                item(1, "Verdadero"), item(2, "Falso"));
        guardar("ESCALA_DESEMPENO", "Cinco niveles valorativos", false,
                item(1, "Malo"), item(2, "Excelente"));
        guardar("ESCALA_VIEJA", "Eliminada", true,
                item(1, "Uno"), item(2, "Dos"));

        List<ListadoOpcionRespuestaDTO> listado = gestor.buscarPorNombre("escala", 0, 50);

        assertThat(listado).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isPositive();
            assertThat(dto.getNombre()).isEqualTo("ESCALA_DESEMPENO");
            assertThat(dto.getDescripcion()).isEqualTo("Cinco niveles valorativos");
        });
    }

    @Test
    void buscarPorIdDevuelveOpcionActivaConSusItemsOrdenadosPorOrdenAscendente() {
        OpcionRespuesta guardada = guardar("ESCALA_FRECUENCIA", "Tres niveles de frecuencia", false,
                item(3, "Siempre"),
                item(1, "Nunca"),
                item(2, "A veces"));

        OpcionRespuestaDTO detalle = gestor.buscarPorId(guardada.getId());

        assertThat(detalle.getId()).isEqualTo(guardada.getId());
        assertThat(detalle.getNombre()).isEqualTo("ESCALA_FRECUENCIA");
        assertThat(detalle.getDescripcion()).isEqualTo("Tres niveles de frecuencia");
        assertThat(detalle.getItems())
                .extracting(ItemOpcionDTO::getOrden)
                .containsExactly(1, 2, 3);
        assertThat(detalle.getItems())
                .extracting(ItemOpcionDTO::getDescripcion)
                .containsExactly("Nunca", "A veces", "Siempre");
        assertThat(detalle.getItems()).allSatisfy(i -> assertThat(i.getId()).isPositive());
    }

    @Test
    void buscarPorIdFallaSiNoExisteOEstaDadaDeBajaLogica() {
        OpcionRespuesta baja = guardar("OPCION_BAJA", "Dada de baja", true,
                item(1, "Sí"), item(2, "No"));

        assertThatThrownBy(() -> gestor.buscarPorId(999L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> gestor.buscarPorId(baja.getId()))
                .isInstanceOf(RecursoActualmenteEliminadoException.class);
    }

    @Test
    void darDeAltaNormalizaTextosOrdenaItemsPorOrdenAscendenteYPersisteComoActiva() {
        OpcionRespuestaDTO solicitud = new OpcionRespuestaDTO(
                null,
                "  ESCALA_DESEMPENO  ",
                "  Cinco niveles valorativos  ",
                List.of(
                        new ItemOpcionDTO(null, 3, "  Excelente  "),
                        new ItemOpcionDTO(null, 1, "  Malo  "),
                        new ItemOpcionDTO(null, 2, "  Regular  ")
                )
        );

        OpcionRespuestaDTO creada = gestor.darDeAlta(solicitud);

        assertThat(creada.getId()).isPositive();
        assertThat(creada.getNombre()).isEqualTo("ESCALA_DESEMPENO");
        assertThat(creada.getDescripcion()).isEqualTo("Cinco niveles valorativos");
        assertThat(creada.getItems())
                .extracting(ItemOpcionDTO::getOrden)
                .containsExactly(1, 2, 3);
        assertThat(creada.getItems())
                .extracting(ItemOpcionDTO::getDescripcion)
                .containsExactly("Malo", "Regular", "Excelente");
        assertThat(creada.getItems()).allSatisfy(i -> assertThat(i.getId()).isPositive());

        OpcionRespuesta persistida = opcionRespuestaDAO.buscarPorId(creada.getId()).orElseThrow();
        assertThat(persistida.getEliminada()).isFalse();
        assertThat(persistida.getItems()).allSatisfy(i -> assertThat(i.getOpcionRespuesta()).isSameAs(persistida));
    }

    @Test
    void darDeAltaRechazaConRecursoDuplicadoEnCampoNombreSiYaExisteOtraActiva() {
        guardar("VERDADERO_FALSO", "Opciones dicotómicas", false,
                item(1, "Verdadero"), item(2, "Falso"));

        OpcionRespuestaDTO duplicada = new OpcionRespuestaDTO(
                null,
                "  verdadero_falso ",
                "Otra descripción",
                List.of(
                        new ItemOpcionDTO(null, 1, "Sí"),
                        new ItemOpcionDTO(null, 2, "No")
                )
        );

        assertThatThrownBy(() -> gestor.darDeAlta(duplicada))
                .isInstanceOfSatisfying(RecursoDuplicadoException.class, ex -> {
                    assertThat(ex.getCampo()).isEqualTo("nombre");
                    assertThat(ex.getMessage()).isNotBlank();
                });
    }

    @Test
    void darDeAltaPermiteReutilizarNombreDeOpcionRespuestaEliminadaLogicamente() {
        OpcionRespuesta historica = guardar("VERDADERO_FALSO", "Versión histórica dada de baja", true,
                item(1, "Verdadero"), item(2, "Falso"));

        OpcionRespuestaDTO nueva = new OpcionRespuestaDTO(
                null,
                "  VERDADERO_FALSO  ",
                "Nueva versión activa",
                List.of(
                        new ItemOpcionDTO(null, 2, "Falso"),
                        new ItemOpcionDTO(null, 1, "Verdadero")
                )
        );

        OpcionRespuestaDTO creada = gestor.darDeAlta(nueva);

        assertThat(creada.getId()).isNotEqualTo(historica.getId());
        assertThat(creada.getNombre()).isEqualTo("VERDADERO_FALSO");
        assertThat(creada.getDescripcion()).isEqualTo("Nueva versión activa");
        assertThat(gestor.buscarPorNombre("VERDADERO_FALSO", 0, 50))
                .singleElement()
                .extracting(ListadoOpcionRespuestaDTO::getId)
                .isEqualTo(creada.getId());
    }

    @Test
    void actualizarModificaDescripcionEItemsInPlacePreservandoIdsYNombreYOrdenandoAscendentemente() {
        OpcionRespuesta original = guardar("ESCALA_DESEMPENO", "Descripción inicial", false,
                item(1, "Malo"),
                item(2, "Regular"),
                item(3, "Bueno"));
        ItemOpcion itemMalo = original.getItems().get(0);
        ItemOpcion itemRegular = original.getItems().get(1);
        ItemOpcion itemBueno = original.getItems().get(2);

        OpcionRespuestaDTO solicitud = new OpcionRespuestaDTO(
                original.getId(),
                null,
                "  Tres niveles actualizados  ",
                List.of(
                        new ItemOpcionDTO(itemMalo.getId(), 3, "  Excelente  "),
                        new ItemOpcionDTO(itemRegular.getId(), 1, "  Insuficiente  "),
                        new ItemOpcionDTO(itemBueno.getId(), 2, "  Aceptable  ")
                )
        );

        OpcionRespuestaDTO actualizada = gestor.actualizar(solicitud);

        assertThat(actualizada.getId()).isEqualTo(original.getId());
        assertThat(actualizada.getNombre()).isEqualTo("ESCALA_DESEMPENO");
        assertThat(actualizada.getDescripcion()).isEqualTo("Tres niveles actualizados");
        assertThat(actualizada.getItems())
                .extracting(ItemOpcionDTO::getId)
                .containsExactly(itemRegular.getId(), itemBueno.getId(), itemMalo.getId());
        assertThat(actualizada.getItems())
                .extracting(ItemOpcionDTO::getOrden)
                .containsExactly(1, 2, 3);
        assertThat(actualizada.getItems())
                .extracting(ItemOpcionDTO::getDescripcion)
                .containsExactly("Insuficiente", "Aceptable", "Excelente");

        assertThat(itemRegular.getOrden()).isEqualTo(1);
        assertThat(itemRegular.getDescripcion()).isEqualTo("Insuficiente");
        assertThat(itemBueno.getOrden()).isEqualTo(2);
        assertThat(itemBueno.getDescripcion()).isEqualTo("Aceptable");
        assertThat(itemMalo.getOrden()).isEqualTo(3);
        assertThat(itemMalo.getDescripcion()).isEqualTo("Excelente");

        OpcionRespuestaDTO conNombreIgual = gestor.actualizar(new OpcionRespuestaDTO(
                original.getId(),
                "  ESCALA_DESEMPENO  ",
                "Descripción con nombre igual",
                actualizada.getItems()
        ));
        assertThat(conNombreIgual.getNombre()).isEqualTo("ESCALA_DESEMPENO");
        assertThat(conNombreIgual.getDescripcion()).isEqualTo("Descripción con nombre igual");
    }

    @Test
    void actualizarFallaSiOpcionRespuestaNoExisteOEstaDadaDeBajaLogica() {
        OpcionRespuesta eliminada = guardar("OPCION_BAJA", "Dada de baja", true,
                item(1, "Sí"),
                item(2, "No"));

        OpcionRespuestaDTO inexistente = new OpcionRespuestaDTO(
                999L,
                null,
                "Nueva descripción",
                List.of(new ItemOpcionDTO(1L, 1, "Sí"), new ItemOpcionDTO(2L, 2, "No"))
        );
        OpcionRespuestaDTO sobreEliminada = new OpcionRespuestaDTO(
                eliminada.getId(),
                null,
                "Nueva descripción",
                List.of(
                        new ItemOpcionDTO(eliminada.getItems().get(0).getId(), 1, "Sí"),
                        new ItemOpcionDTO(eliminada.getItems().get(1).getId(), 2, "No")
                )
        );

        assertThatThrownBy(() -> gestor.actualizar(inexistente))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> gestor.actualizar(sobreEliminada))
                .isInstanceOf(RecursoActualmenteEliminadoException.class);
    }

    @Test
    void actualizarConservaNombreSiEsBlancoORechazaConIllegalArgumentSiIntentaCambiarse() {
        OpcionRespuesta original = guardar("VERDADERO_FALSO", "Descripción inicial", false,
                item(1, "Verdadero"),
                item(2, "Falso"));
        List<ItemOpcionDTO> items = List.of(
                new ItemOpcionDTO(original.getItems().get(0).getId(), 1, "Verdadero"),
                new ItemOpcionDTO(original.getItems().get(1).getId(), 2, "Falso")
        );

        OpcionRespuestaDTO conNombreBlanco = gestor.actualizar(new OpcionRespuestaDTO(
                original.getId(), "   ", "Descripción con nombre blanco", items));
        assertThat(conNombreBlanco.getNombre()).isEqualTo("VERDADERO_FALSO");

        assertThatThrownBy(() -> gestor.actualizar(new OpcionRespuestaDTO(
                original.getId(), "OTRO_NOMBRE", "Intento cambio nombre", items)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> gestor.actualizar(new OpcionRespuestaDTO(
                original.getId(), "verdadero_falso", "Intento cambio mayúsculas", items)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(gestor.buscarPorId(original.getId()).getNombre()).isEqualTo("VERDADERO_FALSO");
    }

    @Test
    void actualizarRechazaAgregarQuitarRepetirOEnviarIdsAjenosDeItemsSinAlterarElEstado() {
        OpcionRespuesta original = guardar("ESCALA_TRES", "Tres niveles", false,
                item(1, "Bajo"),
                item(2, "Medio"),
                item(3, "Alto"));
        OpcionRespuesta otra = guardar("OTRA_OPCION", "Otra plantilla", false,
                item(1, "Sí"),
                item(2, "No"));

        Long id1 = original.getItems().get(0).getId();
        Long id2 = original.getItems().get(1).getId();
        Long id3 = original.getItems().get(2).getId();
        Long idAjeno = otra.getItems().get(0).getId();

        // Quitar un ítem (quedan 2 en vez de 3)
        assertThatThrownBy(() -> gestor.actualizar(new OpcionRespuestaDTO(
                original.getId(), null, "Menos ítems",
                List.of(new ItemOpcionDTO(id1, 1, "Bajo"), new ItemOpcionDTO(id2, 2, "Medio")))))
                .isInstanceOf(IllegalArgumentException.class);

        // Agregar un ítem (4 en vez de 3)
        assertThatThrownBy(() -> gestor.actualizar(new OpcionRespuestaDTO(
                original.getId(), null, "Más ítems",
                List.of(
                        new ItemOpcionDTO(id1, 1, "Bajo"),
                        new ItemOpcionDTO(id2, 2, "Medio"),
                        new ItemOpcionDTO(id3, 3, "Alto"),
                        new ItemOpcionDTO(999L, 4, "Muy alto")))))
                .isInstanceOf(IllegalArgumentException.class);

        // Repetir un ID manteniendo la cantidad
        assertThatThrownBy(() -> gestor.actualizar(new OpcionRespuestaDTO(
                original.getId(), null, "ID repetido",
                List.of(
                        new ItemOpcionDTO(id1, 1, "Bajo"),
                        new ItemOpcionDTO(id1, 2, "Medio"),
                        new ItemOpcionDTO(id2, 3, "Alto")))))
                .isInstanceOf(IllegalArgumentException.class);

        // Enviar un ID perteneciente a otra opción de respuesta manteniendo la cantidad
        assertThatThrownBy(() -> gestor.actualizar(new OpcionRespuestaDTO(
                original.getId(), null, "ID ajeno",
                List.of(
                        new ItemOpcionDTO(id1, 1, "Bajo"),
                        new ItemOpcionDTO(id2, 2, "Medio"),
                        new ItemOpcionDTO(idAjeno, 3, "Alto")))))
                .isInstanceOf(IllegalArgumentException.class);

        // Enviar un ID nulo
        assertThatThrownBy(() -> gestor.actualizar(new OpcionRespuestaDTO(
                original.getId(), null, "ID nulo",
                List.of(
                        new ItemOpcionDTO(id1, 1, "Bajo"),
                        new ItemOpcionDTO(id2, 2, "Medio"),
                        new ItemOpcionDTO(null, 3, "Alto")))))
                .isInstanceOf(IllegalArgumentException.class);

        OpcionRespuestaDTO intacta = gestor.buscarPorId(original.getId());
        assertThat(intacta.getDescripcion()).isEqualTo("Tres niveles");
        assertThat(intacta.getItems())
                .extracting(ItemOpcionDTO::getDescripcion)
                .containsExactly("Bajo", "Medio", "Alto");
    }

    @Test
    void eliminarLogicamenteMarcaEliminadaAunqueExistaOtraEliminadaConMismoNombreYRegistraAuditoria() {
        Consultor consultor = guardarConsultor("CONSULTOR1", true);
        OpcionRespuesta historica = guardar("VERDADERO_FALSO", "Versión previa dada de baja", true,
                item(1, "Verdadero"), item(2, "Falso"));
        OpcionRespuesta activa = guardar("VERDADERO_FALSO", "Versión activa a eliminar", false,
                item(1, "Verdadero"), item(2, "Falso"));

        LocalDateTime antes = LocalDateTime.now();
        gestor.eliminarLogicamente(activa.getId(), consultor.getId());
        LocalDateTime despues = LocalDateTime.now();

        assertThat(opcionRespuestaDAO.buscarPorId(activa.getId()).orElseThrow().getEliminada()).isTrue();
        assertThat(opcionRespuestaDAO.buscarPorId(historica.getId()).orElseThrow().getEliminada()).isTrue();
        assertThat(gestor.buscarPorNombre("VERDADERO_FALSO", 0, 50)).isEmpty();

        assertThat(auditoriaDAO.registros()).singleElement().satisfies(registro -> {
            assertThat(registro.getId()).isPositive();
            assertThat(registro.getConsultor()).isSameAs(consultor);
            assertThat(registro.getFechaHora()).isBetween(antes, despues);
            assertThat(registro.getAccion()).isEqualTo("BAJA");
            assertThat(registro.getEntidad()).isEqualTo("OPCION_RESPUESTA");
            assertThat(registro.getEntidadId()).isEqualTo(activa.getId());
            assertThat(registro.getDetalle()).contains("VERDADERO_FALSO");
        });
    }

    @Test
    void eliminarLogicamenteRechazaConCredencialesInvalidasSiConsultorNoExisteOEstaInactivo() {
        Consultor inactivo = guardarConsultor("CONSULTOR_BAJA", false);
        OpcionRespuesta activa = guardar("ESCALA_DESEMPENO", "Activa", false,
                item(1, "Malo"), item(2, "Bueno"));

        assertThatThrownBy(() -> gestor.eliminarLogicamente(activa.getId(), null))
                .isInstanceOf(CredencialesInvalidasException.class);
        assertThatThrownBy(() -> gestor.eliminarLogicamente(activa.getId(), 999L))
                .isInstanceOf(CredencialesInvalidasException.class);
        assertThatThrownBy(() -> gestor.eliminarLogicamente(activa.getId(), inactivo.getId()))
                .isInstanceOf(CredencialesInvalidasException.class);

        assertThat(opcionRespuestaDAO.buscarPorId(activa.getId()).orElseThrow().getEliminada()).isFalse();
        assertThat(auditoriaDAO.registros()).isEmpty();
    }

    @Test
    void eliminarLogicamenteFallaSiOpcionRespuestaNoExisteOYaEstaEliminada() {
        Consultor consultor = guardarConsultor("CONSULTOR1", true);
        OpcionRespuesta eliminada = guardar("OPCION_BAJA", "Dada de baja", true,
                item(1, "Sí"), item(2, "No"));

        assertThatThrownBy(() -> gestor.eliminarLogicamente(999L, consultor.getId()))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> gestor.eliminarLogicamente(eliminada.getId(), consultor.getId()))
                .isInstanceOf(RecursoActualmenteEliminadoException.class);

        assertThat(auditoriaDAO.registros()).isEmpty();
    }

    @Test
    void eliminarLogicamenteRechazaConRecursoEnUsoYMensajeOficialSiTienePreguntasActivas() {
        Consultor consultor = guardarConsultor("CONSULTOR1", true);
        OpcionRespuesta enUso = guardar("ESCALA_LIKERT", "Cinco niveles", false,
                item(1, "Totalmente en desacuerdo"), item(2, "Totalmente de acuerdo"));
        opcionRespuestaDAO.marcarEnUso(enUso.getId());

        assertThatThrownBy(() -> gestor.eliminarLogicamente(enUso.getId(), consultor.getId()))
                .isInstanceOf(RecursoEnUsoException.class)
                .hasMessage("La opción de respuesta ESCALA_LIKERT está siendo usada en la base de datos y no puede eliminarse.");

        assertThat(opcionRespuestaDAO.buscarPorId(enUso.getId()).orElseThrow().getEliminada()).isFalse();
        assertThat(auditoriaDAO.registros()).isEmpty();
    }

    @Test
    void darDeAltaRechazaOrdenNoConsecutivoODescripcionRepetidaSinPersistir() {
        List<List<ItemOpcionDTO>> invalidas = List.of(
                List.of(new ItemOpcionDTO(null, 2, "A"), new ItemOpcionDTO(null, 3, "B")),
                List.of(new ItemOpcionDTO(null, 1, "A"), new ItemOpcionDTO(null, 3, "B")),
                List.of(new ItemOpcionDTO(null, 1, "A"), new ItemOpcionDTO(null, 1, "B")));
        for (List<ItemOpcionDTO> items : invalidas) {
            assertThatThrownBy(() -> gestor.darDeAlta(new OpcionRespuestaDTO(null, "ESCALA", "Desc", items)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("consecutivo");
        }

        OpcionRespuestaDTO repetida = new OpcionRespuestaDTO(null, "ESCALA", "Desc", List.of(
                new ItemOpcionDTO(null, 1, "Verdadero"),
                new ItemOpcionDTO(null, 2, "  verdadero  ")));
        assertThatThrownBy(() -> gestor.darDeAlta(repetida))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede repetirse");

        assertThat(gestor.buscarPorNombre(null, 0, 50)).isEmpty();
    }

    @Test
    void actualizarRechazaOrdenNoConsecutivoODescripcionRepetidaSinAlterarElEstado() {
        OpcionRespuesta existente = guardar("VERDADERO_FALSO", "Dicotómica", false,
                item(1, "Verdadero"), item(2, "Falso"));
        Long idPrimero = existente.getItems().get(0).getId();
        Long idSegundo = existente.getItems().get(1).getId();

        OpcionRespuestaDTO conSalto = new OpcionRespuestaDTO(existente.getId(), null, "Nueva", List.of(
                new ItemOpcionDTO(idPrimero, 1, "Verdadero"),
                new ItemOpcionDTO(idSegundo, 3, "Falso")));
        assertThatThrownBy(() -> gestor.actualizar(conSalto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consecutivo");

        OpcionRespuestaDTO repetida = new OpcionRespuestaDTO(existente.getId(), null, "Nueva", List.of(
                new ItemOpcionDTO(idPrimero, 1, "Falso"),
                new ItemOpcionDTO(idSegundo, 2, "FALSO")));
        assertThatThrownBy(() -> gestor.actualizar(repetida))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede repetirse");

        OpcionRespuestaDTO detalle = gestor.buscarPorId(existente.getId());
        assertThat(detalle.getDescripcion()).isEqualTo("Dicotómica");
        assertThat(detalle.getItems()).extracting(ItemOpcionDTO::getDescripcion)
                .containsExactly("Verdadero", "Falso");
    }

    @Test
    void estaEnUsoIndicaSiTienePreguntasActivasYFallaSiNoExisteOEstaEliminada() {
        OpcionRespuesta libre = guardar("LIBRE", "Sin preguntas", false, item(1, "Sí"), item(2, "No"));
        OpcionRespuesta usada = guardar("USADA", "Con preguntas", false, item(1, "Sí"), item(2, "No"));
        OpcionRespuesta baja = guardar("BAJA", "Eliminada", true, item(1, "Sí"), item(2, "No"));
        opcionRespuestaDAO.marcarEnUso(usada.getId());

        assertThat(gestor.estaEnUso(libre.getId())).isFalse();
        assertThat(gestor.estaEnUso(usada.getId())).isTrue();
        assertThatThrownBy(() -> gestor.estaEnUso(999L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> gestor.estaEnUso(baja.getId()))
                .isInstanceOf(RecursoActualmenteEliminadoException.class);
    }

    private Consultor guardarConsultor(String username, boolean activo) {
        return consultorDAO.guardar(new Consultor(
                null,
                username,
                "Ana",
                "Gómez",
                username.toLowerCase(Locale.ROOT) + "@capitalhumano.test",
                activo,
                LocalDateTime.of(2026, 4, 1, 9, 0)));
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

    /** Doble en memoria con la misma semántica que OpcionRespuestaPostgreSQLDAO. */
    private static class FakeOpcionRespuestaDAO implements OpcionRespuestaDAO {
        private final Map<Long, OpcionRespuesta> store = new LinkedHashMap<>();
        private final Set<Long> enUso = new HashSet<>();
        private long opcionSeq = 1;
        private long itemSeq = 1;

        void marcarEnUso(Long id) {
            enUso.add(id);
        }

        @Override
        public Optional<OpcionRespuesta> buscarPorId(Long id) {
            return Optional.ofNullable(id == null ? null : store.get(id));
        }

        @Override
        public boolean existeActivaPorNombre(String nombre) {
            if (nombre == null || nombre.isBlank()) {
                return false;
            }
            String normalizado = nombre.strip().toLowerCase(Locale.ROOT);
            return store.values().stream()
                    .filter(o -> !Boolean.TRUE.equals(o.getEliminada()))
                    .anyMatch(o -> o.getNombre().strip().toLowerCase(Locale.ROOT).equals(normalizado));
        }

        @Override
        public List<OpcionRespuesta> buscarActivasPorNombre(String nombre, Integer offset, Integer cantidad) {
            int desde = (offset != null && offset >= 0) ? offset : 0;
            int max = (cantidad != null && cantidad > 0) ? cantidad : 50;
            return store.values().stream()
                    .filter(o -> !Boolean.TRUE.equals(o.getEliminada()))
                    .filter(o -> contiene(o.getNombre(), nombre))
                    .sorted(Comparator.comparing(OpcionRespuesta::getNombre)
                            .thenComparing(OpcionRespuesta::getId))
                    .skip(desde)
                    .limit(max)
                    .toList();
        }

        @Override
        public boolean estaEnUso(Long id) {
            return id != null && enUso.contains(id);
        }

        @Override
        public OpcionRespuesta guardar(OpcionRespuesta opcionRespuesta) {
            if (opcionRespuesta.getId() == null) {
                opcionRespuesta.setId(opcionSeq++);
            }
            if (opcionRespuesta.getItems() != null) {
                for (ItemOpcion item : opcionRespuesta.getItems()) {
                    if (item.getId() == null) {
                        item.setId(itemSeq++);
                    }
                }
            }
            store.put(opcionRespuesta.getId(), opcionRespuesta);
            return opcionRespuesta;
        }

        @Override
        public void eliminarLogicamente(Long id) {
            OpcionRespuesta opcion = id == null ? null : store.get(id);
            if (opcion != null) {
                opcion.setEliminada(true);
            }
        }

        @Override
        public void sincronizar() {
        }

        private static boolean contiene(String valor, String buscado) {
            return buscado == null || buscado.isBlank()
                    || valor.toLowerCase(Locale.ROOT).contains(buscado.strip().toLowerCase(Locale.ROOT));
        }
    }

    private static class FakeConsultorDAO implements ConsultorDAO {
        private final Map<Long, Consultor> store = new LinkedHashMap<>();
        private long seq = 1;

        @Override
        public Optional<Consultor> buscarPorUsername(String username) {
            if (username == null || username.isBlank()) {
                return Optional.empty();
            }
            return store.values().stream()
                    .filter(c -> Boolean.TRUE.equals(c.getActivo()) && c.getUsername().equals(username.strip()))
                    .findFirst();
        }

        @Override
        public Optional<Consultor> buscarPorId(Long id) {
            return Optional.ofNullable(id == null ? null : store.get(id));
        }

        @Override
        public Optional<Consultor> buscarPorEmail(String email) {
            if (email == null || email.isBlank()) {
                return Optional.empty();
            }
            return store.values().stream()
                    .filter(c -> Boolean.TRUE.equals(c.getActivo()) && c.getEmail().equalsIgnoreCase(email.strip()))
                    .findFirst();
        }

        @Override
        public Consultor guardar(Consultor consultor) {
            if (consultor.getId() == null) {
                consultor.setId(seq++);
            }
            store.put(consultor.getId(), consultor);
            return consultor;
        }
    }

    private static class FakeAuditoriaDAO implements AuditoriaDAO {
        private final List<RegistroAuditoria> store = new ArrayList<>();
        private long seq = 1;

        @Override
        public RegistroAuditoria guardar(RegistroAuditoria registro) {
            if (registro.getId() == null) {
                registro.setId(seq++);
            }
            store.add(registro);
            return registro;
        }

        List<RegistroAuditoria> registros() {
            return List.copyOf(store);
        }
    }
}
