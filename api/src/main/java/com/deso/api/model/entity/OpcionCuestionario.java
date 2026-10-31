package com.deso.api.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
    name = "opcion_cuestionario",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_opcion_cuestionario_orden", columnNames = {"pregunta_cuestionario_id", "orden"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpcionCuestionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Many-to-One: Pertenece a una pregunta de cuestionario específica
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pregunta_cuestionario_id", nullable = false)
    private PreguntaCuestionario preguntaCuestionario;

    @Column(name = "modelo_opcion_id", nullable = false)
    private Long modeloOpcionId;

    @Column(nullable = false)
    private Integer orden;

    @Column(name = "descripcion_snapshot", nullable = false, length = 150)
    private String descripcionSnapshot;

    @Column(name = "ponderacion_snapshot", nullable = false, precision = 4, scale = 2)
    private BigDecimal ponderacionSnapshot;
}
