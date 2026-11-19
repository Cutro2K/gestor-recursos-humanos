package com.deso.api.service;

import com.deso.api.dao.CompetenciaDAO;
import com.deso.api.dto.CompetenciaDTO;
import com.deso.api.exception.RecursoDuplicadoException;
import com.deso.api.exception.RecursoNoEncontradoException;
import com.deso.api.model.entity.Competencia;
import com.deso.api.model.enums.TipoCompetencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GestorCompetenciasTests {

    private CompetenciaDAO competenciaDAO;
    private GestorCompetencias gestor;

    @BeforeEach
    void setUp() {
        competenciaDAO = mock(CompetenciaDAO.class);
        gestor = new GestorCompetencias(competenciaDAO);
    }

    @Test
    void darDeAltaRegistraCorrectamenteUnaCompetencia() {
        CompetenciaDTO dto = CompetenciaDTO.builder()
                .codigo("COMP-10")
                .nombre("Liderazgo")
                .tipo(TipoCompetencia.ACTITUDINAL)
                .descripcion("Capacidad para orientar y motivar al equipo.")
                .build();

        when(competenciaDAO.existePorCodigo("COMP-10")).thenReturn(false);
        when(competenciaDAO.existePorNombre("Liderazgo")).thenReturn(false);
        when(competenciaDAO.guardar(any(Competencia.class))).thenAnswer(invocation -> {
            Competencia c = invocation.getArgument(0);
            c.setId(10L);
            return c;
        });

        CompetenciaDTO resultado = gestor.darDeAlta(dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getCodigo()).isEqualTo("COMP-10");
        assertThat(resultado.getNombre()).isEqualTo("Liderazgo");
        assertThat(resultado.getTipo()).isEqualTo(TipoCompetencia.ACTITUDINAL);
        assertThat(resultado.getEliminada()).isFalse();

        ArgumentCaptor<Competencia> captor = ArgumentCaptor.forClass(Competencia.class);
        verify(competenciaDAO).guardar(captor.capture());
        assertThat(captor.getValue().getCodigo()).isEqualTo("COMP-10");
        assertThat(captor.getValue().getNombre()).isEqualTo("Liderazgo");
    }

    @Test
    void darDeAltaFallaSiCodigoYaExiste() {
        CompetenciaDTO dto = CompetenciaDTO.builder()
                .codigo("COMP-01")
                .nombre("Gestión de Proyectos 2")
                .tipo(TipoCompetencia.TECNICA)
                .build();

        when(competenciaDAO.existePorCodigo("COMP-01")).thenReturn(true);

        assertThatThrownBy(() -> gestor.darDeAlta(dto))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("COMP-01");

        verify(competenciaDAO, never()).guardar(any());
    }

    @Test
    void darDeAltaFallaSiNombreYaExiste() {
        CompetenciaDTO dto = CompetenciaDTO.builder()
                .codigo("COMP-99")
                .nombre("Lealtad")
                .tipo(TipoCompetencia.ACTITUDINAL)
                .build();

        when(competenciaDAO.existePorCodigo("COMP-99")).thenReturn(false);
        when(competenciaDAO.existePorNombre("Lealtad")).thenReturn(true);

        assertThatThrownBy(() -> gestor.darDeAlta(dto))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("Lealtad");

        verify(competenciaDAO, never()).guardar(any());
    }

    @Test
    void darDeAltaFallaConCamposObligatoriosFaltantes() {
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
    void listarMapeaEntidadesADTO() {
        Competencia c1 = new Competencia(1L, "COMP-01", "Gestión", "Desc", TipoCompetencia.TECNICA, false);
        Competencia c2 = new Competencia(2L, "COMP-02", "Lealtad", "Desc", TipoCompetencia.ACTITUDINAL, false);

        when(competenciaDAO.listarActivas(null, null, null)).thenReturn(List.of(c1, c2));

        List<CompetenciaDTO> lista = gestor.listar(null, null, null);

        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).getCodigo()).isEqualTo("COMP-01");
        assertThat(lista.get(1).getCodigo()).isEqualTo("COMP-02");
    }

    @Test
    void buscarPorIdRetornaDTOCorrespondiente() {
        Competencia c = new Competencia(1L, "COMP-01", "Gestión", "Desc", TipoCompetencia.TECNICA, false);
        when(competenciaDAO.buscarPorId(1L)).thenReturn(Optional.of(c));

        CompetenciaDTO resultado = gestor.buscarPorId(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Gestión");
    }

    @Test
    void buscarPorIdLanzaExcepcionSiNoExiste() {
        when(competenciaDAO.buscarPorId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gestor.buscarPorId(999L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("999");
    }
}
