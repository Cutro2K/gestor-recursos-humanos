package com.deso.api.dao;

import com.deso.api.dao.impl.FactorPostgreSQLDAO;
import com.deso.api.model.entity.Competencia;
import com.deso.api.model.entity.Factor;
import com.deso.api.model.enums.TipoCompetencia;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        // La app usa ddl-auto=none (el esquema sale de db/01-schema.sql); en H2 lo crea Hibernate.
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.file.name=target/factor-dao-tests.log"
})
@Import({FactorPostgreSQLDAO.class, PersistenceExceptionTranslationPostProcessor.class})
class DAOPostgreSQLFactorTests {

    @Autowired
    private FactorDAO factorDAO;

    @Autowired
    private EntityManager entityManager;

    private Competencia gestion;
    private Competencia liderazgo;

    @BeforeEach
    void setUp() {
        gestion = persistir(new Competencia(null, "COMP-01", "Gestión de proyectos", null, TipoCompetencia.TECNICA, false));
        liderazgo = persistir(new Competencia(null, "COMP-02", "Liderazgo", null, TipoCompetencia.ACTITUDINAL, false));
    }

    @Test
    void guardaYBuscaPorIdIncluyendoLosDadosDeBaja() {
        Factor activo = factorDAO.guardar(factor(gestion, "FAC-01", "Análisis", 1, false));
        Factor baja = factorDAO.guardar(factor(gestion, "FAC-02", "Detalle", 2, true));
        limpiar();

        assertThat(factorDAO.buscarPorId(activo.getId())).get()
                .satisfies(f -> assertThat(f.getNombre()).isEqualTo("Análisis"));
        // buscarPorId no filtra la baja lógica: lo decide el servicio.
        assertThat(factorDAO.buscarPorId(baja.getId())).get()
                .satisfies(f -> assertThat(f.getEliminado()).isTrue());
        assertThat(factorDAO.buscarPorId(null)).isEmpty();
        assertThat(factorDAO.buscarPorId(9999L)).isEmpty();
    }

    @Test
    void guardarActualizaUnFactorExistente() {
        Factor factor = factorDAO.guardar(factor(gestion, "FAC-01", "Análisis", 1, false));
        limpiar();

        Factor cargado = factorDAO.buscarPorId(factor.getId()).orElseThrow();
        cargado.setNombre("Análisis crítico");
        factorDAO.guardar(cargado);
        limpiar();

        assertThat(factorDAO.buscarPorId(factor.getId()).orElseThrow().getNombre()).isEqualTo("Análisis crítico");
    }

    @Test
    void existePorCodigoYNombreIgnoraMayusculasYEspaciosYCuentaLosDadosDeBaja() {
        factorDAO.guardar(factor(gestion, "FAC-01", "Análisis", 1, true));
        limpiar();

        assertThat(factorDAO.existePorCodigo("  fac-01 ")).isTrue();
        assertThat(factorDAO.existePorCodigo("FAC-99")).isFalse();
        assertThat(factorDAO.existePorNombre(" ANÁLISIS ")).isTrue();
        assertThat(factorDAO.existePorNombre("Otro")).isFalse();
    }

    @Test
    void existePorCompetenciaYOrdenSoloCuentaLosActivosDeEsaCompetencia() {
        factorDAO.guardar(factor(gestion, "FAC-01", "Análisis", 1, false));
        factorDAO.guardar(factor(gestion, "FAC-02", "Detalle", 2, true));
        limpiar();

        assertThat(factorDAO.existePorCompetenciaYOrden(gestion.getId(), 1)).isTrue();
        assertThat(factorDAO.existePorCompetenciaYOrden(gestion.getId(), 2)).isFalse();
        assertThat(factorDAO.existePorCompetenciaYOrden(liderazgo.getId(), 1)).isFalse();
    }

