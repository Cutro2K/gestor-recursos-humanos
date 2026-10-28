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
import java.util.Locale;
import java.util.Optional;

@Repository
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Transactional(readOnly = true)
public class DAOPostgreSQLCandidato extends DAOCandidato {

	private final EntityManager entityManager;

	/** Spring crea una instancia por contexto e inyecta un EntityManager asociado a cada transacción. */
	public DAOPostgreSQLCandidato(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	public List<Candidato> listarActivos(
			String apellido, String nombre, Long nroCandidato, Long cursor, int cantidad) {

		return entityManager.createQuery("""
				select c from Candidato c
				where c.eliminado = false
				and (:apellido is null or lower(c.apellido) like :apellido escape '!')
				and (:nombre is null or lower(c.nombre) like :nombre escape '!')
				and (:numero is null or c.nroCandidato = :numero)
				and (:cursor is null or c.id > :cursor)
				order by c.id
				""", Candidato.class)
				.setParameter("apellido", patronDeBusqueda(apellido))
				.setParameter("nombre", patronDeBusqueda(nombre))
				.setParameter("numero", nroCandidato)
				.setParameter("cursor", cursor)
				.setMaxResults(cantidad)
				.getResultList();
	}

	private String patronDeBusqueda(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}

		String texto = valor.strip().toLowerCase(Locale.ROOT)
				.replace("!", "!!")
				.replace("%", "!%")
				.replace("_", "!_");

		return "%" + texto + "%";
	}

	@Override
	public Optional<Candidato> buscarPorNumeroCandidato(Long numero) {
		return entityManager.createQuery("select c from Candidato c where c.nroCandidato = :numero", Candidato.class)
				.setParameter("numero", numero)
				.getResultList().stream().findFirst();
	}

	@Override
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

	@Override
	public List<Candidato> buscarPorEmail(String email) {
		return entityManager.createQuery("""
				select c from Candidato c
				where lower(c.email) = lower(:email)
				order by c.id
				""", Candidato.class)
				.setParameter("email", email)
				.getResultList();
	}

	@Override
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
