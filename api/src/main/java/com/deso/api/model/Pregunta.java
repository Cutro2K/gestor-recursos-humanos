package com.deso.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "pregunta",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_pregunta_nombre", columnNames = {"factor_id", "nombre"}),
        @UniqueConstraint(name = "uq_pregunta_enunciado", columnNames = {"factor_id", "enunciado"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Many-to-One: La pregunta evalúa un Factor determinado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factor_id", nullable = false)
    private Factor factor;

    // Relación Many-to-One: La estructura de opciones de respuesta que usa esta pregunta
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opcion_respuesta_id", nullable = false)
    private OpcionRespuesta opcionRespuesta;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false, length = 255)
    private String enunciado;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Boolean activa = true;

    @Column(nullable = false)
    private Boolean eliminada = false;
}