package com.deso.api.service;

import com.deso.api.dao.OpcionRespuestaDAO;
import com.deso.api.dto.ItemOpcionDTO;
import com.deso.api.dto.ListadoOpcionRespuestaDTO;
import com.deso.api.dto.OpcionRespuestaDTO;
import com.deso.api.exception.RecursoActualmenteEliminadoException;
import com.deso.api.exception.RecursoDuplicadoException;
import com.deso.api.exception.RecursoNoEncontradoException;
import com.deso.api.model.entity.ItemOpcion;
import com.deso.api.model.entity.OpcionRespuesta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reglas de negocio de Opciones de Respuesta (CU 24 y consulta de detalle).
 */
class GestorOpcionesRespuestaTests {

    private FakeOpcionRespuestaDAO opcionRespuestaDAO;
    private GestorOpcionesRespuesta gestor;

    @BeforeEach
    void setUp() {
        opcionRespuestaDAO = new FakeOpcionRespuestaDAO();
        gestor = new GestorOpcionesRespuesta(opcionRespuestaDAO);
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
        private long opcionSeq = 1;
        private long itemSeq = 1;

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

        private static boolean contiene(String valor, String buscado) {
            return buscado == null || buscado.isBlank()
                    || valor.toLowerCase(Locale.ROOT).contains(buscado.strip().toLowerCase(Locale.ROOT));
        }
    }
}
