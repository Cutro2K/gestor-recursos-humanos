package com.deso.api.controller;

import com.deso.api.dto.ListadoCandidatosDTO;
import com.deso.api.dto.PrevisualizacionCandidatosDTO;
import com.deso.api.dto.ResultadoImportacionCandidatosDTO;
import com.deso.api.exception.FormatoCsvException;
import com.deso.api.service.GestorCandidatos;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.util.Locale;

@RestController
@RequestMapping(value = "/api/candidatos", produces = MediaType.APPLICATION_JSON_VALUE)
public class CandidatoController {

	private final GestorCandidatos gestorCandidatos;

	public CandidatoController(GestorCandidatos gestorCandidatos) {
		this.gestorCandidatos = gestorCandidatos;
	}

	@GetMapping
	public ListadoCandidatosDTO listar(
			@RequestParam(required = false) @Size(max = 50, message = "Máximo 50 caracteres.") String apellido,
			@RequestParam(required = false) @Size(max = 50, message = "Máximo 50 caracteres.") String nombre,
			@RequestParam(required = false) @Positive(message = "Debe ser positivo.") Long nroCandidato,
			@RequestParam(required = false) @Positive(message = "Debe ser positivo.") Long cursor,
			@RequestParam(defaultValue = "50")
			@Min(value = 1, message = "El límite mínimo es 1.")
			@Max(value = 100, message = "El límite máximo es 100.") int limite) {

		return gestorCandidatos.listarCandidatos(apellido, nombre, nroCandidato, cursor, limite);
	}

	@PostMapping(value = "/previsualizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public PrevisualizacionCandidatosDTO previsualizar(@RequestParam("archivo") MultipartFile archivo)
			throws IOException {

		validarArchivo(archivo);

		try (InputStream contenido = archivo.getInputStream()) {
			return gestorCandidatos.previsualizarCSV(contenido);
		} catch (CharacterCodingException ex) {
			throw new FormatoCsvException("El archivo CSV debe estar codificado en UTF-8.", ex);
		}
	}

	@PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResultadoImportacionCandidatosDTO importar(@RequestParam("archivo") MultipartFile archivo)
			throws IOException {

		validarArchivo(archivo);

		try (InputStream contenido = archivo.getInputStream()) {
			return gestorCandidatos.importarCSV(contenido);
		} catch (CharacterCodingException ex) {
			throw new FormatoCsvException("El archivo CSV debe estar codificado en UTF-8.", ex);
		}
	}

	private void validarArchivo(MultipartFile archivo) {
		if (archivo.isEmpty()) {
			throw new FormatoCsvException("El archivo CSV no puede estar vacío.");
		}

		String nombre = archivo.getOriginalFilename();

		if (nombre == null || !nombre.toLowerCase(Locale.ROOT).endsWith(".csv")) {
			throw new FormatoCsvException("El archivo debe tener extensión .csv.");
		}
	}
}
