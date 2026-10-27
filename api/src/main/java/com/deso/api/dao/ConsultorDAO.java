package com.deso.api.dao;

import com.deso.api.model.entity.Consultor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsultorDAO extends JpaRepository<Consultor, Long> {
    
    Optional<Consultor> findByNombreUsuarioAndEliminadoFalse(String nombreUsuario);
}