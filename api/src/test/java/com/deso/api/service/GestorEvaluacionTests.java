package com.deso.api.service;

import com.deso.api.config.PropiedadesCuestionario;
import com.deso.api.dao.CandidatoDAO;
import com.deso.api.dao.ConsultorDAO;
import com.deso.api.dao.CuestionarioDAO;
import com.deso.api.dao.EvaluacionDAO;
import com.deso.api.dao.PreguntaDAO;
import com.deso.api.dao.PuestoDAO;
import com.deso.api.dao.PuestoDAO.CompetenciaPonderada;
import com.deso.api.dto.ArchivoDTO;
import com.deso.api.dto.CompetenciaPuestoDTO;
import com.deso.api.dto.CredencialCuestionarioDTO;
import com.deso.api.dto.EvaluacionDTO;
import com.deso.api.dto.EvaluacionResultadoDTO;
import com.deso.api.exception.CredencialesInvalidasException;
import com.deso.api.exception.EvaluacionNoPermitidaException;
import com.deso.api.exception.RecursoNoEncontradoException;
import com.deso.api.model.entity.Candidato;
import com.deso.api.model.entity.Competencia;
import com.deso.api.model.entity.Consultor;
import com.deso.api.model.entity.Cuestionario;
import com.deso.api.model.entity.Evaluacion;
import com.deso.api.model.entity.Factor;
import com.deso.api.model.entity.ItemOpcion;
import com.deso.api.model.entity.ModeloCompetencia;
import com.deso.api.model.entity.ModeloFactor;
import com.deso.api.model.entity.ModeloOpcion;
import com.deso.api.model.entity.ModeloPregunta;
import com.deso.api.model.entity.OpcionRespuesta;
import com.deso.api.model.entity.Pregunta;
import com.deso.api.model.entity.PreguntaItemOpcion;
import com.deso.api.model.entity.PreguntaItemOpcionId;
import com.deso.api.model.entity.Puesto;
import com.deso.api.model.enums.EstadoCuestionario;
import com.deso.api.model.enums.TipoCompetencia;
import com.deso.api.model.enums.TipoDocumento;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestorEvaluacionTests {

    private static final PropiedadesCuestionario PROPIEDADES =
            new PropiedadesCuestionario("Instrucciones", 3, 1440, 60, 2);

    @Mock private EvaluacionDAO evaluacionDAO;
    @Mock private PuestoDAO puestoDAO;
    @Mock private CandidatoDAO candidatoDAO;
    @Mock private CuestionarioDAO cuestionarioDAO;
    @Mock private PreguntaDAO preguntaDAO;
    @Mock private ConsultorDAO consultorDAO;

    private GestorEvaluacion gestor;
    private Puesto puesto;
    private Consultor consultor;
    private OpcionRespuesta verdaderoFalso;

    @BeforeEach
    void setUp() {
        gestor = new GestorEvaluacion(
                evaluacionDAO, puestoDAO, candidatoDAO, cuestionarioDAO, preguntaDAO, consultorDAO, PROPIEDADES);
        puesto = new Puesto(1L, "PUESTO-01", "Desarrollador Java Senior", "Desc", "Acme", false);
        consultor = new Consultor(5L, "consultor", "Ana", "Gómez", "ana@test", true, LocalDateTime.now());
        verdaderoFalso = opcion(1L, item(11L, 1, "Verdadero"), item(12L, 2, "Falso"));

        lenient().when(puestoDAO.buscarPorId(1L)).thenReturn(Optional.of(puesto));
        lenient().when(consultorDAO.buscarPorId(5L)).thenReturn(Optional.of(consultor));
        lenient().when(cuestionarioDAO.buscarPorClave(anyString())).thenReturn(Optional.empty());
        lenient().when(evaluacionDAO.guardar(any(Evaluacion.class))).thenAnswer(inv -> {
            Evaluacion evaluacion = inv.getArgument(0);
            evaluacion.setId(99L);
            return evaluacion;
        });
    }

    @Test
    void evaluarCandidatos_congelaElModeloYCreaUnCuestionarioActivoSinPreguntasPorCandidato() {
        Competencia gestion = competencia(1L, "Gestión de Proyectos");
        Factor alcance = factor(10L, gestion, 1);
        Factor tiempo = factor(11L, gestion, 2);
        Pregunta p1 = pregunta(100L, alcance);
        Pregunta p2 = pregunta(101L, alcance);
        Pregunta soloUna = pregunta(102L, tiempo);
        catalogo(List.of(new CompetenciaPonderada(gestion, new BigDecimal("7.00"))), List.of(p1, p2, soloUna),
                ponderaciones(p1, "10", "0"), ponderaciones(p2, "8", "2"), ponderaciones(soloUna, "10", "0"));
        Candidato lopez = candidato(20L, "López", "María");
        Candidato perez = candidato(10L, "Pérez", "Juan");
        when(candidatoDAO.bloquearActivosPorIds(Set.of(20L, 10L))).thenReturn(List.of(perez, lopez));
        when(cuestionarioDAO.buscarActivosOEnProcesoPorCandidatos(anyCollection())).thenReturn(List.of());

        EvaluacionResultadoDTO resultado = gestor.evaluarCandidatos(new EvaluacionDTO(1L, List.of(20L, 10L, 20L)), 5L);

        ArgumentCaptor<Evaluacion> guardada = ArgumentCaptor.forClass(Evaluacion.class);
        verify(evaluacionDAO).guardar(guardada.capture());
        Evaluacion evaluacion = guardada.getValue();
        assertThat(evaluacion.getConsultor()).isSameAs(consultor);
        assertThat(evaluacion.getPuesto()).isSameAs(puesto);
        assertThat(evaluacion.getNombreArchivoExcel())
                .matches("\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-DesarrolladorJavaSenior\\.xls");

        // Modelo congelado: solo el factor con al menos 2 preguntas, con las ponderaciones del catálogo.
        assertThat(evaluacion.getModelo().getEvaluacion()).isSameAs(evaluacion);
        assertThat(evaluacion.getModelo().getVersion()).isEqualTo(evaluacion.getVersionModelo());
        assertThat(evaluacion.getModelo().getCompetencias()).hasSize(1);
        ModeloCompetencia modeloCompetencia = evaluacion.getModelo().getCompetencias().getFirst();
        assertThat(modeloCompetencia.getNombreSnapshot()).isEqualTo("Gestión de Proyectos");
        assertThat(modeloCompetencia.getTipoSnapshot()).isEqualTo("TECNICA");
        assertThat(modeloCompetencia.getPonderacionMinimaSnapshot()).isEqualByComparingTo("7");
        assertThat(modeloCompetencia.getFactores()).extracting(ModeloFactor::getFactorOrigen).containsExactly(alcance);
        List<ModeloPregunta> preguntas = modeloCompetencia.getFactores().getFirst().getPreguntas();
        assertThat(preguntas).extracting(ModeloPregunta::getPreguntaOrigen).containsExactly(p1, p2);
        assertThat(preguntas.get(1).getOpciones())
                .extracting(ModeloOpcion::getDescripcionSnapshot, o -> o.getPonderacionSnapshot().toPlainString())
                .containsExactly(tuple("Verdadero", "8"), tuple("Falso", "2"));

        // Un cuestionario ACTIVO por candidato (sin duplicados), con la configuración vigente y sin bloques:
        // las preguntas se sortean del modelo cuando el candidato lo empieza (CU 26).
        assertThat(evaluacion.getCuestionarios()).hasSize(2).allSatisfy(c -> {
            assertThat(c.getEstado()).isEqualTo(EstadoCuestionario.ACTIVO);
            assertThat(c.getBloques()).isEmpty();
            assertThat(c.getCantidadAccesos()).isZero();
            assertThat(c.getCantidadMaximaAccesos()).isEqualTo(3);
            assertThat(c.getMinutosMaximosActivo()).isEqualTo(1440);
            assertThat(c.getMinutosMaximosResolucion()).isEqualTo(60);
            assertThat(c.getPreguntasPorBloque()).isEqualTo(2);
            assertThat(c.getClaveIngreso()).matches("[A-Z2-9]{8}").matches(".*[A-Z].*").matches(".*\\d.*");
        });
        assertThat(evaluacion.getCuestionarios()).extracting(Cuestionario::getClaveIngreso).doesNotHaveDuplicates();

        assertThat(resultado.evaluacionId()).isEqualTo(99L);
        assertThat(resultado.empresa()).isEqualTo("Acme");
        assertThat(resultado.credenciales())
                .extracting(CredencialCuestionarioDTO::apellido, CredencialCuestionarioDTO::tipoDocumento,
                        CredencialCuestionarioDTO::nroDocumento)
                .containsExactly(tuple("López", TipoDocumento.DNI, "20"), tuple("Pérez", TipoDocumento.DNI, "10"));
    }

    @Test
    void evaluarCandidatos_conCuestionariosAbiertos_nombraATodosLosCandidatosYNoGuarda() {
        Candidato perez = candidato(10L, "Pérez", "Juan");
        Candidato lopez = candidato(20L, "López", "María");
        when(candidatoDAO.bloquearActivosPorIds(Set.of(10L, 20L))).thenReturn(List.of(perez, lopez));
        when(cuestionarioDAO.buscarActivosOEnProcesoPorCandidatos(Set.of(10L, 20L)))
                .thenReturn(List.of(cuestionarioDe(lopez), cuestionarioDe(perez)));

        assertThatThrownBy(() -> gestor.evaluarCandidatos(new EvaluacionDTO(1L, List.of(10L, 20L)), 5L))
                .isInstanceOf(EvaluacionNoPermitidaException.class)
                .hasMessageContaining("López, María")
                .hasMessageContaining("Pérez, Juan");
        verify(evaluacionDAO, never()).guardar(any());
    }

    @Test
    void verificarCandidatos_sinCuestionariosAbiertos_noLanza() {
        when(cuestionarioDAO.buscarActivosOEnProcesoPorCandidatos(Set.of(10L))).thenReturn(List.of());

        gestor.verificarCandidatos(List.of(10L, 10L));

        verify(cuestionarioDAO).buscarActivosOEnProcesoPorCandidatos(Set.of(10L));
    }

    @Test
    void verificarCandidatos_conUnCuestionarioAbierto_loInforma() {
        when(cuestionarioDAO.buscarActivosOEnProcesoPorCandidatos(Set.of(10L)))
                .thenReturn(List.of(cuestionarioDe(candidato(10L, "Pérez", "Juan"))));

        assertThatThrownBy(() -> gestor.verificarCandidatos(List.of(10L)))
                .isInstanceOf(EvaluacionNoPermitidaException.class)
                .hasMessage("El candidato Pérez, Juan posee un cuestionario activo o en proceso"
                        + " y no puede ser evaluado nuevamente.");
    }

    @Test
    void evaluarCandidatos_conCompetenciaSinFactorEvaluable_nombraLasCompetencias() {
        Competencia gestion = competencia(1L, "Gestión de Proyectos");
        Competencia lealtad = competencia(3L, "Lealtad");
        Factor alcance = factor(10L, gestion, 1);
        Factor credibilidad = factor(30L, lealtad, 1);
        Pregunta p1 = pregunta(100L, alcance);
        Pregunta p2 = pregunta(101L, alcance);
        Pregunta l1 = pregunta(300L, credibilidad);
        Pregunta sinPonderacionCompleta = pregunta(301L, credibilidad);
        catalogo(List.of(new CompetenciaPonderada(gestion, BigDecimal.ONE), new CompetenciaPonderada(lealtad, BigDecimal.ONE)),
                List.of(p1, p2, l1, sinPonderacionCompleta),
                ponderaciones(p1, "10", "0"), ponderaciones(p2, "10", "0"), ponderaciones(l1, "10", "0"),
                List.of(ponderacion(sinPonderacionCompleta, verdaderoFalso.getItems().getFirst(), "10")));
        when(candidatoDAO.bloquearActivosPorIds(Set.of(10L))).thenReturn(List.of(candidato(10L, "Pérez", "Juan")));
        when(cuestionarioDAO.buscarActivosOEnProcesoPorCandidatos(anyCollection())).thenReturn(List.of());

        assertThatThrownBy(() -> gestor.evaluarCandidatos(new EvaluacionDTO(1L, List.of(10L)), 5L))
                .isInstanceOf(EvaluacionNoPermitidaException.class)
                .hasMessageStartingWith("Las siguientes competencias no pueden ser evaluadas: Lealtad.");
        verify(evaluacionDAO, never()).guardar(any());
    }

    @Test
    void consultarCompetenciasPuesto_indicaPonderacionYSiCadaCompetenciaEsEvaluable() {
        Competencia gestion = competencia(1L, "Gestión de Proyectos");
        Competencia lealtad = competencia(3L, "Lealtad");
        Factor alcance = factor(10L, gestion, 1);
        Pregunta p1 = pregunta(100L, alcance);
        Pregunta p2 = pregunta(101L, alcance);
        catalogo(List.of(new CompetenciaPonderada(gestion, new BigDecimal("7.00")),
                        new CompetenciaPonderada(lealtad, new BigDecimal("6.00"))),
                List.of(p1, p2), ponderaciones(p1, "10", "0"), ponderaciones(p2, "10", "0"));

        List<CompetenciaPuestoDTO> competencias = gestor.consultarCompetenciasPuesto(1L);

        assertThat(competencias)
                .extracting(CompetenciaPuestoDTO::nombre, c -> c.ponderacionMinima().toPlainString(),
                        CompetenciaPuestoDTO::evaluable)
                .containsExactly(tuple("Gestión de Proyectos", "7.00", true), tuple("Lealtad", "6.00", false));
    }

    @Test
    void evaluarCandidatos_puestoSinCompetencias_rechaza() {
        when(candidatoDAO.bloquearActivosPorIds(Set.of(10L))).thenReturn(List.of(candidato(10L, "Pérez", "Juan")));
        when(cuestionarioDAO.buscarActivosOEnProcesoPorCandidatos(anyCollection())).thenReturn(List.of());
        when(puestoDAO.listarCompetenciasRequeridas(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> gestor.evaluarCandidatos(new EvaluacionDTO(1L, List.of(10L)), 5L))
                .isInstanceOf(EvaluacionNoPermitidaException.class)
                .hasMessageContaining("no tiene competencias asociadas");
    }

    @Test
    void evaluarCandidatos_candidatoInexistenteODadoDeBaja_informaSusIds() {
        when(candidatoDAO.bloquearActivosPorIds(Set.of(10L, 77L))).thenReturn(List.of(candidato(10L, "Pérez", "Juan")));

        assertThatThrownBy(() -> gestor.evaluarCandidatos(new EvaluacionDTO(1L, List.of(10L, 77L)), 5L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("ID 77 ");
    }

    @Test
    void evaluarCandidatos_puestoInexistente_lanza404() {
        when(puestoDAO.buscarPorId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gestor.evaluarCandidatos(new EvaluacionDTO(999L, List.of(10L)), 5L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void evaluarCandidatos_consultorInactivo_esSesionInvalida() {
        consultor.setActivo(false);

        assertThatThrownBy(() -> gestor.evaluarCandidatos(new EvaluacionDTO(1L, List.of(10L)), 5L))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void evaluarCandidatos_siLaClaveYaExisteEnLaBase_generaOtra() {
        Competencia gestion = competencia(1L, "Gestión de Proyectos");
        Factor alcance = factor(10L, gestion, 1);
        Pregunta p1 = pregunta(100L, alcance);
        Pregunta p2 = pregunta(101L, alcance);
        catalogo(List.of(new CompetenciaPonderada(gestion, BigDecimal.ONE)), List.of(p1, p2),
                ponderaciones(p1, "10", "0"), ponderaciones(p2, "10", "0"));
        when(candidatoDAO.bloquearActivosPorIds(Set.of(10L))).thenReturn(List.of(candidato(10L, "Pérez", "Juan")));
        when(cuestionarioDAO.buscarActivosOEnProcesoPorCandidatos(anyCollection())).thenReturn(List.of());
        when(cuestionarioDAO.buscarPorClave(anyString()))
                .thenReturn(Optional.of(new Cuestionario()))
                .thenReturn(Optional.empty());

        EvaluacionResultadoDTO resultado = gestor.evaluarCandidatos(new EvaluacionDTO(1L, List.of(10L)), 5L);

        assertThat(resultado.credenciales().getFirst().claveIngreso()).hasSize(8);
        verify(cuestionarioDAO, times(2)).buscarPorClave(anyString());
    }

    @Test
    void generarClave_tieneOchoCaracteresConLetrasYDigitosSinAmbiguos() {
        for (int i = 0; i < 2000; i++) {
            assertThat(gestor.generarClave())
                    .matches("[A-HJ-NP-Z2-9]{8}")
                    .matches(".*[A-Z].*")
                    .matches(".*[2-9].*");
        }
    }

    @Test
    void nombreArchivoExcel_usaFechaYNombreDeLaFuncionSinEspaciosNiAcentos() {
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 29, 10, 0);

        assertThat(GestorEvaluacion.nombreArchivoExcel(fecha, "Desarrollador Java Senior"))
                .isEqualTo("2026-10-29-10-00-DesarrolladorJavaSenior.xls");
        assertThat(GestorEvaluacion.nombreArchivoExcel(fecha, "analista de información / datos"))
                .isEqualTo("2026-10-29-10-00-AnalistaDeInformacionDatos.xls");
    }

    @Test
    void exportarClavesExcel_generaUnXlsConLosCandidatosYSusClaves() throws Exception {
        Evaluacion evaluacion = new Evaluacion();
        evaluacion.setId(99L);
        evaluacion.setPuesto(puesto);
        evaluacion.setFechaCreacion(LocalDateTime.of(2026, 10, 29, 10, 0));
        evaluacion.setNombreArchivoExcel("2026-10-29-10-00-DesarrolladorJavaSenior.xls");
        Cuestionario cuestionario = cuestionarioDe(candidato(10L, "Pérez", "Juan"));
        cuestionario.setClaveIngreso("A2B3C4D5");
        evaluacion.getCuestionarios().add(cuestionario);
        when(evaluacionDAO.buscarConCuestionarios(99L)).thenReturn(Optional.of(evaluacion));

        ArchivoDTO archivo = gestor.exportarClavesExcel(99L);

        assertThat(archivo.nombre()).isEqualTo("2026-10-29-10-00-DesarrolladorJavaSenior.xls");
        assertThat(archivo.tipoMime()).isEqualTo("application/vnd.ms-excel");
        try (HSSFWorkbook libro = new HSSFWorkbook(new ByteArrayInputStream(archivo.contenido()))) {
            Sheet hoja = libro.getSheetAt(0);
            assertThat(hoja.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Desarrollador Java Senior (Acme)");
            assertThat(hoja.getRow(3).getCell(4).getStringCellValue()).isEqualTo("Clave de ingreso");
            assertThat(hoja.getRow(4).getCell(0).getStringCellValue()).isEqualTo("Pérez");
            assertThat(hoja.getRow(4).getCell(2).getStringCellValue()).isEqualTo("DNI");
            assertThat(hoja.getRow(4).getCell(4).getStringCellValue()).isEqualTo("A2B3C4D5");
        }
    }

    @Test
    void exportarClavesExcel_evaluacionInexistente_lanza404() {
        when(evaluacionDAO.buscarConCuestionarios(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gestor.exportarClavesExcel(5L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ---------------------------------------------------------------------------------------

    @SafeVarargs
    private void catalogo(List<CompetenciaPonderada> requeridas, List<Pregunta> preguntas,
                          List<PreguntaItemOpcion>... ponderaciones) {
        when(puestoDAO.listarCompetenciasRequeridas(1L)).thenReturn(requeridas);
        when(preguntaDAO.buscarActivasPorCompetencias(anyCollection())).thenReturn(preguntas);
        List<PreguntaItemOpcion> todas = new ArrayList<>();
        for (List<PreguntaItemOpcion> lista : ponderaciones) {
            todas.addAll(lista);
        }
        when(preguntaDAO.buscarPonderaciones(anyCollection())).thenReturn(todas);
    }

    private List<PreguntaItemOpcion> ponderaciones(Pregunta pregunta, String verdadero, String falso) {
        return List.of(
                ponderacion(pregunta, verdaderoFalso.getItems().get(0), verdadero),
                ponderacion(pregunta, verdaderoFalso.getItems().get(1), falso));
    }

    private static PreguntaItemOpcion ponderacion(Pregunta pregunta, ItemOpcion item, String valor) {
        return new PreguntaItemOpcion(
                new PreguntaItemOpcionId(pregunta.getId(), item.getId()), pregunta, item, new BigDecimal(valor));
    }

    private static Competencia competencia(Long id, String nombre) {
        Competencia competencia = new Competencia();
        competencia.setId(id);
        competencia.setCodigo("COMP-" + id);
        competencia.setNombre(nombre);
        competencia.setTipo(TipoCompetencia.TECNICA);
        return competencia;
    }

    private static Factor factor(Long id, Competencia competencia, int orden) {
        Factor factor = new Factor();
        factor.setId(id);
        factor.setCompetencia(competencia);
        factor.setCodigo("FAC-" + id);
        factor.setNombre("Factor " + id);
        factor.setOrden(orden);
        return factor;
    }

    private Pregunta pregunta(Long id, Factor factor) {
        return new Pregunta(id, factor, verdaderoFalso, "P" + id, "Enunciado " + id, null, true, false);
    }

    private static OpcionRespuesta opcion(Long id, ItemOpcion... items) {
        OpcionRespuesta opcion = new OpcionRespuesta();
        opcion.setId(id);
        for (ItemOpcion item : items) {
            item.setOpcionRespuesta(opcion);
            opcion.getItems().add(item);
        }
        return opcion;
    }

    private static ItemOpcion item(Long id, int orden, String descripcion) {
        return new ItemOpcion(id, null, orden, descripcion);
    }

    private static Candidato candidato(Long id, String apellido, String nombre) {
        Candidato candidato = new Candidato();
        candidato.setId(id);
        candidato.setApellido(apellido);
        candidato.setNombre(nombre);
        candidato.setTipoDocumento(TipoDocumento.DNI);
        candidato.setNroDocumento(String.valueOf(id));
        return candidato;
    }

    private static Cuestionario cuestionarioDe(Candidato candidato) {
        Cuestionario cuestionario = new Cuestionario();
        cuestionario.setCandidato(candidato);
        return cuestionario;
    }
}
