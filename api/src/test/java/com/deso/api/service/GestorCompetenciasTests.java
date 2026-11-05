package com.deso.api.service;

import com.deso.api.dao.CompetenciaDAO;
import com.deso.api.dto.CompetenciaDTO;
import com.deso.api.exception.RecursoDuplicadoException;
import com.deso.api.exception.RecursoNoEncontradoException;
import com.deso.api.model.entity.Competencia;
import com.deso.api.model.enums.TipoCompetencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestorCompetenciasTests {

    private FakeCompetenciaDAO dao;
    private GestorCompetencias gestor;

    @BeforeEach
    void setUp() {
        dao = new FakeCompetenciaDAO();
        gestor = new GestorCompetencias(dao);
    }

    @Test
    void darDeAltaRegistraCorrectamenteUnaCompetencia() {
        CompetenciaDTO dto = CompetenciaDTO.builder()
                .codigo("COMP-10")
                .nombre("Liderazgo")
                .tipo(TipoCompetencia.ACTITUDINAL)
                .descripcion("Capacidad para orientar y motivar al equipo.")
                .build();

        CompetenciaDTO resultado = gestor.darDeAlta(dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isNotNull();
        assertThat(resultado.getCodigo()).isEqualTo("COMP-10");
        assertThat(resultado.getNombre()).isEqualTo("Liderazgo");
        assertThat(resultado.getTipo()).isEqualTo(TipoCompetencia.ACTITUDINAL);
        assertThat(resultado.getEliminada()).isFalse();

        Competencia persistida = dao.buscarPorId(resultado.getId()).orElse(null);
        assertThat(persistida).isNotNull();
        assertThat(persistida.getCodigo()).isEqualTo("COMP-10");
    }

    @Test
    void darDeAltaFallaSiCodigoYaExiste() {
        dao.guardar(new Competencia(null, "COMP-01", "Gestión", null, TipoCompetencia.TECNICA, false));

        CompetenciaDTO dto = CompetenciaDTO.builder()
                .codigo("comp-01")
                .nombre("Gestión de Proyectos 2")
                .tipo(TipoCompetencia.TECNICA)
                .build();

        assertThatThrownBy(() -> gestor.darDeAlta(dto))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("comp-01");
    }

    @Test
    void darDeAltaFallaSiNombreYaExiste() {
        dao.guardar(new Competencia(null, "COMP-99", "Lealtad", null, TipoCompetencia.ACTITUDINAL, false));

        CompetenciaDTO dto = CompetenciaDTO.builder()
                .codigo("COMP-100")
                .nombre("lealtad")
                .tipo(TipoCompetencia.ACTITUDINAL)
                .build();

        assertThatThrownBy(() -> gestor.darDeAlta(dto))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("lealtad");
    }

    @Test
    void darDeAltaFallaConCamposObligatoriosFaltantes() {
        assertThatThrownBy(() -> gestor.darDeAlta(null))
                .isInstanceOf(IllegalArgumentException.class);

        CompetenciaDTO sinCodigo = CompetenciaDTO.builder()
                .nombre("Nombre")
                .tipo(TipoCompetencia.TECNICA)
                .build();
        assertThatThrownBy(() -> gestor.darDeAlta(sinCodigo))
                .isInstanceOf(IllegalArgumentException.class);

        CompetenciaDTO sinNombre = CompetenciaDTO.builder()
                .codigo("COD-01")
                .tipo(TipoCompetencia.TECNICA)
                .build();
        assertThatThrownBy(() -> gestor.darDeAlta(sinNombre))
                .isInstanceOf(IllegalArgumentException.class);

        CompetenciaDTO sinTipo = CompetenciaDTO.builder()
                .codigo("COD-01")
                .nombre("Nombre")
                .build();
        assertThatThrownBy(() -> gestor.darDeAlta(sinTipo))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void darDeAltaFallaSiSuperaLongitudesMaximas() {
        CompetenciaDTO codigoLargo = CompetenciaDTO.builder()
                .codigo("C".repeat(21))
                .nombre("Nombre")
                .tipo(TipoCompetencia.TECNICA)
                .build();
        assertThatThrownBy(() -> gestor.darDeAlta(codigoLargo))
                .isInstanceOf(IllegalArgumentException.class);

        CompetenciaDTO nombreLargo = CompetenciaDTO.builder()
                .codigo("COMP-01")
                .nombre("N".repeat(76))
                .tipo(TipoCompetencia.TECNICA)
                .build();
        assertThatThrownBy(() -> gestor.darDeAlta(nombreLargo))
                .isInstanceOf(IllegalArgumentException.class);

        CompetenciaDTO descLarga = CompetenciaDTO.builder()
                .codigo("COMP-01")
                .nombre("Nombre")
                .tipo(TipoCompetencia.TECNICA)
                .descripcion("D".repeat(1001))
                .build();
        assertThatThrownBy(() -> gestor.darDeAlta(descLarga))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listarMapeaEntidadesADTOYFiltra() {
        dao.guardar(new Competencia(null, "COMP-01", "Gestión", "Desc", TipoCompetencia.TECNICA, false));
        dao.guardar(new Competencia(null, "COMP-02", "Lealtad", "Desc", TipoCompetencia.ACTITUDINAL, false));
        dao.guardar(new Competencia(null, "COMP-03", "Eliminada", "Desc", TipoCompetencia.TECNICA, true));

        List<CompetenciaDTO> lista = gestor.listar(null, null, null);
        assertThat(lista).hasSize(2);
        assertThat(lista).extracting(CompetenciaDTO::getCodigo).containsExactly("COMP-01", "COMP-02");

        List<CompetenciaDTO> soloTecnicas = gestor.listar(null, null, TipoCompetencia.TECNICA);
        assertThat(soloTecnicas).hasSize(1);
        assertThat(soloTecnicas.get(0).getCodigo()).isEqualTo("COMP-01");
    }

    @Test
    void buscarPorIdRetornaDTOCorrespondiente() {
        Competencia c = dao.guardar(new Competencia(null, "COMP-01", "Gestión", "Desc", TipoCompetencia.TECNICA, false));

        CompetenciaDTO resultado = gestor.buscarPorId(c.getId());
        assertThat(resultado.getId()).isEqualTo(c.getId());
        assertThat(resultado.getNombre()).isEqualTo("Gestión");
    }

    @Test
    void buscarPorIdLanzaExcepcionSiNoExisteOEstaEliminada() {
        Competencia eliminada = dao.guardar(new Competencia(null, "COMP-X", "Eliminada", "Desc", TipoCompetencia.TECNICA, true));

        assertThatThrownBy(() -> gestor.buscarPorId(999L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("999");

        assertThatThrownBy(() -> gestor.buscarPorId(eliminada.getId()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(eliminada.getId().toString());
    }

    @Test
    void modificarActualizaCamposCorrectamente() {
        Competencia c = dao.guardar(new Competencia(null, "COMP-01", "Original", "Desc", TipoCompetencia.TECNICA, false));

        CompetenciaDTO mod = CompetenciaDTO.builder()
                .id(c.getId())
                .nombre("Modificado")
                .tipo(TipoCompetencia.ACTITUDINAL)
                .descripcion("Nueva desc")
                .build();

        CompetenciaDTO resultado = gestor.modificar(mod);
        assertThat(resultado.getNombre()).isEqualTo("Modificado");
        assertThat(resultado.getTipo()).isEqualTo(TipoCompetencia.ACTITUDINAL);
        assertThat(resultado.getDescripcion()).isEqualTo("Nueva desc");
    }

    @Test
    void modificarFallaSiCodigoYaExisteEnOtraCompetencia() {
        dao.guardar(new Competencia(null, "COMP-01", "Comp 1", null, TipoCompetencia.TECNICA, false));
        Competencia c2 = dao.guardar(new Competencia(null, "COMP-02", "Comp 2", null, TipoCompetencia.TECNICA, false));

        CompetenciaDTO mod = CompetenciaDTO.builder()
                .id(c2.getId())
                .codigo("COMP-01")
                .build();

        assertThatThrownBy(() -> gestor.modificar(mod))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("COMP-01");
    }

    @Test
    void darDeBajaMarcaComoEliminada() {
        Competencia c = dao.guardar(new Competencia(null, "COMP-01", "Comp 1", null, TipoCompetencia.TECNICA, false));

        gestor.darDeBaja(c.getId(), "admin");

        assertThat(dao.buscarDirecto(c.getId()).getEliminada()).isTrue();
        assertThat(dao.buscarPorId(c.getId())).isEmpty();
    }

    private static class FakeCompetenciaDAO implements CompetenciaDAO {
        private final Map<Long, Competencia> store = new LinkedHashMap<>();
        private long seq = 1;

        public Competencia buscarDirecto(Long id) {
            return store.get(id);
        }

        @Override
        public List<Competencia> listarActivas(String nombre, String codigo, TipoCompetencia tipo) {
            return store.values().stream()
                    .filter(c -> !Boolean.TRUE.equals(c.getEliminada()))
                    .filter(c -> nombre == null || c.getNombre().toLowerCase(Locale.ROOT).contains(nombre.toLowerCase(Locale.ROOT)))
                    .filter(c -> codigo == null || c.getCodigo().toLowerCase(Locale.ROOT).contains(codigo.toLowerCase(Locale.ROOT)))
                    .filter(c -> tipo == null || c.getTipo() == tipo)
                    .toList();
        }

        @Override
        public Optional<Competencia> buscarPorId(Long id) {
            if (id == null) return Optional.empty();
            Competencia c = store.get(id);
            if (c == null || Boolean.TRUE.equals(c.getEliminada())) {
                return Optional.empty();
            }
            return Optional.of(c);
        }

        @Override
        public Optional<Competencia> buscarPorCodigo(String codigo) {
            return store.values().stream()
                    .filter(c -> !Boolean.TRUE.equals(c.getEliminada()))
                    .filter(c -> c.getCodigo().equalsIgnoreCase(codigo))
                    .findFirst();
        }

        @Override
        public Optional<Competencia> buscarPorNombre(String nombre) {
            return store.values().stream()
                    .filter(c -> !Boolean.TRUE.equals(c.getEliminada()))
                    .filter(c -> c.getNombre().equalsIgnoreCase(nombre))
                    .findFirst();
        }

        @Override
        public boolean existePorCodigo(String codigo) {
            return store.values().stream()
                    .anyMatch(c -> c.getCodigo().equalsIgnoreCase(codigo));
        }

        @Override
        public boolean existePorNombre(String nombre) {
            return store.values().stream()
                    .anyMatch(c -> c.getNombre().equalsIgnoreCase(nombre));
        }

        @Override
        public Competencia guardar(Competencia c) {
            if (c.getId() == null) {
                c.setId(seq++);
            }
            Competencia copia = new Competencia(c.getId(), c.getCodigo(), c.getNombre(), c.getDescripcion(), c.getTipo(), c.getEliminada());
            store.put(c.getId(), copia);
            return copia;
        }

        @Override
        public void eliminarLogicamente(Long id) {
            Competencia c = store.get(id);
            if (c != null) {
                c.setEliminada(true);
            }
        }
    }
}
