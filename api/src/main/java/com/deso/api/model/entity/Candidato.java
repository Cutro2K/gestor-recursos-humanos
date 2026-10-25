package com.deso.api.model.entity;

import com.deso.api.model.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
    name = "candidato",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_candidato_nro", columnNames = {"nro_candidato"}),
        @UniqueConstraint(name = "uq_candidato_documento", columnNames = {"tipo_documento", "nro_documento"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Candidato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nro_candidato", nullable = false, unique = true)
    private Long nroCandidato;

    @Column(nullable = false, length = 50)
    private String apellido;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 10)
    private TipoDocumento tipoDocumento;

    @Column(name = "nro_documento", nullable = false, length = 20)
    private String nroDocumento;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(nullable = false, length = 1)
    private String genero;

    @Column(nullable = false, length = 50)
    private String nacionalidad;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 50)
    private String escolaridad;

    @Column(nullable = false)
    private Boolean eliminado = false;
}
