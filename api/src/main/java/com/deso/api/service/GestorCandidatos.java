package com.deso.api.service;

import com.deso.api.dao.DAOCandidato;
import com.deso.api.dto.ListadoCandidatosDTO;
import com.deso.api.dto.ListadoCandidatosDTO.CandidatoResumen;
import com.deso.api.dto.PrevisualizacionCandidatosDTO;
import com.deso.api.dto.PrevisualizacionCandidatosDTO.Accion;
import com.deso.api.dto.PrevisualizacionCandidatosDTO.ErrorFila;
import com.deso.api.dto.PrevisualizacionCandidatosDTO.Fila;
import com.deso.api.dto.ResultadoImportacionCandidatosDTO;
import com.deso.api.exception.FormatoCsvException;
import com.deso.api.model.entity.Candidato;
import com.deso.api.model.enums.TipoDocumento;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.UncheckedIOException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class GestorCandidatos {

	private static final List<String> COLUMNAS = List.of(
			"Nro candidato", "Apellido", "Nombre", "Nro documento", "Tipo documento",
			"Fecha de nacimiento", "Género", "Nacionalidad", "Email", "Escolaridad", "Eliminado");

	private static final List<String> COLUMNAS_NORMALIZADAS = COLUMNAS.stream()
			.map(GestorCandidatos::normalizarEncabezado)
			.toList();

	private static final CSVFormat FORMATO = CSVFormat.RFC4180.builder()
			.setDelimiter(';')
			.setIgnoreSurroundingSpaces(false)
			.setIgnoreEmptyLines(false)
			.get();

	private final Validator validator;
	private final DAOCandidato candidatoDAO;

	public GestorCandidatos(Validator validator, DAOCandidato candidatoDAO) {
		this.validator = validator;
		this.candidatoDAO = candidatoDAO;
	}

	@Transactional(readOnly = true)
	public ListadoCandidatosDTO listarCandidatos(
			String apellido, String nombre, Long nroCandidato, Long cursor, int limite) {

		List<Candidato> encontrados = candidatoDAO.listarActivos(apellido, nombre, nroCandidato, cursor, limite + 1);
		boolean hayMas = encontrados.size() > limite;
		List<Candidato> pagina = encontrados.subList(0, Math.min(limite, encontrados.size()));

		List<CandidatoResumen> candidatos = pagina.stream()
				.map(candidato -> new CandidatoResumen(
						candidato.getId(), candidato.getNroCandidato(), candidato.getApellido(), candidato.getNombre(),
						candidato.getTipoDocumento(), candidato.getNroDocumento(), candidato.getEmail()))
				.toList();

		Long siguienteCursor = hayMas ? pagina.getLast().getId() : null;

		return new ListadoCandidatosDTO(candidatos, siguienteCursor);
	}

	/** Clasifica por nroCandidato sin persistir ni modificar entidades administradas por JPA. */
	@Transactional(readOnly = true)
	public PrevisualizacionCandidatosDTO previsualizarCSV(InputStream archivo) throws IOException {
		return prepararImportacion(archivo).previsualizacion();
	}

	/**
	 * Relee el CSV y consulta nuevamente la BD al confirmar. Guarda solo filas válidas e informa
	 * las rechazadas. Un fallo de persistencia revierte todo el lote válido de esta confirmación.
	 */
	@Transactional(rollbackFor = IOException.class)
	public ResultadoImportacionCandidatosDTO importarCSV(InputStream archivo) throws IOException {
		PlanImportacion plan = prepararImportacion(archivo);

		if (!plan.candidatos().isEmpty()) {
			candidatoDAO.guardarLote(plan.candidatos());
		}

		PrevisualizacionCandidatosDTO previa = plan.previsualizacion();
		List<ErrorFila> errores = previa.filas().stream()
				.flatMap(fila -> fila.errores().stream())
				.toList();

		return new ResultadoImportacionCandidatosDTO(
				previa.aCrear(),
				previa.aActualizar(),
				previa.aEliminar(),
				previa.rechazadas(),
				errores,
				previa.resumenErrores());
	}

	private PlanImportacion prepararImportacion(InputStream archivo) throws IOException {
		ResultadoParseo parseo = parsearCSV(archivo);
		List<DatosCandidato> candidatos = parseo.candidatos();

		Map<Long, Long> cantidadesPorNumero = candidatos.stream()
				.collect(Collectors.groupingBy(DatosCandidato::nroCandidato, Collectors.counting()));

		Map<Documento, Long> cantidadesPorDocumento = candidatos.stream()
				.filter(candidato -> !candidato.eliminado())
				.collect(Collectors.groupingBy(
						candidato -> new Documento(candidato.tipoDocumento(), candidato.nroDocumento()),
						Collectors.counting()));

		List<Fila> filas = new ArrayList<>();
		List<Candidato> paraGuardar = new ArrayList<>();

		for (FilaParseada fila : parseo.filas()) {
			filas.add(prepararFila(fila, cantidadesPorNumero, cantidadesPorDocumento, paraGuardar));
		}

		return new PlanImportacion(PrevisualizacionCandidatosDTO.desdeFilas(filas), paraGuardar);
	}

	private Fila prepararFila(
			FilaParseada fila,
			Map<Long, Long> cantidadesPorNumero,
			Map<Documento, Long> cantidadesPorDocumento,
			List<Candidato> paraGuardar) {

		List<ErrorFila> errores = new ArrayList<>(fila.errores());
		DatosCandidato datos = fila.candidato();

		if (datos == null) {
			return new Fila(fila.fila(), fila.valores(), Accion.RECHAZAR, errores);
		}

		validarDuplicados(datos, cantidadesPorNumero, cantidadesPorDocumento, errores);

		if (!errores.isEmpty()) {
			return new Fila(fila.fila(), fila.valores(), Accion.RECHAZAR, errores);
		}

		Candidato existente = candidatoDAO.buscarPorNumeroCandidato(datos.nroCandidato()).orElse(null);
		validarIdentidad(datos, existente, errores);

		if (!errores.isEmpty()) {
			return new Fila(fila.fila(), fila.valores(), Accion.RECHAZAR, errores);
		}

		Accion accion = determinarAccion(datos, existente);
		paraGuardar.add(prepararEntidad(datos, existente));

		return new Fila(fila.fila(), fila.valores(), accion, errores);
	}

	private void validarDuplicados(
			DatosCandidato datos,
			Map<Long, Long> cantidadesPorNumero,
			Map<Documento, Long> cantidadesPorDocumento,
			List<ErrorFila> errores) {

		if (cantidadesPorNumero.get(datos.nroCandidato()) > 1) {
			errores.add(new ErrorFila(datos.fila(), "Nro candidato", "El número está repetido en el CSV."));
		}

		Documento documento = new Documento(datos.tipoDocumento(), datos.nroDocumento());

		if (!datos.eliminado() && cantidadesPorDocumento.get(documento) > 1) {
			errores.add(new ErrorFila(
					datos.fila(), "Nro documento", "El tipo y número de documento están repetidos en el CSV."));
		}
	}

	private void validarIdentidad(DatosCandidato datos, Candidato existente, List<ErrorFila> errores) {
		if (datos.eliminado()) {
			if (existente == null) {
				errores.add(new ErrorFila(
						datos.fila(), "Nro candidato", "No existe un candidato con ese número para eliminar."));
			}

			return;
		}

		candidatoDAO.buscarPorDocumento(datos.tipoDocumento(), datos.nroDocumento())
				.filter(otro -> !otro.getNroCandidato().equals(datos.nroCandidato()))
				.ifPresent(otro -> errores.add(new ErrorFila(
						datos.fila(), "Nro documento", "El documento pertenece a otro número de candidato.")));
	}

	private Accion determinarAccion(DatosCandidato datos, Candidato existente) {
		if (datos.eliminado()) {
			return Accion.ELIMINAR;
		}

		return existente == null ? Accion.CREAR : Accion.ACTUALIZAR;
	}

	private Candidato prepararEntidad(DatosCandidato datos, Candidato existente) {
		if (datos.eliminado()) {
			// La baja conserva los datos almacenados: solo cambia el indicador eliminado.
			return new Candidato(
					existente.getId(), existente.getNroCandidato(),
					existente.getApellido(), existente.getNombre(),
					existente.getTipoDocumento(), existente.getNroDocumento(),
					existente.getFechaNacimiento(), existente.getGenero(),
					existente.getNacionalidad(), existente.getEmail(), existente.getEscolaridad(), true);
		}

		return new Candidato(
				existente == null ? null : existente.getId(), datos.nroCandidato(),
				datos.apellido(), datos.nombre(), datos.tipoDocumento(), datos.nroDocumento(),
				datos.fechaNacimiento(), datos.genero(), datos.nacionalidad(),
				datos.email(), datos.escolaridad(), false);
	}

	/**
	 * Parseo interno en UTF-8, con o sin BOM. Cierra el stream recibido y conserva las filas
	 * inválidas para la preview. Los errores de estructura interrumpen la lectura completa.
	 */
	ResultadoParseo parsearCSV(InputStream archivo) throws IOException {
		var decoder = StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);

		try (PushbackReader reader = new PushbackReader(new InputStreamReader(archivo, decoder), 1)) {
			int primero = reader.read();

			if (primero != -1 && primero != '\uFEFF') {
				reader.unread(primero);
			}

			try (CSVParser parser = FORMATO.parse(reader)) {
				return leerRegistros(parser);
			} catch (UncheckedIOException ex) {
				throw new FormatoCsvException("No se pudo interpretar el CSV: " + ex.getCause().getMessage(), ex);
			}
		}
	}

	private ResultadoParseo leerRegistros(CSVParser parser) {
		Iterator<CSVRecord> registros = parser.iterator();

		if (!registros.hasNext()) {
			throw new FormatoCsvException("El archivo CSV está vacío; falta el encabezado.");
		}

		List<String> encabezados = registros.next().stream()
				.map(GestorCandidatos::normalizarEncabezado)
				.toList();

		validarEncabezado(encabezados);

		List<Integer> posiciones = COLUMNAS_NORMALIZADAS.stream().map(encabezados::indexOf).toList();
		List<FilaParseada> filas = new ArrayList<>();

		while (true) {
			// hasNext() consume el próximo registro: guardar la línea antes de invocarlo.
			long fila = parser.getCurrentLineNumber() + 1;

			if (!registros.hasNext()) {
				break;
			}

			CSVRecord registro = registros.next();

			if (registro.size() == 1 && registro.get(0).isBlank()) {
				continue;
			}

			filas.add(parsearRegistro(registro, posiciones, fila));
		}

		return new ResultadoParseo(filas);
	}

	private void validarEncabezado(List<String> encabezados) {
		if (encabezados.size() != COLUMNAS.size()
				|| !new HashSet<>(encabezados).equals(new HashSet<>(COLUMNAS_NORMALIZADAS))) {
			throw new FormatoCsvException(
					"El encabezado debe contener una vez cada columna, separada por ';': "
							+ String.join(";", COLUMNAS));
		}
	}

	private FilaParseada parsearRegistro(CSVRecord registro, List<Integer> posiciones, long fila) {
		Map<String, String> originales = leerValoresOriginales(registro, posiciones);
		List<ErrorFila> errores = new ArrayList<>();

		if (registro.size() != COLUMNAS.size()) {
			errores.add(new ErrorFila(
					fila, "registro", "Se esperaban 11 columnas y se recibieron " + registro.size() + "."));

			return new FilaParseada(fila, originales, null, errores);
		}

		List<String> valores = COLUMNAS.stream().map(columna -> originales.get(columna).strip()).toList();
		DatosCandidato candidato = convertirCampos(valores, fila, errores);

		return new FilaParseada(fila, originales, errores.isEmpty() ? candidato : null, errores);
	}

	private Map<String, String> leerValoresOriginales(CSVRecord registro, List<Integer> posiciones) {
		Map<String, String> originales = new LinkedHashMap<>();

		for (int columna = 0; columna < COLUMNAS.size(); columna++) {
			int posicion = posiciones.get(columna);
			String valor = posicion < registro.size() ? registro.get(posicion) : "";

			originales.put(COLUMNAS.get(columna), valor);
		}

		return originales;
	}

	private DatosCandidato convertirCampos(List<String> valores, long fila, List<ErrorFila> errores) {
		IntStream.range(0, COLUMNAS.size())
				.filter(columna -> valores.get(columna).isEmpty())
				.forEach(columna -> errores.add(new ErrorFila(
						fila, COLUMNAS.get(columna), "El campo es obligatorio.")));

		Long numero = convertir(
				valores.get(0), fila, 0, errores,
				GestorCandidatos::parsearNumero,
				"Debe ser un entero positivo dentro del rango de Long.");

		TipoDocumento documento = convertir(
				valores.get(4), fila, 4, errores,
				valor -> TipoDocumento.valueOf(valor.toUpperCase(Locale.ROOT)),
				"Debe ser DNI, LE, LC o PP.");

		LocalDate fecha = convertir(
				valores.get(5), fila, 5, errores,
				GestorCandidatos::parsearFecha,
				"Debe ser una fecha válida con formato aaaa-mm-dd.");

		String genero = convertir(
				valores.get(6), fila, 6, errores,
				GestorCandidatos::parsearGenero,
				"Debe ser H o M.");

		Boolean eliminado = convertir(
				valores.get(10), fila, 10, errores,
				GestorCandidatos::parsearEliminado,
				"Debe ser True o False.");

		// Límites de la entidad; el documento conserva ceros iniciales y caracteres de pasaporte.
		validarLongitud(valores, 1, 50, fila, errores);
		validarLongitud(valores, 2, 50, fila, errores);
		validarLongitud(valores, 3, 20, fila, errores);
		validarLongitud(valores, 7, 50, fila, errores);
		validarLongitud(valores, 8, 255, fila, errores);
		validarLongitud(valores, 9, 50, fila, errores);

		DatosCandidato candidato = new DatosCandidato(
				fila, numero, valores.get(1), valores.get(2), valores.get(3), documento,
				fecha, genero, valores.get(7), valores.get(8), valores.get(9), eliminado);

		validator.validateProperty(candidato, "email")
				.forEach(error -> errores.add(new ErrorFila(fila, "Email", "El email no es válido.")));

		return candidato;
	}

	private <T> T convertir(
			String valor,
			long fila,
			int columna,
			List<ErrorFila> errores,
			Function<String, T> conversion,
			String mensaje) {

		if (valor.isEmpty()) {
			return null;
		}

		try {
			return conversion.apply(valor);
		} catch (IllegalArgumentException | DateTimeException ex) {
			errores.add(new ErrorFila(fila, COLUMNAS.get(columna), mensaje));
			return null;
		}
	}

	private void validarLongitud(
			List<String> valores, int columna, int maximo, long fila, List<ErrorFila> errores) {

		if (valores.get(columna).length() > maximo) {
			errores.add(new ErrorFila(
					fila, COLUMNAS.get(columna), "Debe tener como máximo " + maximo + " caracteres."));
		}
	}

	private static Long parsearNumero(String valor) {
		if (!valor.matches("[0-9]+")) {
			throw new IllegalArgumentException();
		}

		long numero = Long.parseLong(valor);

		if (numero <= 0) {
			throw new IllegalArgumentException();
		}

		return numero;
	}

	private static LocalDate parsearFecha(String valor) {
		if (!valor.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
			throw new IllegalArgumentException();
		}

		return LocalDate.parse(valor);
	}

	private static String parsearGenero(String valor) {
		String codigo = valor.toUpperCase(Locale.ROOT);

		if (!codigo.equals("H") && !codigo.equals("M")) {
			throw new IllegalArgumentException();
		}

		return codigo;
	}

	private static Boolean parsearEliminado(String valor) {
		if (!valor.equalsIgnoreCase("True") && !valor.equalsIgnoreCase("False")) {
			throw new IllegalArgumentException();
		}

		return Boolean.valueOf(valor);
	}

	private static String normalizarEncabezado(String valor) {
		return Normalizer.normalize(valor.strip(), Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT);
	}

	// Estructuras internas del parseo: no forman parte de los resultados públicos del servicio.
	record DatosCandidato(
			long fila,
			Long nroCandidato,
			String apellido,
			String nombre,
			String nroDocumento,
			TipoDocumento tipoDocumento,
			LocalDate fechaNacimiento,
			String genero,
			String nacionalidad,
			@Email String email,
			String escolaridad,
			Boolean eliminado) {
	}

	record FilaParseada(
			long fila,
			Map<String, String> valores,
			DatosCandidato candidato,
			List<ErrorFila> errores) {

		FilaParseada {
			valores = Collections.unmodifiableMap(new LinkedHashMap<>(valores));
			errores = List.copyOf(errores);
		}
	}

	record ResultadoParseo(List<FilaParseada> filas) {

		ResultadoParseo {
			filas = List.copyOf(filas);
		}

		List<DatosCandidato> candidatos() {
			return filas.stream()
					.map(FilaParseada::candidato)
					.filter(Objects::nonNull)
					.toList();
		}

		List<ErrorFila> errores() {
			return filas.stream().flatMap(fila -> fila.errores().stream()).toList();
		}
	}

	private record Documento(TipoDocumento tipo, String numero) {
	}

	private record PlanImportacion(PrevisualizacionCandidatosDTO previsualizacion, List<Candidato> candidatos) {
	}
}
