package com.deso.api.model.entity;

import com.deso.api.model.enums.EstadoCuestionario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "cuestionario",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_cuestionario_evaluacion_candidato", columnNames = {"evaluacion_id", "candidato_id"}),
        @UniqueConstraint(name = "uq_cuestionario_clave", columnNames = {"clave_ingreso"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cuestionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Many-to-One: Evaluación a la que corresponde este cuestionario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluacion_id", nullable = false)
    private Evaluacion evaluacion;

    // Relación Many-to-One: Candidato asignado para responderlo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidato_id", nullable = false)
    private Candidato candidato;

    @Column(name = "clave_ingreso", nullable = false, unique = true, length = 8)
    private String claveIngreso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoCuestionario estado = EstadoCuestionario.ACTIVO;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion = LocalDateTime.now();

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "ultimo_ingreso")
    private LocalDateTime ultimoIngreso;

    @Column(name = "fecha_cambio_estado", nullable = false)
    private LocalDateTime fechaCambioEstado = LocalDateTime.now();

    @Column(name = "cantidad_accesos", nullable = false)
    private Integer cantidadAccesos = 0;

    @Column(name = "cantidad_maxima_accesos", nullable = false)
    private Integer cantidadMaximaAccesos;

    @Column(name = "minutos_maximos_activo", nullable = false)
    private Integer minutosMaximosActivo;

    @Column(name = "minutos_maximos_resolucion", nullable = false)
    private Integer minutosMaximosResolucion;

    @Column(name = "preguntas_por_bloque", nullable = false)
    private Integer preguntasPorBloque;

    @Column(name = "puntaje_total", precision = 6, scale = 2)
    private BigDecimal puntajeTotal;

    // Relación One-to-Many: El cuestionario se divide en bloques ordenados de preguntas
    @OneToMany(mappedBy = "cuestionario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BloqueCuestionario> bloques = new ArrayList<>();
}
