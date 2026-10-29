package com.deso.api.dto;

import com.deso.api.model.entity.Candidato;
import com.deso.api.model.enums.TipoDocumento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter 
@Getter 
@NoArgsConstructor 
@AllArgsConstructor 
public class CandidatoDTO {
    private Long id;

    @NotNull(message = "El número de candidato es obligatorio")
    private Long nroCandidato;

    @NotBlank(message  = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre no puede superar mas de 50 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 50, message = "El apellido no puede superar mas de 50 caracteres")
    private String apellido;

    @NotBlank(message = "El tipo de documento es obligatorio")
    private TipoDocumento tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    private String nroDocumento;

    public static CandidatoDTO fromEntity(Candidato candidato) {
        if(candidato == null) return null;
        return new CandidatoDTO(
            candidato.getId(),
            candidato.getNroCandidato(),
            candidato.getNombre(),
            candidato.getApellido(),
            candidato.getTipoDocumento(),
            candidato.getNroDocumento()
        );
    }
}
