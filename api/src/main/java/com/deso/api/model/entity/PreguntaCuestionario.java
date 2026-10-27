package com.deso.api.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "pregunta_cuestionario",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_pregunta_cuestionario_orden", columnNames = {"bloque_cuestionario_id", "orden"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PreguntaCuestionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Many-to-One: Pregunta asignada a un bloque del cuestionario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bloque_cuestionario_id", nullable = false)
    private BloqueCuestionario bloqueCuestionario;

    @Column(name = "modelo_pregunta_id", nullable = false)
    private Long modeloPreguntaId;

    @Column(nullable = false)
    private Integer orden;

    @Column(name = "enunciado_snapshot", nullable = false, length = 255)
    private String enunciadoSnapshot;

    @Column(nullable = false)
    private Boolean respondida = false;

    // Relación One-to-Many: Opciones congeladas disponibles para esta pregunta
    @OneToMany(mappedBy = "preguntaCuestionario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OpcionCuestionario> opciones = new ArrayList<>();

    // Relación One-to-One: La respuesta efectuada por el candidato si ya fue respondida
    @OneToOne(mappedBy = "preguntaCuestionario", cascade = CascadeType.ALL, orphanRemoval = true)
    private RespuestaCandidato respuesta;
}
