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
import com.deso.api.dto.PuestoDTO;
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
import com.deso.api.model.entity.ModeloEvaluacion;
import com.deso.api.model.entity.ModeloFactor;
import com.deso.api.model.entity.ModeloPregunta;
import com.deso.api.model.entity.Pregunta;
import com.deso.api.model.entity.PreguntaItemOpcion;
import com.deso.api.model.entity.Puesto;
import com.deso.api.model.enums.EstadoCuestionario;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gestor del CU 25: Evaluar a Candidatos.
 *
 * Al generar la evaluación congela el modelo del puesto (competencias, factores con al menos
 * 2 preguntas, preguntas y opciones con su ponderación) y crea un cuestionario en estado ACTIVO
 * con una clave de ingreso por candidato. Las preguntas de cada cuestionario se sortean de ese
 * modelo recién cuando el candidato lo empieza (CU 26, paso 3).
 */
@Service
public class GestorEvaluacion {

    /** Preguntas que el CU 26 sortea por factor: un factor con menos no puede evaluarse. */
    static final int PREGUNTAS_POR_FACTOR = 2;

    static final String TIPO_MIME_XLS = "application/vnd.ms-excel";

    private static final String LETRAS_CLAVE = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITOS_CLAVE = "23456789";
    private static final String CARACTERES_CLAVE = LETRAS_CLAVE + DIGITOS_CLAVE;
    private static final int LONGITUD_CLAVE = 8;
    private static final int INTENTOS_CLAVE = 20;
    private static final DateTimeFormatter FORMATO_ARCHIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
    private static final DateTimeFormatter FORMATO_VERSION = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter FORMATO_FECHA_EXCEL = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SecureRandom random = new SecureRandom();

    private final EvaluacionDAO evaluacionDAO;
    private final PuestoDAO puestoDAO;
    private final CandidatoDAO candidatoDAO;
    private final CuestionarioDAO cuestionarioDAO;
    private final PreguntaDAO preguntaDAO;
    private final ConsultorDAO consultorDAO;
    private final PropiedadesCuestionario propiedadesCuestionario;

    public GestorEvaluacion(
            EvaluacionDAO evaluacionDAO,
            PuestoDAO puestoDAO,
            CandidatoDAO candidatoDAO,
            CuestionarioDAO cuestionarioDAO,
            PreguntaDAO preguntaDAO,
            ConsultorDAO consultorDAO,
            PropiedadesCuestionario propiedadesCuestionario) {
        this.evaluacionDAO = evaluacionDAO;
        this.puestoDAO = puestoDAO;
        this.candidatoDAO = candidatoDAO;
        this.cuestionarioDAO = cuestionarioDAO;
        this.preguntaDAO = preguntaDAO;
        this.consultorDAO = consultorDAO;
        this.propiedadesCuestionario = propiedadesCuestionario;
    }

    /** Puestos que se pueden elegir para evaluar (paso 8). */
    @Transactional(readOnly = true)
    public List<PuestoDTO> listarPuestosActivos() {
        return puestoDAO.listarActivos().stream()
                .map(p -> new PuestoDTO(p.getId(), p.getCodigo(), p.getNombre(), p.getEmpresa()))
                .toList();
    }

    /** Competencias del puesto con su ponderación y si pueden evaluarse (paso 9 y flujo 9.A). */
    @Transactional(readOnly = true)
    public List<CompetenciaPuestoDTO> consultarCompetenciasPuesto(Long puestoId) {
        Puesto puesto = buscarPuesto(puestoId);
        return armarModelo(puesto).stream()
                .map(c -> new CompetenciaPuestoDTO(
                        c.competencia().getId(),
                        c.competencia().getCodigo(),
                        c.competencia().getNombre(),
                        c.ponderacionMinima(),
                        c.esEvaluable()))
                .toList();
    }

