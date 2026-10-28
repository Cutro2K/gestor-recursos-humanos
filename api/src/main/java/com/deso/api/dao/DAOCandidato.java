package com.deso.api.dao;

import com.deso.api.model.entity.Candidato;
import com.deso.api.model.enums.TipoDocumento;

import java.util.List;
import java.util.Optional;

/** Contrato de acceso a candidatos independiente del mecanismo de persistencia. */
public abstract class DAOCandidato {

	/** Página de candidatos activos ordenada por ID, con filtros y cursor opcionales. */
	public abstract List<Candidato> listarActivos(
			String apellido, String nombre, Long nroCandidato, Long cursor, int cantidad);

	/** Busca por el identificador del sistema de origen, incluyendo candidatos dados de baja. */
	public abstract Optional<Candidato> buscarPorNumeroCandidato(Long numero);

	/** Incluye candidatos dados de baja para detectar identidades existentes al importar. */
	public abstract Optional<Candidato> buscarPorDocumento(TipoDocumento tipoDocumento, String nroDocumento);

	/** Devuelve todas las coincidencias sin distinguir mayúsculas, incluidas las bajas, ordenadas por ID. */
	public abstract List<Candidato> buscarPorEmail(String email);

	/**
	 * Guarda el lote de forma atómica: inserta candidatos sin ID y actualiza los que tienen ID.
	 * Devuelve los candidatos guardados. Las reglas de importación corresponden al servicio.
	 */
	public abstract List<Candidato> guardarLote(List<Candidato> candidatos);
}
