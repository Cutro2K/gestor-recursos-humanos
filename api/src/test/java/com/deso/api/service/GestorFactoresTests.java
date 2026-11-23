package com.deso.api.service;

import com.deso.api.dao.CompetenciaDAO;
import com.deso.api.dao.FactorDAO;
import com.deso.api.dto.FactorDTO;
import com.deso.api.dto.ListadoGestorFactoresDTO;
import com.deso.api.exception.RecursoActualmenteEliminadoException;
import com.deso.api.exception.RecursoDuplicadoException;
import com.deso.api.exception.RecursoNoEncontradoException;
import com.deso.api.model.entity.Competencia;
import com.deso.api.model.entity.Factor;
import com.deso.api.model.enums.TipoCompetencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Reglas de negocio de Factores (CU 16 a CU 19). El DAO de factores es un doble en memoria;
 * el de competencias es un mock porque solo se usa para buscar la competencia padre.
 */
class GestorFactoresTests {

    private FakeFactorDAO factorDAO;
    private CompetenciaDAO competenciaDAO;
    private GestorFactores gestor;

    private Competencia gestion;
    private Competencia liderazgo;

    @BeforeEach
    void setUp() {
        factorDAO = new FakeFactorDAO();
        competenciaDAO = mock(CompetenciaDAO.class);
        gestor = new GestorFactores(factorDAO, competenciaDAO);

        gestion = competencia(1L, "Gestión de proyectos", false);
        liderazgo = competencia(2L, "Liderazgo", false);
        when(competenciaDAO.buscarPorId(any())).thenReturn(Optional.empty());
        when(competenciaDAO.buscarPorId(1L)).thenReturn(Optional.of(gestion));
        when(competenciaDAO.buscarPorId(2L)).thenReturn(Optional.of(liderazgo));
    }

    // --- CU 17: dar de alta ---

    @Test
    void darDeAltaGuardaElFactorSinEspaciosSobrantesYActivo() {
        FactorDTO creado = gestor.darDeAlta(dto(1L, "  FAC-01 ", "  Capacidad de análisis ", 1, "  Pensamiento lógico  "));

        assertThat(creado.getId()).isNotNull();
        assertThat(creado.getCompetenciaId()).isEqualTo(1L);
        assertThat(creado.getCodigo()).isEqualTo("FAC-01");
        assertThat(creado.getNombre()).isEqualTo("Capacidad de análisis");
        assertThat(creado.getOrden()).isEqualTo(1);
        assertThat(creado.getDescripcion()).isEqualTo("Pensamiento lógico");
        assertThat(factorDAO.buscarPorId(creado.getId())).get()
                .satisfies(f -> assertThat(f.getEliminado()).isFalse());
    }

    @Test
    void darDeAltaAceptaDescripcionNula() {
        FactorDTO creado = gestor.darDeAlta(dto(1L, "FAC-01", "Análisis", 1, null));

        assertThat(creado.getDescripcion()).isNull();
    }