    /**
     * Paso 6: verifica que ningún candidato tenga un cuestionario activo o en proceso.
     * Si alguno lo tiene, informa quiénes no pueden ser evaluados nuevamente (flujo 6.A).
     */
    @Transactional(readOnly = true)
    public void verificarCandidatos(Collection<Long> candidatoIds) {
        verificarSinCuestionariosAbiertos(new LinkedHashSet<>(candidatoIds));
    }

    /**
     * Pasos 12 y 13: genera la evaluación con el modelo congelado y un cuestionario ACTIVO por
     * candidato. Vuelve a validar los flujos 6.A y 9.A por si algo cambió desde que se mostraron.
     */
    @Transactional
    public EvaluacionResultadoDTO evaluarCandidatos(EvaluacionDTO datos, Long consultorId) {
        Consultor consultor = consultorDAO.buscarPorId(consultorId)
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .orElseThrow(() -> new CredencialesInvalidasException("La sesión no es válida o venció"));
        Puesto puesto = buscarPuesto(datos.puestoId());

        // Bloquea a los candidatos antes de verificar: otra evaluación simultánea espera acá.
        Set<Long> ids = new LinkedHashSet<>(datos.candidatoIds());
        List<Candidato> candidatos = candidatoDAO.bloquearActivosPorIds(ids);
        if (candidatos.size() < ids.size()) {
            Set<Long> encontrados = candidatos.stream().map(Candidato::getId).collect(Collectors.toSet());
            String faltantes = ids.stream().filter(id -> !encontrados.contains(id))
                    .map(String::valueOf).collect(Collectors.joining(", "));
            throw new RecursoNoEncontradoException(
                    "No se encontraron los candidatos con ID " + faltantes + " (no existen o fueron dados de baja).");
        }
        verificarSinCuestionariosAbiertos(ids);

        List<CompetenciaModelo> modelo = armarModelo(puesto);
        if (modelo.isEmpty()) {
            throw new EvaluacionNoPermitidaException(
                    "La función o puesto " + puesto.getNombre() + " no tiene competencias asociadas para evaluar.");
        }
        List<String> noEvaluables = modelo.stream()
                .filter(c -> !c.esEvaluable())
                .map(c -> c.competencia().getNombre())
                .toList();
        if (!noEvaluables.isEmpty()) {
            throw new EvaluacionNoPermitidaException(
                    "Las siguientes competencias no pueden ser evaluadas: " + String.join(", ", noEvaluables)
                            + ". Cada competencia necesita al menos un factor con " + PREGUNTAS_POR_FACTOR
                            + " preguntas activas.");
        }

        LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String version = "v" + ahora.format(FORMATO_VERSION);

        Evaluacion evaluacion = new Evaluacion();
        evaluacion.setConsultor(consultor);
        evaluacion.setPuesto(puesto);
        evaluacion.setFechaCreacion(ahora);
        evaluacion.setVersionModelo(version);
        evaluacion.setNombreArchivoExcel(nombreArchivoExcel(ahora, puesto.getNombre()));
        evaluacion.setModelo(congelarModelo(evaluacion, modelo, ahora, version));

        Set<String> clavesGeneradas = new HashSet<>();
        for (Candidato candidato : candidatos) {
            Cuestionario cuestionario = new Cuestionario(
                    evaluacion, candidato, generarClaveUnica(clavesGeneradas), propiedadesCuestionario);
            cuestionario.setEstado(EstadoCuestionario.ACTIVO);
            cuestionario.setFechaGeneracion(ahora);
            cuestionario.setFechaCambioEstado(ahora);
            cuestionario.setCantidadAccesos(0);
            evaluacion.getCuestionarios().add(cuestionario);
        }

        return aResultado(evaluacionDAO.guardar(evaluacion));
    }