    @Test
    void listarActivosPorCompetenciaOrdenaPorOrdenYExcluyeLosDadosDeBaja() {
        factorDAO.guardar(factor(gestion, "FAC-03", "Tercero", 3, false));
        factorDAO.guardar(factor(gestion, "FAC-01", "Primero", 1, false));
        factorDAO.guardar(factor(gestion, "FAC-02", "Baja", 2, true));
        factorDAO.guardar(factor(liderazgo, "FAC-04", "Otra competencia", 1, false));
        limpiar();

        List<Factor> factores = factorDAO.listarActivosPorCompetencia(gestion.getId());

        assertThat(factores).extracting(Factor::getCodigo).containsExactly("FAC-01", "FAC-03");
        assertThat(factores.get(0).getCompetencia().getNombre()).isEqualTo("Gestión de proyectos");
    }

    @Test
    void buscarPorNombresFiltraPorCompetenciaYFactorSinImportarMayusculas() {
        factorDAO.guardar(factor(gestion, "FAC-01", "Capacidad de análisis", 1, false));
        factorDAO.guardar(factor(gestion, "FAC-02", "Atención al detalle", 2, false));
        factorDAO.guardar(factor(liderazgo, "FAC-03", "Análisis del equipo", 1, false));
        factorDAO.guardar(factor(liderazgo, "FAC-04", "Análisis dado de baja", 2, true));
        limpiar();

        assertThat(factorDAO.buscarPorNombres(null, null, 0, 50))
                .extracting(Factor::getCodigo).containsExactly("FAC-03", "FAC-02", "FAC-01");
        assertThat(factorDAO.buscarPorNombres("  GESTIÓN ", null, 0, 50))
                .extracting(Factor::getCodigo).containsExactlyInAnyOrder("FAC-01", "FAC-02");
        assertThat(factorDAO.buscarPorNombres(null, "análisis", 0, 50))
                .extracting(Factor::getCodigo).containsExactly("FAC-03", "FAC-01");
        assertThat(factorDAO.buscarPorNombres("liderazgo", "análisis", 0, 50))
                .extracting(Factor::getCodigo).containsExactly("FAC-03");
        assertThat(factorDAO.buscarPorNombres(" ", "", 0, 50)).hasSize(3);
    }

    @Test
    void buscarPorNombresTrataComodinesComoTextoLiteral() {
        factorDAO.guardar(factor(gestion, "FAC-01", "Avance 100%", 1, false));
        factorDAO.guardar(factor(gestion, "FAC-02", "Avance 1000", 2, false));
        factorDAO.guardar(factor(gestion, "FAC-03", "dato_clave", 3, false));
        factorDAO.guardar(factor(gestion, "FAC-04", "datoXclave", 4, false));
        limpiar();

        assertThat(factorDAO.buscarPorNombres(null, "100%", 0, 50))
                .extracting(Factor::getCodigo).containsExactly("FAC-01");
        assertThat(factorDAO.buscarPorNombres(null, "o_c", 0, 50))
                .extracting(Factor::getCodigo).containsExactly("FAC-03");
    }

    @Test
    void buscarPorNombresPagina() {
        for (int i = 1; i <= 5; i++) {
            factorDAO.guardar(factor(gestion, "FAC-0" + i, "Factor " + i, i, false));
        }
        limpiar();

        assertThat(factorDAO.buscarPorNombres(null, null, 0, 2))
                .extracting(Factor::getCodigo).containsExactly("FAC-01", "FAC-02");
        assertThat(factorDAO.buscarPorNombres(null, null, 4, 2))
                .extracting(Factor::getCodigo).containsExactly("FAC-05");
    }

    @Test
    void eliminarLogicamenteMarcaElFactorSinBorrarlo() {
        Factor factor = factorDAO.guardar(factor(gestion, "FAC-01", "Análisis", 1, false));
        limpiar();

        factorDAO.eliminarLogicamente(factor.getId());
        limpiar();

        assertThat(factorDAO.buscarPorId(factor.getId())).get()
                .satisfies(f -> assertThat(f.getEliminado()).isTrue());
        assertThat(factorDAO.listarActivosPorCompetencia(gestion.getId())).isEmpty();
    }

    private Competencia persistir(Competencia competencia) {
        entityManager.persist(competencia);
        return competencia;
    }

    private static Factor factor(Competencia competencia, String codigo, String nombre, int orden, boolean eliminado) {
        return new Factor(null, competencia, codigo, nombre, orden, null, eliminado);
    }

    private void limpiar() {
        entityManager.flush();
        entityManager.clear();
    }
}