    @Test
    void darDeAltaFallaSiLaCompetenciaNoExiste() {
        assertThatThrownBy(() -> gestor.darDeAlta(dto(99L, "FAC-01", "Análisis", 1, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThat(factorDAO.store).isEmpty();
    }

    @Test
    void darDeAltaFallaSiLaCompetenciaEstaEliminada() {
        when(competenciaDAO.buscarPorId(3L)).thenReturn(Optional.of(competencia(3L, "Vieja", true)));

        assertThatThrownBy(() -> gestor.darDeAlta(dto(3L, "FAC-01", "Análisis", 1, null)))
                .isInstanceOf(RecursoActualmenteEliminadoException.class);
        assertThat(factorDAO.store).isEmpty();
    }

    @Test
    void darDeAltaFallaSiElCodigoYaExisteSinImportarMayusculas() {
        guardar(gestion, "FAC-01", "Análisis", 1, false);

        assertThatThrownBy(() -> gestor.darDeAlta(dto(1L, "fac-01", "Otro nombre", 2, null)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("código");
    }

    @Test
    void darDeAltaFallaSiElNombreYaExisteSinImportarMayusculas() {
        guardar(gestion, "FAC-01", "Análisis", 1, false);

        assertThatThrownBy(() -> gestor.darDeAlta(dto(2L, "FAC-02", "ANÁLISIS", 1, null)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    void darDeAltaFallaSiElOrdenYaEstaEnUsoEnLaMismaCompetencia() {
        guardar(gestion, "FAC-01", "Análisis", 1, false);

        assertThatThrownBy(() -> gestor.darDeAlta(dto(1L, "FAC-02", "Detalle", 1, null)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("orden 1");
    }

    @Test
    void darDeAltaPermiteElMismoOrdenEnOtraCompetencia() {
        guardar(gestion, "FAC-01", "Análisis", 1, false);

        FactorDTO creado = gestor.darDeAlta(dto(2L, "FAC-02", "Empatía", 1, null));

        assertThat(creado.getOrden()).isEqualTo(1);
        assertThat(creado.getCompetenciaId()).isEqualTo(2L);
    }

    @Test
    void darDeAltaReutilizaElOrdenDeUnFactorDadoDeBaja() {
        guardar(gestion, "FAC-01", "Análisis", 1, true);

        FactorDTO creado = gestor.darDeAlta(dto(1L, "FAC-02", "Detalle", 1, null));

        assertThat(creado.getOrden()).isEqualTo(1);
    }

    @Test
    void darDeAltaNoReutilizaElCodigoNiElNombreDeUnFactorDadoDeBaja() {
        guardar(gestion, "FAC-01", "Análisis", 1, true);

        assertThatThrownBy(() -> gestor.darDeAlta(dto(1L, "FAC-01", "Nuevo", 2, null)))
                .isInstanceOf(RecursoDuplicadoException.class);
        assertThatThrownBy(() -> gestor.darDeAlta(dto(1L, "FAC-02", "Análisis", 2, null)))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    // --- CU 18: modificar ---

    @Test
    void actualizarCambiaCompetenciaNombreOrdenYDescripcionPeroNoElCodigo() {
        Factor factor = guardar(gestion, "FAC-01", "Análisis", 1, false);

        FactorDTO actualizado = gestor.actualizar(
                dtoConId(factor.getId(), 2L, "OTRO-CODIGO", "  Escucha activa ", 3, "  Nueva  "));

        assertThat(actualizado.getCompetenciaId()).isEqualTo(2L);
        assertThat(actualizado.getCodigo()).isEqualTo("FAC-01");
        assertThat(actualizado.getNombre()).isEqualTo("Escucha activa");
        assertThat(actualizado.getOrden()).isEqualTo(3);
        assertThat(actualizado.getDescripcion()).isEqualTo("Nueva");
    }

    @Test
    void actualizarPermiteConservarElMismoNombreYOrden() {
        Factor factor = guardar(gestion, "FAC-01", "Análisis", 1, false);

        FactorDTO actualizado = gestor.actualizar(dtoConId(factor.getId(), 1L, "FAC-01", "análisis", 1, "Otra"));

        assertThat(actualizado.getNombre()).isEqualTo("análisis");
        assertThat(actualizado.getDescripcion()).isEqualTo("Otra");
    }

    @Test
    void actualizarFallaSiElNombrePerteneceAOtroFactor() {
        guardar(gestion, "FAC-01", "Análisis", 1, false);
        Factor factor = guardar(gestion, "FAC-02", "Detalle", 2, false);

        assertThatThrownBy(() -> gestor.actualizar(dtoConId(factor.getId(), 1L, "FAC-02", "ANÁLISIS", 2, null)))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    void actualizarFallaSiElOrdenEstaOcupadoEnLaCompetenciaDestino() {
        guardar(liderazgo, "FAC-01", "Empatía", 4, false);
        Factor factor = guardar(gestion, "FAC-02", "Detalle", 2, false);

        assertThatThrownBy(() -> gestor.actualizar(dtoConId(factor.getId(), 2L, "FAC-02", "Detalle", 4, null)))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    void actualizarPermiteTomarElOrdenDeUnFactorDadoDeBaja() {
        guardar(gestion, "FAC-01", "Análisis", 1, true);
        Factor factor = guardar(gestion, "FAC-02", "Detalle", 2, false);

        FactorDTO actualizado = gestor.actualizar(dtoConId(factor.getId(), 1L, "FAC-02", "Detalle", 1, null));

        assertThat(actualizado.getOrden()).isEqualTo(1);
    }

    @Test
    void actualizarFallaSiElFactorNoExiste() {
        assertThatThrownBy(() -> gestor.actualizar(dtoConId(99L, 1L, "FAC-01", "Análisis", 1, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizarFallaSiElFactorEstaDadoDeBaja() {
        Factor factor = guardar(gestion, "FAC-01", "Análisis", 1, true);

        assertThatThrownBy(() -> gestor.actualizar(dtoConId(factor.getId(), 1L, "FAC-01", "Análisis", 1, null)))
                .isInstanceOf(RecursoActualmenteEliminadoException.class);
    }

    @Test
    void actualizarFallaSiLaCompetenciaNuevaNoExiste() {
        Factor factor = guardar(gestion, "FAC-01", "Análisis", 1, false);

        assertThatThrownBy(() -> gestor.actualizar(dtoConId(factor.getId(), 99L, "FAC-01", "Análisis", 1, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizarFallaSiLaCompetenciaNuevaEstaEliminada() {
        when(competenciaDAO.buscarPorId(3L)).thenReturn(Optional.of(competencia(3L, "Vieja", true)));
        Factor factor = guardar(gestion, "FAC-01", "Análisis", 1, false);

        assertThatThrownBy(() -> gestor.actualizar(dtoConId(factor.getId(), 3L, "FAC-01", "Análisis", 1, null)))
                .isInstanceOf(RecursoActualmenteEliminadoException.class);
    }

    // --- CU 19: dar de baja ---

    @Test
    void eliminarLogicamenteMarcaElFactorComoEliminado() {
        Factor factor = guardar(gestion, "FAC-01", "Análisis", 1, false);

        gestor.eliminarLogicamente(factor.getId());

        assertThat(factorDAO.store.get(factor.getId()).getEliminado()).isTrue();
    }

    @Test
    void eliminarLogicamenteFallaSiElFactorNoExiste() {
        assertThatThrownBy(() -> gestor.eliminarLogicamente(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminarLogicamenteFallaSiYaEstabaEliminado() {
        Factor factor = guardar(gestion, "FAC-01", "Análisis", 1, true);

        assertThatThrownBy(() -> gestor.eliminarLogicamente(factor.getId()))
                .isInstanceOf(RecursoActualmenteEliminadoException.class);
    }

    // --- CU 16 y consultas ---

    @Test
    void buscarPorNombresDevuelveElListadoConLaCompetencia() {
        guardar(gestion, "FAC-01", "Análisis", 1, false);

        List<ListadoGestorFactoresDTO> listado = gestor.buscarPorNombres("gestión", "anál", 0, 50);

        assertThat(listado).singleElement().satisfies(item -> {
            assertThat(item.getCodigo()).isEqualTo("FAC-01");
            assertThat(item.getNombreFactor()).isEqualTo("Análisis");
            assertThat(item.getCompetenciaId()).isEqualTo(1L);
            assertThat(item.getNombreCompetencia()).isEqualTo("Gestión de proyectos");
            assertThat(item.getOrden()).isEqualTo(1);
        });
    }

    @Test
    void listarPorCompetenciaDevuelveSoloLosActivosOrdenados() {
        guardar(gestion, "FAC-03", "Tercero", 3, false);
        guardar(gestion, "FAC-01", "Primero", 1, false);
        guardar(gestion, "FAC-02", "Baja", 2, true);
        guardar(liderazgo, "FAC-04", "Otra competencia", 1, false);

        List<FactorDTO> factores = gestor.listarPorCompetencia(1L);

        assertThat(factores).extracting(FactorDTO::getCodigo).containsExactly("FAC-01", "FAC-03");
    }

    @Test
    void listarPorCompetenciaFallaSiLaCompetenciaNoExisteOEstaEliminada() {
        when(competenciaDAO.buscarPorId(3L)).thenReturn(Optional.of(competencia(3L, "Vieja", true)));

        assertThatThrownBy(() -> gestor.listarPorCompetencia(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> gestor.listarPorCompetencia(3L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void buscarPorIdDevuelveElFactorActivo() {
        Factor factor = guardar(gestion, "FAC-01", "Análisis", 1, false);

        FactorDTO encontrado = gestor.buscarPorId(factor.getId());

        assertThat(encontrado.getCodigo()).isEqualTo("FAC-01");
        assertThat(encontrado.getCompetenciaId()).isEqualTo(1L);
    }

    @Test
    void buscarPorIdFallaSiNoExisteOEstaDadoDeBaja() {
        Factor baja = guardar(gestion, "FAC-01", "Análisis", 1, true);

        assertThatThrownBy(() -> gestor.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> gestor.buscarPorId(baja.getId()))
                .isInstanceOf(RecursoActualmenteEliminadoException.class);
    }

    // --- auxiliares ---

    private static Competencia competencia(Long id, String nombre, boolean eliminada) {
        return new Competencia(id, "COMP-" + id, nombre, null, TipoCompetencia.TECNICA, eliminada);
    }

    private Factor guardar(Competencia competencia, String codigo, String nombre, int orden, boolean eliminado) {
        return factorDAO.guardar(new Factor(null, competencia, codigo, nombre, orden, null, eliminado));
    }

    private static FactorDTO dto(Long competenciaId, String codigo, String nombre, Integer orden, String descripcion) {
        return dtoConId(null, competenciaId, codigo, nombre, orden, descripcion);
    }

    private static FactorDTO dtoConId(Long id, Long competenciaId, String codigo, String nombre, Integer orden,
            String descripcion) {
        return new FactorDTO(id, competenciaId, codigo, nombre, orden, descripcion);
    }

    /** Doble en memoria con la misma semántica que FactorPostgreSQLDAO. */
    private static class FakeFactorDAO implements FactorDAO {
        private final Map<Long, Factor> store = new LinkedHashMap<>();
        private long seq = 1;

        @Override
        public Optional<Factor> buscarPorId(Long id) {
            return Optional.ofNullable(id == null ? null : store.get(id));
        }

        @Override
        public boolean existePorCodigo(String codigo) {
            return store.values().stream().anyMatch(f -> f.getCodigo().equalsIgnoreCase(codigo.strip()));
        }

        @Override
        public boolean existePorNombre(String nombre) {
            return store.values().stream().anyMatch(f -> f.getNombre().equalsIgnoreCase(nombre.strip()));
        }

        @Override
        public boolean existePorCompetenciaYOrden(Long competenciaId, Integer orden) {
            return activos()
                    .anyMatch(f -> f.getCompetencia().getId().equals(competenciaId) && f.getOrden().equals(orden));
        }

        @Override
        public List<Factor> listarActivosPorCompetencia(Long competenciaId) {
            return activos()
                    .filter(f -> f.getCompetencia().getId().equals(competenciaId))
                    .sorted(Comparator.comparing(Factor::getOrden))
                    .toList();
        }

        @Override
        public List<Factor> buscarPorNombres(String nombreCompetencia, String nombreFactor, Integer offset,
                Integer cantidad) {
            return activos()
                    .filter(f -> contiene(f.getCompetencia().getNombre(), nombreCompetencia))
                    .filter(f -> contiene(f.getNombre(), nombreFactor))
                    .sorted(Comparator.comparing(Factor::getNombre))
                    .skip(offset)
                    .limit(cantidad)
                    .toList();
        }

        @Override
        public Factor guardar(Factor factor) {
            if (factor.getId() == null) {
                factor.setId(seq++);
            }
            store.put(factor.getId(), factor);
            return factor;
        }

        @Override
        public void eliminarLogicamente(Long id) {
            buscarPorId(id).ifPresent(f -> f.setEliminado(true));
        }

        private java.util.stream.Stream<Factor> activos() {
            return store.values().stream().filter(f -> !Boolean.TRUE.equals(f.getEliminado()));
        }

        private static boolean contiene(String valor, String buscado) {
            return buscado == null || buscado.isBlank()
                    || valor.toLowerCase(Locale.ROOT).contains(buscado.strip().toLowerCase(Locale.ROOT));
        }
    }
}
