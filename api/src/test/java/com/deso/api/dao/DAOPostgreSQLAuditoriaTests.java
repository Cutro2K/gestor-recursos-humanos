package com.deso.api.dao;

import com.deso.api.dao.impl.AuditoriaPostgreSQLDAO;
import com.deso.api.model.entity.Consultor;
import com.deso.api.model.entity.RegistroAuditoria;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.file.name=target/auditoria-dao-tests.log"
})
@Import({AuditoriaPostgreSQLDAO.class, PersistenceExceptionTranslationPostProcessor.class})
class DAOPostgreSQLAuditoriaTests {

    @Autowired
    private AuditoriaDAO auditoriaDAO;

    @Autowired
    private EntityManager entityManager;

    private Consultor consultor;

    @BeforeEach
    void setUp() {
        consultor = new Consultor(
                null,
                "CONSULTOR1",
                "Ana",
                "Gómez",
                "ana@capitalhumano.test",
                true,
                LocalDateTime.of(2026, 4, 1, 9, 0));
        entityManager.persist(consultor);
        entityManager.flush();
    }

    @Test
    void guardarPersisteElRegistroDeAuditoriaAsociadoAlConsultorConTodosSusAtributos() {
        LocalDateTime momentoBaja = LocalDateTime.of(2026, 4, 14, 15, 30, 45);
        RegistroAuditoria nuevo = new RegistroAuditoria(
                null,
                consultor,
                momentoBaja,
                "BAJA",
                "OPCION_RESPUESTA",
                77L,
                "Baja lógica de la opción de respuesta VERDADERO/FALSO");

        RegistroAuditoria guardado = auditoriaDAO.guardar(nuevo);
        limpiarContexto();

        assertThat(guardado.getId()).isNotNull().isPositive();
        RegistroAuditoria persistido = entityManager.find(RegistroAuditoria.class, guardado.getId());
        assertThat(persistido).isNotNull();
        assertThat(persistido.getConsultor().getId()).isEqualTo(consultor.getId());
        assertThat(persistido.getConsultor().getUsername()).isEqualTo("CONSULTOR1");
        assertThat(persistido.getFechaHora()).isEqualTo(momentoBaja);
        assertThat(persistido.getAccion()).isEqualTo("BAJA");
        assertThat(persistido.getEntidad()).isEqualTo("OPCION_RESPUESTA");
        assertThat(persistido.getEntidadId()).isEqualTo(77L);
        assertThat(persistido.getDetalle()).isEqualTo("Baja lógica de la opción de respuesta VERDADERO/FALSO");
    }

    @Test
    void guardarRechazaRegistroSinConsultor() {
        RegistroAuditoria invalido = new RegistroAuditoria(
                null,
                null,
                LocalDateTime.of(2026, 4, 14, 16, 0),
                "BAJA",
                "OPCION_RESPUESTA",
                77L,
                null);

        assertThatThrownBy(() -> {
            auditoriaDAO.guardar(invalido);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void limpiarContexto() {
        entityManager.flush();
        entityManager.clear();
    }
}
