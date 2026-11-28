package com.deso.api.dao;

import com.deso.api.dao.impl.OpcionRespuestaPostgreSQLDAO;
import com.deso.api.model.entity.Competencia;
import com.deso.api.model.entity.Factor;
import com.deso.api.model.entity.ItemOpcion;
import com.deso.api.model.entity.OpcionRespuesta;
import com.deso.api.model.entity.Pregunta;
import com.deso.api.model.enums.TipoCompetencia;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.file.name=target/opcion-respuesta-dao-tests.log"
})
@Import({OpcionRespuestaPostgreSQLDAO.class, PersistenceExceptionTranslationPostProcessor.class})
class DAOPostgreSQLOpcionRespuestaTests {

    @Autowired
    private OpcionRespuestaDAO opcionRespuestaDAO;

    @Autowired
    private EntityManager entityManager;

    @Test
    void guardaYBuscaPorIdConItemsOrdenadosAscendentementeSinFiltrarBajaLogica() {
        OpcionRespuesta activa = opcionRespuestaDAO.guardar(
                opcion("ESCALA_DESEMPENO", "Cinco niveles valorativos", false,
                        item(3, "Excelente"),
                        item(1, "Malo"),
                        item(2, "Regular")));
        OpcionRespuesta eliminadaMismoNombre = opcionRespuestaDAO.guardar(
                opcion("ESCALA_DESEMPENO", "Versión histórica dada de baja", true,
                        item(2, "No"),
                        item(1, "Sí")));
        limpiar();

        OpcionRespuesta encontrada = opcionRespuestaDAO.buscarPorId(activa.getId()).orElseThrow();
        assertThat(encontrada.getNombre()).isEqualTo("ESCALA_DESEMPENO");
        assertThat(encontrada.getDescripcion()).isEqualTo("Cinco niveles valorativos");
        assertThat(encontrada.getEliminada()).isFalse();
        assertThat(Hibernate.isInitialized(encontrada.getItems())).isTrue();
        assertThat(encontrada.getItems())
                .extracting(ItemOpcion::getOrden)
                .containsExactly(1, 2, 3);
        assertThat(encontrada.getItems())
                .extracting(ItemOpcion::getDescripcion)
                .containsExactly("Malo", "Regular", "Excelente");

        OpcionRespuesta bajaEncontrada = opcionRespuestaDAO.buscarPorId(eliminadaMismoNombre.getId()).orElseThrow();
        assertThat(bajaEncontrada.getEliminada()).isTrue();
        assertThat(bajaEncontrada.getItems())
                .extracting(ItemOpcion::getOrden)
                .containsExactly(1, 2);

        assertThat(opcionRespuestaDAO.buscarPorId(null)).isEmpty();
        assertThat(opcionRespuestaDAO.buscarPorId(9999L)).isEmpty();
    }

