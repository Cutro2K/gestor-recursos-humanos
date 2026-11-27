package com.deso.api.service;

import com.deso.api.dao.*;
import com.deso.api.dto.EvaluacionRequestDTO;
import com.deso.api.dto.EvaluacionResponseDTO;
import com.deso.api.dto.PuestoDTO;
import com.deso.api.exception.RecursoDuplicadoException;
import com.deso.api.exception.RecursoNoEncontradoException;
import com.deso.api.model.entity.*;
import com.deso.api.model.enums.EstadoCuestionario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Gestor del caso de uso CU 25: Evaluar Candidatos (Generación de Cuestionarios).
 */
@Validated
@Service
public class GestorEvaluacion {

    private static final String CLAVE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CLAVE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EvaluacionDAO evaluacionDAO;
    private final PuestoDAO puestoDAO;
    private final CandidatoDAO candidatoDAO;
    private final CuestionarioDAO cuestionarioDAO;
    private final ConfiguracionCuestionarioDAO configuracionCuestionarioDAO;
    private final PreguntaDAO preguntaDAO;
    private final ConsultorDAO consultorDAO;

    public GestorEvaluacion(
            EvaluacionDAO evaluacionDAO,
            PuestoDAO puestoDAO,
            CandidatoDAO candidatoDAO,
            CuestionarioDAO cuestionarioDAO,
            ConfiguracionCuestionarioDAO configuracionCuestionarioDAO,
            PreguntaDAO preguntaDAO,
            ConsultorDAO consultorDAO) {
        this.evaluacionDAO = evaluacionDAO;
        this.puestoDAO = puestoDAO;
        this.candidatoDAO = candidatoDAO;
        this.cuestionarioDAO = cuestionarioDAO;
        this.configuracionCuestionarioDAO = configuracionCuestionarioDAO;
        this.preguntaDAO = preguntaDAO;
        this.consultorDAO = consultorDAO;
    }

    @Transactional(readOnly = true)
    public List<PuestoDTO> listarPuestosActivos() {
        return puestoDAO.listarActivos().stream()
                .map(p -> PuestoDTO.builder()
                        .id(p.getId())
                        .codigo(p.getCodigo())
                        .nombre(p.getNombre())
                        .empresa(p.getEmpresa())
                        .descripcion(p.getDescripcion())
                        .build())
                .toList();
    }

