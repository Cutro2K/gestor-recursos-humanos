package com.deso.api.service;

import com.deso.api.dao.CandidatoDAO;
import com.deso.api.dao.ConsultorDAO;
import com.deso.api.dto.SesionDTO;
import com.deso.api.exception.CredencialesInvalidasException;
import com.deso.api.model.entity.Candidato;
import com.deso.api.model.entity.Consultor;
import org.springframework.stereotype.Service;

@Service
public class GestorAutenticacion {

    private final ConsultorDAO consultorDAO;
    private final CandidatoDAO candidatoDAO;

    public GestorAutenticacion(ConsultorDAO consultorDAO, CandidatoDAO candidatoDAO) {
        this.consultorDAO = consultorDAO;
        this.candidatoDAO = candidatoDAO;
    }

    // Corresponde a +autenticarConsultor(usuario, contrasena): SesionDTO
    public SesionDTO autenticarConsultor(String nombreconsultor, String contrasena) {
        Consultor consultor = consultorDAO.findByNombreUsuarioAndEliminadoFalse(nombreconsultor)
                .orElseThrow(() -> new CredencialesInvalidasException("Consultor no encontrado"));

        // En un entorno real, usar PasswordEncoder de Spring Security (ej. BCrypt)
        if (!consultor.getPassword().equals(contrasena)) {
            throw new CredencialesInvalidasException("Contraseña incorrecta");
        }

        return new SesionDTO(consultor.getId(), "CONSULTOR");
    }

    // Corresponde a +autenticarCandidato(tipoDoc, nroDoc, clave): SesionDTO
    public SesionDTO autenticarCandidato(String tipoDoc, String nroDoc, String clave) {
        Candidato candidato = candidatoDAO.findByTipoDocumentoAndNroDocumentoAndEliminadoFalse(tipoDoc, nroDoc)
                .orElseThrow(() -> new CredencialesInvalidasException("Candidato no encontrado"));

        // Aquí deberías integrar la validación con la clave de ingreso del Cuestionario ACTIVO
        // delegando en el GestorCuestionario o consultando el CuestionarioDAO directamente
        validarClaveCuestionario(candidato.getId(), clave); 

        return new SesionDTO(candidato.getId(), "CANDIDATO");
    }

    private void validarClaveCuestionario(Long candidatoId, String claveIngreso) {
        // Lógica de validación de los accesos al cuestionario para el candidato
        // Si falla: throw new CredencialesInvalidasException("Clave de cuestionario inválida o expirada");
    }
}