    @Test
    void exigeDescripcionNoNulaEnOpcionRespuesta() {
        OpcionRespuesta sinDescripcion = opcion("VERDADERO_FALSO", null, false,
                item(1, "Verdadero"),
                item(2, "Falso"));

        assertThatThrownBy(() -> {
            opcionRespuestaDAO.guardar(sinDescripcion);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void guardarActualizaUnaOpcionRespuestaExistente() {
        OpcionRespuesta creada = opcionRespuestaDAO.guardar(
                opcion("VERDADERO_FALSO", "Descripción inicial", false,
                        item(1, "Verdadero"),
                        item(2, "Falso")));
        limpiar();

        OpcionRespuesta cargada = opcionRespuestaDAO.buscarPorId(creada.getId()).orElseThrow();
        cargada.setDescripcion("Descripción actualizada");
        opcionRespuestaDAO.guardar(cargada);
        limpiar();

        assertThat(opcionRespuestaDAO.buscarPorId(creada.getId()).orElseThrow().getDescripcion())
                .isEqualTo("Descripción actualizada");
    }

    @Test
    void buscarActivasPorNombreFiltraSinImportarMayusculasExcluyeEliminadasYOrdenaPorNombreEId() {
        opcionRespuestaDAO.guardar(opcion("VERDADERO_FALSO", "Dicotómica", false,
                item(1, "Verdadero"), item(2, "Falso")));
        opcionRespuestaDAO.guardar(opcion("ESCALA_DESEMPENO", "Cinco niveles", false,
                item(1, "Malo"), item(2, "Bueno")));
        opcionRespuestaDAO.guardar(opcion("Escala Frecuencia", "Tres niveles", false,
                item(1, "Nunca"), item(2, "Siempre")));
        opcionRespuestaDAO.guardar(opcion("ESCALA_BAJA", "Eliminada lógicamente", true,
                item(1, "Uno"), item(2, "Dos")));
        limpiar();

        assertThat(opcionRespuestaDAO.buscarActivasPorNombre(null, 0, 50))
                .extracting(OpcionRespuesta::getNombre)
                .containsExactly("ESCALA_DESEMPENO", "Escala Frecuencia", "VERDADERO_FALSO");
        assertThat(opcionRespuestaDAO.buscarActivasPorNombre("   ", 0, 50))
                .hasSize(3);
        assertThat(opcionRespuestaDAO.buscarActivasPorNombre("  escala ", 0, 50))
                .extracting(OpcionRespuesta::getNombre)
                .containsExactly("ESCALA_DESEMPENO", "Escala Frecuencia");
    }

    @Test
    void buscarActivasPorNombreTrataComodinesComoTextoLiteral() {
        opcionRespuestaDAO.guardar(opcion("CUMPLIMIENTO 100%", "Porcentaje exacto", false,
                item(1, "Sí"), item(2, "No")));
        opcionRespuestaDAO.guardar(opcion("CUMPLIMIENTO 1000", "Sin signo porcentaje", false,
                item(1, "Sí"), item(2, "No")));
        opcionRespuestaDAO.guardar(opcion("OPCION_CLAVE", "Con guion bajo", false,
                item(1, "Sí"), item(2, "No")));
        opcionRespuestaDAO.guardar(opcion("OPCIONXCLAVE", "Sin guion bajo", false,
                item(1, "Sí"), item(2, "No")));
        opcionRespuestaDAO.guardar(opcion("ALERTA!ALTA", "Con exclamación", false,
                item(1, "Sí"), item(2, "No")));
        opcionRespuestaDAO.guardar(opcion("ALERTAALTA", "Sin exclamación", false,
                item(1, "Sí"), item(2, "No")));
        limpiar();

        assertThat(opcionRespuestaDAO.buscarActivasPorNombre("100%", 0, 50))
                .extracting(OpcionRespuesta::getNombre)
                .containsExactly("CUMPLIMIENTO 100%");
        assertThat(opcionRespuestaDAO.buscarActivasPorNombre("n_c", 0, 50))
                .extracting(OpcionRespuesta::getNombre)
                .containsExactly("OPCION_CLAVE");
        assertThat(opcionRespuestaDAO.buscarActivasPorNombre("!a", 0, 50))
                .extracting(OpcionRespuesta::getNombre)
                .containsExactly("ALERTA!ALTA");
    }

    @Test
    void buscarActivasPorNombrePaginaConOffsetYCantidad() {
        for (int i = 1; i <= 5; i++) {
            opcionRespuestaDAO.guardar(opcion("OPCION_0" + i, "Descripción " + i, false,
                    item(1, "Sí"), item(2, "No")));
        }
        limpiar();

        assertThat(opcionRespuestaDAO.buscarActivasPorNombre(null, 0, 2))
                .extracting(OpcionRespuesta::getNombre)
                .containsExactly("OPCION_01", "OPCION_02");
        assertThat(opcionRespuestaDAO.buscarActivasPorNombre(null, 4, 2))
                .extracting(OpcionRespuesta::getNombre)
                .containsExactly("OPCION_05");
    }

    @Test
    void existeActivaPorNombreIgnoraMayusculasEspaciosYExcluyeEliminadas() {
        opcionRespuestaDAO.guardar(opcion("VERDADERO_FALSO", "Activa", false,
                item(1, "Verdadero"), item(2, "Falso")));
        opcionRespuestaDAO.guardar(opcion("ESCALA_HISTORICA", "Dada de baja", true,
                item(1, "Uno"), item(2, "Dos")));
        limpiar();

        assertThat(opcionRespuestaDAO.existeActivaPorNombre("VERDADERO_FALSO")).isTrue();
        assertThat(opcionRespuestaDAO.existeActivaPorNombre("  verdadero_falso  ")).isTrue();
        assertThat(opcionRespuestaDAO.existeActivaPorNombre("ESCALA_HISTORICA")).isFalse();
        assertThat(opcionRespuestaDAO.existeActivaPorNombre("  escala_historica ")).isFalse();
        assertThat(opcionRespuestaDAO.existeActivaPorNombre("INEXISTENTE")).isFalse();
        assertThat(opcionRespuestaDAO.existeActivaPorNombre("   ")).isFalse();
        assertThat(opcionRespuestaDAO.existeActivaPorNombre(null)).isFalse();
    }

    @Test
    void estaEnUsoConsideraSoloPreguntasNoEliminadasYPermiteBajaLogicaCoexistiendoNombresEliminados() {
        OpcionRespuesta usadaPorActiva = opcionRespuestaDAO.guardar(opcion("USADA_ACTIVA", "Con pregunta activa", false,
                item(1, "Sí"), item(2, "No")));
        OpcionRespuesta usadaSoloPorEliminada = opcionRespuestaDAO.guardar(opcion("ESCALA_REPETIDA", "Solo pregunta eliminada", false,
                item(1, "Sí"), item(2, "No")));
        OpcionRespuesta libre = opcionRespuestaDAO.guardar(opcion("LIBRE", "Sin preguntas", false,
                item(1, "Sí"), item(2, "No")));
        OpcionRespuesta historicaMismoNombre = opcionRespuestaDAO.guardar(opcion("ESCALA_REPETIDA", "Histórica ya eliminada", true,
                item(1, "Uno"), item(2, "Dos")));

        Factor factor = crearFactorBase();
        entityManager.persist(new Pregunta(null, factor, usadaPorActiva, "P_ACTIVA", "¿Enunciado activo?", null, true, false));
        entityManager.persist(new Pregunta(null, factor, usadaSoloPorEliminada, "P_BAJA", "¿Enunciado eliminado?", null, true, true));
        limpiar();

        assertThat(opcionRespuestaDAO.estaEnUso(usadaPorActiva.getId())).isTrue();
        assertThat(opcionRespuestaDAO.estaEnUso(usadaSoloPorEliminada.getId())).isFalse();
        assertThat(opcionRespuestaDAO.estaEnUso(libre.getId())).isFalse();
        assertThat(opcionRespuestaDAO.estaEnUso(99999L)).isFalse();
        assertThat(opcionRespuestaDAO.estaEnUso(null)).isFalse();

        opcionRespuestaDAO.eliminarLogicamente(usadaSoloPorEliminada.getId());
        limpiar();

        assertThat(opcionRespuestaDAO.buscarPorId(usadaSoloPorEliminada.getId()).orElseThrow().getEliminada()).isTrue();
        assertThat(opcionRespuestaDAO.buscarPorId(historicaMismoNombre.getId()).orElseThrow().getEliminada()).isTrue();
    }

    private Factor crearFactorBase() {
        Competencia competencia = new Competencia(null, "COMP_01", "Competencia Base", "Desc", TipoCompetencia.TECNICA, false);
        entityManager.persist(competencia);
        Factor factor = new Factor(null, competencia, "FAC_01", "Factor Base", 1, "Desc", false);
        entityManager.persist(factor);
        return factor;
    }

    private static OpcionRespuesta opcion(String nombre, String descripcion, boolean eliminada, ItemOpcion... items) {
        OpcionRespuesta opcion = new OpcionRespuesta(null, nombre, descripcion, eliminada, new ArrayList<>());
        for (ItemOpcion item : items) {
            item.setOpcionRespuesta(opcion);
            opcion.getItems().add(item);
        }
        return opcion;
    }

    private static ItemOpcion item(int orden, String descripcion) {
        return new ItemOpcion(null, null, orden, descripcion);
    }

    private void limpiar() {
        entityManager.flush();
        entityManager.clear();
    }
}