    @Transactional
    public EvaluacionResponseDTO generarEvaluacion(@NotNull @Valid EvaluacionRequestDTO request) {
        Puesto puesto = puestoDAO.buscarPorId(request.getPuestoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el puesto activo con ID " + request.getPuestoId() + "."));

        ConfiguracionCuestionario config = configuracionCuestionarioDAO.buscarVigente()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe una configuración de cuestionario vigente."));

        Long consultorId = request.getConsultorId() != null ? request.getConsultorId() : 1L;
        Consultor consultor = consultorDAO.buscarPorId(consultorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el consultor con ID " + consultorId + "."));

        List<Long> competenciaIds = puestoDAO.obtenerCompetenciaIdsPorPuesto(puesto.getId());
        if (competenciaIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "El puesto '" + puesto.getNombre() + "' no tiene competencias asociadas.");
        }

        List<Pregunta> preguntas = preguntaDAO.buscarActivasPorCompetencias(competenciaIds);
        if (preguntas.isEmpty()) {
            throw new IllegalArgumentException(
                    "No hay preguntas activas disponibles para las competencias del puesto '" + puesto.getNombre() + "'.");
        }

        Set<Long> candidatoIdsUnicos = new LinkedHashSet<>(request.getCandidatoIds());
        List<Candidato> candidatos = new ArrayList<>();
        for (Long candidatoId : candidatoIdsUnicos) {
            Candidato candidato = candidatoDAO.buscarPorId(candidatoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No se encontró el candidato con ID " + candidatoId + "."));

            if (cuestionarioDAO.buscarActivoOEnProcesoPorCandidato(candidatoId).isPresent()) {
                throw new RecursoDuplicadoException(
                        "candidatoIds",
                        "El candidato " + candidato.getNombre() + " " + candidato.getApellido()
                                + " ya posee un cuestionario activo o en proceso.");
            }
            candidatos.add(candidato);
        }

        LocalDateTime ahora = LocalDateTime.now();
        Evaluacion evaluacion = new Evaluacion();
        evaluacion.setConsultor(consultor);
        evaluacion.setPuesto(puesto);
        evaluacion.setConfiguracion(config);
        evaluacion.setFechaCreacion(ahora);
        evaluacion.setVersionModelo(
                request.getVersionModelo() != null && !request.getVersionModelo().isBlank()
                        ? request.getVersionModelo().strip()
                        : "1.0");
        evaluacion.setNombreArchivoExcel(request.getNombreArchivoExcel());

        int preguntasPorBloque = Math.max(1, config.getPreguntasPorBloque());

        for (Candidato candidato : candidatos) {
            Cuestionario cuestionario = construirCuestionario(
                    evaluacion, candidato, config, preguntas, preguntasPorBloque, ahora);
            evaluacion.getCuestionarios().add(cuestionario);
        }

        Evaluacion guardada = evaluacionDAO.guardar(evaluacion);

        List<EvaluacionResponseDTO.CuestionarioGeneradoDTO> cuestionariosDTO = guardada.getCuestionarios().stream()
                .map(c -> EvaluacionResponseDTO.CuestionarioGeneradoDTO.builder()
                        .cuestionarioId(c.getId())
                        .candidatoId(c.getCandidato().getId())
                        .candidatoNombre(c.getCandidato().getNombre())
                        .candidatoApellido(c.getCandidato().getApellido())
                        .claveIngreso(c.getClaveIngreso())
                        .estado(c.getEstado())
                        .cantidadBloques(c.getBloques().size())
                        .cantidadPreguntas(c.getBloques().stream()
                                .mapToInt(b -> b.getPreguntas().size())
                                .sum())
                        .build())
                .toList();

        return EvaluacionResponseDTO.builder()
                .evaluacionId(guardada.getId())
                .puestoId(puesto.getId())
                .puestoNombre(puesto.getNombre())
                .fechaCreacion(guardada.getFechaCreacion())
                .cantidadCuestionarios(cuestionariosDTO.size())
                .cuestionarios(cuestionariosDTO)
                .build();
    }

    private Cuestionario construirCuestionario(
            Evaluacion evaluacion,
            Candidato candidato,
            ConfiguracionCuestionario config,
            List<Pregunta> preguntas,
            int preguntasPorBloque,
            LocalDateTime ahora) {

        Cuestionario cuestionario = new Cuestionario();
        cuestionario.setEvaluacion(evaluacion);
        cuestionario.setCandidato(candidato);
        cuestionario.setClaveIngreso(generarClaveUnica());
        cuestionario.setEstado(EstadoCuestionario.ACTIVO);
        cuestionario.setFechaGeneracion(ahora);
        cuestionario.setFechaCambioEstado(ahora);
        cuestionario.setCantidadAccesos(0);
        cuestionario.setCantidadMaximaAccesos(config.getCantidadMaximaAccesos());
        cuestionario.setMinutosMaximosActivo(config.getMinutosMaximosActivo());
        cuestionario.setMinutosMaximosResolucion(config.getMinutosMaximosResolucion());
        cuestionario.setPreguntasPorBloque(preguntasPorBloque);

        int totalPreguntas = preguntas.size();
        int numeroBloque = 1;
        for (int i = 0; i < totalPreguntas; i += preguntasPorBloque) {
            int fin = Math.min(i + preguntasPorBloque, totalPreguntas);
            BloqueCuestionario bloque = new BloqueCuestionario();
            bloque.setCuestionario(cuestionario);
            bloque.setNumero(numeroBloque++);
            bloque.setCompletado(false);

            int ordenPregunta = 1;
            for (int j = i; j < fin; j++) {
                Pregunta pregunta = preguntas.get(j);
                PreguntaCuestionario pq = new PreguntaCuestionario();
                pq.setBloqueCuestionario(bloque);
                pq.setModeloPreguntaId(pregunta.getId());
                pq.setOrden(ordenPregunta++);
                pq.setEnunciadoSnapshot(pregunta.getEnunciado());
                pq.setRespondida(false);

                List<ItemOpcion> items = pregunta.getOpcionRespuesta().getItems().stream()
                        .sorted(Comparator.comparing(ItemOpcion::getOrden))
                        .toList();
                for (ItemOpcion item : items) {
                    OpcionCuestionario oc = new OpcionCuestionario();
                    oc.setPreguntaCuestionario(pq);
                    oc.setModeloOpcionId(item.getId());
                    oc.setOrden(item.getOrden());
                    oc.setDescripcionSnapshot(item.getDescripcion());
                    oc.setPonderacionSnapshot(BigDecimal.ZERO);
                    pq.getOpciones().add(oc);
                }

                bloque.getPreguntas().add(pq);
            }

            cuestionario.getBloques().add(bloque);
        }

        return cuestionario;
    }

    private String generarClaveUnica() {
        for (int intento = 0; intento < 10; intento++) {
            StringBuilder sb = new StringBuilder(CLAVE_LENGTH);
            for (int i = 0; i < CLAVE_LENGTH; i++) {
                sb.append(CLAVE_CHARS.charAt(RANDOM.nextInt(CLAVE_CHARS.length())));
            }
            String clave = sb.toString();
            if (cuestionarioDAO.buscarPorClave(clave).isEmpty()) {
                return clave;
            }
        }
        throw new IllegalStateException("No se pudo generar una clave de ingreso única.");
    }
}