    /** Pasos 15 a 17: arma el archivo Excel con los candidatos evaluados y sus claves. */
    @Transactional(readOnly = true)
    public ArchivoDTO exportarClavesExcel(Long evaluacionId) {
        Evaluacion evaluacion = evaluacionDAO.buscarConCuestionarios(evaluacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la evaluación con ID " + evaluacionId + "."));

        try (Workbook libro = new HSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Claves de ingreso");
            CellStyle negrita = libro.createCellStyle();
            Font fuente = libro.createFont();
            fuente.setBold(true);
            negrita.setFont(fuente);

            Puesto puesto = evaluacion.getPuesto();
            escribirDato(hoja, 0, negrita, "Función o puesto", puesto.getNombre() + " (" + puesto.getEmpresa() + ")");
            escribirDato(hoja, 1, negrita, "Fecha de generación", evaluacion.getFechaCreacion().format(FORMATO_FECHA_EXCEL));
            escribirFila(hoja, 3, negrita, "Apellido", "Nombre", "Tipo de documento", "Nro. de documento", "Clave de ingreso");

            int fila = 4;
            for (CredencialCuestionarioDTO credencial : credenciales(evaluacion)) {
                escribirFila(hoja, fila++, null,
                        credencial.apellido(),
                        credencial.nombre(),
                        credencial.tipoDocumento().name(),
                        credencial.nroDocumento(),
                        credencial.claveIngreso());
            }
            for (int columna = 0; columna < 5; columna++) {
                hoja.autoSizeColumn(columna);
            }

            libro.write(salida);
            return new ArchivoDTO(evaluacion.getNombreArchivoExcel(), TIPO_MIME_XLS, salida.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el archivo Excel de la evaluación.", e);
        }
    }

    // ---------------------------------------------------------------------------------------

    private Puesto buscarPuesto(Long puestoId) {
        return puestoDAO.buscarPorId(puestoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la función o puesto con ID " + puestoId + "."));
    }

    private void verificarSinCuestionariosAbiertos(Set<Long> candidatoIds) {
        List<String> conCuestionario = cuestionarioDAO.buscarActivosOEnProcesoPorCandidatos(candidatoIds).stream()
                .map(Cuestionario::getCandidato)
                .map(c -> c.getApellido() + ", " + c.getNombre())
                .distinct()
                .toList();
        if (conCuestionario.size() == 1) {
            throw new EvaluacionNoPermitidaException("El candidato " + conCuestionario.getFirst()
                    + " posee un cuestionario activo o en proceso y no puede ser evaluado nuevamente.");
        }
        if (!conCuestionario.isEmpty()) {
            throw new EvaluacionNoPermitidaException("Los siguientes candidatos poseen cuestionarios activos o en"
                    + " proceso y no pueden ser evaluados nuevamente: " + String.join("; ", conCuestionario) + ".");
        }
    }

    /**
     * Lee del catálogo el modelo del puesto: por competencia, sus factores con al menos 2
     * preguntas válidas. Una pregunta es válida si su opción de respuesta tiene ítems y todos
     * tienen una ponderación asignada para esa pregunta.
     */
    private List<CompetenciaModelo> armarModelo(Puesto puesto) {
        List<CompetenciaPonderada> requeridas = puestoDAO.listarCompetenciasRequeridas(puesto.getId());
        if (requeridas.isEmpty()) {
            return List.of();
        }

        List<Pregunta> preguntas = preguntaDAO.buscarActivasPorCompetencias(
                requeridas.stream().map(r -> r.competencia().getId()).toList());
        Map<Long, Map<Long, BigDecimal>> ponderaciones = new HashMap<>();
        for (PreguntaItemOpcion pio : preguntaDAO.buscarPonderaciones(preguntas.stream().map(Pregunta::getId).toList())) {
            ponderaciones.computeIfAbsent(pio.getId().getPreguntaId(), k -> new HashMap<>())
                    .put(pio.getId().getItemOpcionId(), pio.getPonderacion());
        }

        // Preguntas válidas agrupadas por competencia y por factor (claves: IDs).
        Map<Long, Factor> factores = new HashMap<>();
        Map<Long, Map<Long, List<PreguntaModelo>>> porCompetencia = new HashMap<>();
        for (Pregunta pregunta : preguntas) {
            List<OpcionModelo> opciones = opcionesPonderadas(pregunta, ponderaciones.getOrDefault(pregunta.getId(), Map.of()));
            if (opciones.isEmpty()) {
                continue;
            }
            Factor factor = pregunta.getFactor();
            factores.put(factor.getId(), factor);
            porCompetencia
                    .computeIfAbsent(factor.getCompetencia().getId(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(factor.getId(), k -> new ArrayList<>())
                    .add(new PreguntaModelo(pregunta, opciones));
        }

        return requeridas.stream()
                .map(r -> new CompetenciaModelo(
                        r.competencia(),
                        r.ponderacionMinima(),
                        porCompetencia.getOrDefault(r.competencia().getId(), Map.of()).entrySet().stream()
                                .filter(e -> e.getValue().size() >= PREGUNTAS_POR_FACTOR)
                                .map(e -> new FactorModelo(factores.get(e.getKey()), e.getValue()))
                                .sorted(Comparator.comparing((FactorModelo f) -> f.factor().getOrden()))
                                .toList()))
                .toList();
    }

    /** Opciones de la pregunta con su ponderación; vacía si falta alguna ponderación. */
    private static List<OpcionModelo> opcionesPonderadas(Pregunta pregunta, Map<Long, BigDecimal> ponderaciones) {
        List<ItemOpcion> items = pregunta.getOpcionRespuesta().getItems();
        if (items.isEmpty() || !items.stream().allMatch(i -> ponderaciones.containsKey(i.getId()))) {
            return List.of();
        }
        return items.stream()
                .sorted(Comparator.comparing(ItemOpcion::getOrden))
                .map(i -> new OpcionModelo(i, ponderaciones.get(i.getId())))
                .toList();
    }

    private static ModeloEvaluacion congelarModelo(
            Evaluacion evaluacion, List<CompetenciaModelo> modelo, LocalDateTime ahora, String version) {
        ModeloEvaluacion snapshot = new ModeloEvaluacion(evaluacion, ahora, version);
        for (CompetenciaModelo competencia : modelo) {
            ModeloCompetencia modeloCompetencia =
                    snapshot.agregarCompetencia(competencia.competencia(), competencia.ponderacionMinima());
            for (FactorModelo factor : competencia.factores()) {
                ModeloFactor modeloFactor = modeloCompetencia.agregarFactor(factor.factor());
                for (PreguntaModelo pregunta : factor.preguntas()) {
                    ModeloPregunta modeloPregunta = modeloFactor.agregarPregunta(pregunta.pregunta());
                    pregunta.opciones().forEach(o -> modeloPregunta.agregarOpcion(o.item(), o.ponderacion()));
                }
            }
        }
        return snapshot;
    }

    /** Clave aleatoria única en el sistema y en el lote que se está generando. */
    private String generarClaveUnica(Set<String> clavesGeneradas) {
        for (int intento = 0; intento < INTENTOS_CLAVE; intento++) {
            String clave = generarClave();
            if (!clavesGeneradas.contains(clave) && cuestionarioDAO.buscarPorClave(clave).isEmpty()) {
                clavesGeneradas.add(clave);
                return clave;
            }
        }
        throw new IllegalStateException("No se pudo generar una clave de ingreso única.");
    }

    /**
     * 8 caracteres aleatorios con al menos una letra y un dígito (requerimiento especial del CU 25).
     * Se omiten I, O, 0 y 1 para que la clave no se confunda al leerla o dictarla.
     */
    String generarClave() {
        while (true) {
            StringBuilder clave = new StringBuilder(LONGITUD_CLAVE);
            for (int i = 0; i < LONGITUD_CLAVE; i++) {
                clave.append(CARACTERES_CLAVE.charAt(random.nextInt(CARACTERES_CLAVE.length())));
            }
            boolean tieneLetra = clave.chars().anyMatch(c -> LETRAS_CLAVE.indexOf(c) >= 0);
            boolean tieneDigito = clave.chars().anyMatch(c -> DIGITOS_CLAVE.indexOf(c) >= 0);
            if (tieneLetra && tieneDigito) {
                return clave.toString();
            }
        }
    }

    /** aaaa-mm-dd-hh-mm-NombreFunción.xls, con el nombre del puesto sin espacios, acentos ni símbolos. */
    static String nombreArchivoExcel(LocalDateTime fecha, String nombrePuesto) {
        String sinAcentos = Normalizer.normalize(nombrePuesto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String nombreFuncion = Arrays.stream(sinAcentos.split("[^A-Za-z0-9]+"))
                .filter(parte -> !parte.isEmpty())
                .map(parte -> parte.substring(0, 1).toUpperCase(Locale.ROOT) + parte.substring(1))
                .collect(Collectors.joining());
        return fecha.format(FORMATO_ARCHIVO) + "-" + (nombreFuncion.isEmpty() ? "Puesto" : nombreFuncion) + ".xls";
    }

    private static EvaluacionResultadoDTO aResultado(Evaluacion evaluacion) {
        Puesto puesto = evaluacion.getPuesto();
        return new EvaluacionResultadoDTO(
                evaluacion.getId(),
                puesto.getId(),
                puesto.getNombre(),
                puesto.getEmpresa(),
                evaluacion.getFechaCreacion(),
                evaluacion.getNombreArchivoExcel(),
                credenciales(evaluacion));
    }

    /** Candidatos evaluados con su clave, ordenados por apellido y nombre (paso 11). */
    private static List<CredencialCuestionarioDTO> credenciales(Evaluacion evaluacion) {
        return evaluacion.getCuestionarios().stream()
                .map(c -> new CredencialCuestionarioDTO(
                        c.getCandidato().getId(),
                        c.getCandidato().getApellido(),
                        c.getCandidato().getNombre(),
                        c.getCandidato().getTipoDocumento(),
                        c.getCandidato().getNroDocumento(),
                        c.getClaveIngreso()))
                .sorted(Comparator.comparing(CredencialCuestionarioDTO::apellido, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(CredencialCuestionarioDTO::nombre, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(CredencialCuestionarioDTO::candidatoId))
                .toList();
    }

    /** Fila "etiqueta: valor" del encabezado del archivo, con la etiqueta en negrita. */
    private static void escribirDato(Sheet hoja, int numero, CellStyle estiloEtiqueta, String etiqueta, String valor) {
        Row fila = hoja.createRow(numero);
        Cell celdaEtiqueta = fila.createCell(0);
        celdaEtiqueta.setCellValue(etiqueta);
        celdaEtiqueta.setCellStyle(estiloEtiqueta);
        fila.createCell(1).setCellValue(valor);
    }

    private static void escribirFila(Sheet hoja, int numero, CellStyle estilo, String... valores) {
        Row fila = hoja.createRow(numero);
        for (int i = 0; i < valores.length; i++) {
            Cell celda = fila.createCell(i);
            celda.setCellValue(valores[i]);
            if (estilo != null) {
                celda.setCellStyle(estilo);
            }
        }
    }

    private record OpcionModelo(ItemOpcion item, BigDecimal ponderacion) {}

    private record PreguntaModelo(Pregunta pregunta, List<OpcionModelo> opciones) {}

    private record FactorModelo(Factor factor, List<PreguntaModelo> preguntas) {}

    private record CompetenciaModelo(Competencia competencia, BigDecimal ponderacionMinima, List<FactorModelo> factores) {
        boolean esEvaluable() {
            return !factores.isEmpty();
        }
    }
}
