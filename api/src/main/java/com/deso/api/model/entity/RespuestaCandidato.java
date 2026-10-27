package com.deso.api.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "respuesta_candidato",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_respuesta_pregunta", columnNames = {"pregunta_cuestionario_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaCandidato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación One-to-One: Cada pregunta respondida posee una única respuesta de candidato
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pregunta_cuestionario_id", nullable = false, unique = true)
    private PreguntaCuestionario preguntaCuestionario;

    // Relación Many-to-One: Opción que eligió el candidato
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opcion_cuestionario_id", nullable = false)
    private OpcionCuestionario opcionCuestionario;

    @Column(name = "fecha_respuesta", nullable = false)
    private LocalDateTime fechaRespuesta = LocalDateTime.now();
}
