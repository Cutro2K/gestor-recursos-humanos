package com.deso.api.dao;

import com.deso.api.model.entity.Candidato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidatoDAO extends JpaRepository<Candidato, Long> {
    
    // Corresponde a +buscar(apellido, nombre, numero)
    @Query("SELECT c FROM Candidato c WHERE " +
            "(:apellido IS NULL OR c.apellido LIKE %:apellido%) AND " +
            "(:nombre IS NULL OR c.nombre LIKE %:nombre%) AND " +
            "(:numero IS NULL OR c.nroCandidato = :numero) AND " +
            "c.eliminado = false")
    List<Candidato> buscar(String apellido, String nombre, Long numero);

    // Búsqueda específica para autenticación por documento
    Optional<Candidato> findByTipoDocumentoAndNroDocumentoAndEliminadoFalse(String tipoDocumento, String nroDocumento);
}