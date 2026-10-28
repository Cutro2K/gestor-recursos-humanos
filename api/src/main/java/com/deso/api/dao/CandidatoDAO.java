package com.deso.api.dao;

import com.deso.api.model.entity.Candidato;
import com.deso.api.model.enums.TipoDocumento;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Transactional(readOnly = true)
public class CandidatoDAO {

	private final EntityManager entityManager;

	/** Spring crea una instancia por contexto e inyecta un EntityManager asociado a cada transacción. */
	public CandidatoDAO(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/** Identificador del sistema de origen usado para clasificar las filas del CSV del CU06. */
	public Optional<Candidato> buscarPorNumeroCandidato(Long numero) {
		return entityManager.createQuery("select c from Candidato c where c.nroCandidato = :numero", Candidato.class)
				.setParameter("numero", numero)
				.getResultList().stream().findFirst();
	}

	/** Incluye candidatos dados de baja para poder detectar identidades existentes al importar. */
	public Optional<Candidato> buscarPorDocumento(TipoDocumento tipoDocumento, String nroDocumento) {
		return entityManager.createQuery("""
				select c from Candidato c
				where c.tipoDocumento = :tipoDocumento and c.nroDocumento = :nroDocumento
				""", Candidato.class)
				.setParameter("tipoDocumento", tipoDocumento)
				.setParameter("nroDocumento", nroDocumento)
				.getResultList()
				.stream()
				.findFirst();
	}

	/** El email no es único en el modelo; devuelve todas las coincidencias, incluidas las bajas. */
	public List<Candidato> buscarPorEmail(String email) {
		return entityManager.createQuery("""
				select c from Candidato c
				where lower(c.email) = lower(:email)
				order by c.id
				""", Candidato.class)
				.setParameter("email", email)
				.getResultList();
	}

	/**
	 * Guarda el lote en una única transacción: inserta entidades sin ID y actualiza las que tienen ID.
	 * Devuelve las instancias administradas por JPA. La identificación de duplicados y las reglas
	 * de actualización corresponden al servicio de importación.
	 */
	@Transactional
	public List<Candidato> guardarLote(List<Candidato> candidatos) {
		if (candidatos.isEmpty()) {
			return List.of();
		}

		List<Candidato> guardados = new ArrayList<>(candidatos.size());
		for (Candidato candidato : candidatos) {
			if (candidato.getId() == null) {
				entityManager.persist(candidato);
				guardados.add(candidato);
			} else {
				guardados.add(entityManager.merge(candidato));
			}
		}
		entityManager.flush();
		return guardados;
	}
}
