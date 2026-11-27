package com.deso.api.service;

import com.deso.api.dao.*;
import com.deso.api.dto.EvaluacionRequestDTO;
import com.deso.api.dto.EvaluacionResponseDTO;
import com.deso.api.exception.RecursoDuplicadoException;
import com.deso.api.exception.RecursoNoEncontradoException;
import com.deso.api.model.entity.*;
import com.deso.api.model.enums.EstadoCuestionario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestorEvaluacionTests {

    @Mock private EvaluacionDAO evaluacionDAO;
    @Mock private PuestoDAO puestoDAO;
    @Mock private CandidatoDAO candidatoDAO;
    @Mock private CuestionarioDAO cuestionarioDAO;
    @Mock private ConfiguracionCuestionarioDAO configuracionCuestionarioDAO;
    @Mock private PreguntaDAO preguntaDAO;
    @Mock private ConsultorDAO consultorDAO;

    private GestorEvaluacion gestorEvaluacion;

    @BeforeEach
    void setUp() {
        gestorEvaluacion = new GestorEvaluacion(
                evaluacionDAO,
                puestoDAO,
                candidatoDAO,
                cuestionarioDAO,
                configuracionCuestionarioDAO,
                preguntaDAO,
                consultorDAO
        );
    }

    @Test
    void generarEvaluacion_exitosa_creaCuestionariosYBloques() {
        Puesto puesto = new Puesto(1L, "DEV-SR", "Desarrollador Senior", "Desc", "Acme", false);
        ConfiguracionCuestionario config = new ConfiguracionCuestionario(
                1L, LocalDateTime.now().minusDays(1), null, "Instrucciones", 3, 60, 30, 2, true);
        Consultor consultor = new Consultor();
        consultor.setId(1L);

        Candidato c1 = new Candidato();
        c1.setId(10L);
        c1.setNombre("Ana");
        c1.setApellido("Pérez");

        OpcionRespuesta opcionResp = new OpcionRespuesta();
        opcionResp.setId(1L);
        ItemOpcion item1 = new ItemOpcion(1L, opcionResp, 1, "Siempre");
        ItemOpcion item2 = new ItemOpcion(2L, opcionResp, 2, "Nunca");
        opcionResp.setItems(List.of(item1, item2));

        Pregunta p1 = new Pregunta(100L, null, opcionResp, "P1", "Enunciado 1", null, true, false);
        Pregunta p2 = new Pregunta(101L, null, opcionResp, "P2", "Enunciado 2", null, true, false);
        Pregunta p3 = new Pregunta(102L, null, opcionResp, "P3", "Enunciado 3", null, true, false);

        when(puestoDAO.buscarPorId(1L)).thenReturn(Optional.of(puesto));
        when(configuracionCuestionarioDAO.buscarVigente()).thenReturn(Optional.of(config));
        when(consultorDAO.buscarPorId(1L)).thenReturn(Optional.of(consultor));
        when(puestoDAO.obtenerCompetenciaIdsPorPuesto(1L)).thenReturn(List.of(5L));
        when(preguntaDAO.buscarActivasPorCompetencias(List.of(5L))).thenReturn(List.of(p1, p2, p3));
        when(candidatoDAO.buscarPorId(10L)).thenReturn(Optional.of(c1));
        when(cuestionarioDAO.buscarActivoOEnProcesoPorCandidato(10L)).thenReturn(Optional.empty());
        when(cuestionarioDAO.buscarPorClave(anyString())).thenReturn(Optional.empty());
        when(evaluacionDAO.guardar(any(Evaluacion.class))).thenAnswer(inv -> {
            Evaluacion ev = inv.getArgument(0);
            ev.setId(99L);
            return ev;
        });

        EvaluacionRequestDTO req = EvaluacionRequestDTO.builder()
                .puestoId(1L)
                .candidatoIds(List.of(10L))
                .build();

        EvaluacionResponseDTO resp = gestorEvaluacion.generarEvaluacion(req);

        assertEquals(99L, resp.getEvaluacionId());
        assertEquals(1, resp.getCantidadCuestionarios());
        assertEquals(EstadoCuestionario.ACTIVO, resp.getCuestionarios().get(0).getEstado());
        assertEquals(2, resp.getCuestionarios().get(0).getCantidadBloques());
        assertEquals(3, resp.getCuestionarios().get(0).getCantidadPreguntas());
        assertEquals(8, resp.getCuestionarios().get(0).getClaveIngreso().length());
    }

    @Test
    void generarEvaluacion_candidatoConCuestionarioActivo_lanzaExcepcion() {
        Puesto puesto = new Puesto(1L, "DEV-SR", "Desarrollador Senior", "Desc", "Acme", false);
        ConfiguracionCuestionario config = new ConfiguracionCuestionario(
                1L, LocalDateTime.now().minusDays(1), null, "Instrucciones", 3, 60, 30, 2, true);
        Consultor consultor = new Consultor();
        consultor.setId(1L);
        Candidato c1 = new Candidato();
        c1.setId(10L);
        c1.setNombre("Ana");
        c1.setApellido("Pérez");

        when(puestoDAO.buscarPorId(1L)).thenReturn(Optional.of(puesto));
        when(configuracionCuestionarioDAO.buscarVigente()).thenReturn(Optional.of(config));
        when(consultorDAO.buscarPorId(1L)).thenReturn(Optional.of(consultor));
        when(puestoDAO.obtenerCompetenciaIdsPorPuesto(1L)).thenReturn(List.of(5L));
        when(preguntaDAO.buscarActivasPorCompetencias(List.of(5L))).thenReturn(List.of(new Pregunta()));
        when(candidatoDAO.buscarPorId(10L)).thenReturn(Optional.of(c1));
        when(cuestionarioDAO.buscarActivoOEnProcesoPorCandidato(10L)).thenReturn(Optional.of(new Cuestionario()));

        EvaluacionRequestDTO req = EvaluacionRequestDTO.builder()
                .puestoId(1L)
                .candidatoIds(List.of(10L))
                .build();

        assertThrows(RecursoDuplicadoException.class, () -> gestorEvaluacion.generarEvaluacion(req));
    }

    @Test
    void generarEvaluacion_puestoInexistente_lanzaExcepcion() {
        when(puestoDAO.buscarPorId(999L)).thenReturn(Optional.empty());

        EvaluacionRequestDTO req = EvaluacionRequestDTO.builder()
                .puestoId(999L)
                .candidatoIds(List.of(10L))
                .build();

        assertThrows(RecursoNoEncontradoException.class, () -> gestorEvaluacion.generarEvaluacion(req));
    }
}
