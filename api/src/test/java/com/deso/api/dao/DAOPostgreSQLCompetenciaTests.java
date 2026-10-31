package com.deso.api.dao;

import com.deso.api.dao.impl.CompetenciaPostgreSQLDAO;
import com.deso.api.model.entity.Competencia;
import com.deso.api.model.enums.TipoCompetencia;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "logging.file.name=target/competencia-dao-tests.log"
})
@Import({CompetenciaPostgreSQLDAO.class, PersistenceExceptionTranslationPostProcessor.class})
class DAOPostgreSQLCompetenciaTests {

    @Autowired
    private CompetenciaDAO competenciaDAO;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void springConfiguraCompetenciaDAOComoSingleton() {
        String[] nombres = applicationContext.getBeanNamesForType(CompetenciaDAO.class);
        assertThat(nombres).hasSize(1);
        assertThat(applicationContext.isSingleton(nombres[0])).isTrue();
        assertThat(applicationContext.getBean(nombres[0])).isSameAs(competenciaDAO);
    }

    @Test
    void guardaYBuscaPorId() {
        Competencia c = new Competencia(null, "COMP-TEST1", "Competencia de Prueba", "Descripción", TipoCompetencia.TECNICA, false);
        competenciaDAO.guardar(c);
        entityManager.clear();

        assertThat(c.getId()).isNotNull();
        Competencia recuperada = competenciaDAO.buscarPorId(c.getId()).orElse(null);
        assertThat(recuperada).isNotNull();
        assertThat(recuperada.getCodigo()).isEqualTo("COMP-TEST1");
        assertThat(recuperada.getNombre()).isEqualTo("Competencia de Prueba");
        assertThat(recuperada.getTipo()).isEqualTo(TipoCompetencia.TECNICA);
    }

    @Test
    void verificaExistenciaPorCodigoYNombre() {
        Competencia c = new Competencia(null, "COMP-EX1", "Capacidad Analítica", null, TipoCompetencia.TECNICA, false);
        competenciaDAO.guardar(c);
        entityManager.clear();

        assertThat(competenciaDAO.existePorCodigo("comp-ex1")).isTrue();
        assertThat(competenciaDAO.existePorCodigo("COMP-OTRO")).isFalse();

        assertThat(competenciaDAO.existePorNombre("capacidad analítica")).isTrue();
        assertThat(competenciaDAO.existePorNombre("Inexistente")).isFalse();
    }

    @Test
    void listarActivasFiltraPorNombreCodigoYTipo() {
        Competencia c1 = new Competencia(null, "COMP-A1", "Java Backend", null, TipoCompetencia.TECNICA, false);
        Competencia c2 = new Competencia(null, "COMP-A2", "React Frontend", null, TipoCompetencia.TECNICA, false);
        Competencia c3 = new Competencia(null, "COMP-B1", "Comunicación", null, TipoCompetencia.ACTITUDINAL, false);
        Competencia eliminada = new Competencia(null, "COMP-X1", "Eliminada", null, TipoCompetencia.TECNICA, true);

        competenciaDAO.guardar(c1);
        competenciaDAO.guardar(c2);
        competenciaDAO.guardar(c3);
        competenciaDAO.guardar(eliminada);
        entityManager.clear();

        List<Competencia> activas = competenciaDAO.listarActivas(null, null, null);
        assertThat(activas).extracting(Competencia::getCodigo).contains("COMP-A1", "COMP-A2", "COMP-B1").doesNotContain("COMP-X1");

        List<Competencia> soloTecnicas = competenciaDAO.listarActivas(null, null, TipoCompetencia.TECNICA);
        assertThat(soloTecnicas).extracting(Competencia::getCodigo).contains("COMP-A1", "COMP-A2").doesNotContain("COMP-B1");

        List<Competencia> filtradasNombre = competenciaDAO.listarActivas("frontend", null, null);
        assertThat(filtradasNombre).hasSize(1).extracting(Competencia::getCodigo).containsExactly("COMP-A2");
    }

    @Test
    void eliminarLogicamenteMarcaRegistroComoEliminado() {
        Competencia c = new Competencia(null, "COMP-DEL", "A Eliminar", null, TipoCompetencia.TECNICA, false);
        competenciaDAO.guardar(c);
        entityManager.clear();

        competenciaDAO.eliminarLogicamente(c.getId());
        entityManager.clear();

        Competencia recuperada = competenciaDAO.buscarPorId(c.getId()).orElse(null);
        assertThat(recuperada).isNotNull();
        assertThat(recuperada.getEliminada()).isTrue();
        assertThat(competenciaDAO.listarActivas(null, "COMP-DEL", null)).isEmpty();
    }
}
