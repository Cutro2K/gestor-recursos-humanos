package com.deso.api.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot de una competencia del puesto y de la ponderación mínima exigida al generar la evaluación.
 */
@Entity
@Table(
    name = "modelo_competencia",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_modelo_competencia", columnNames = {"modelo_evaluacion_id", "competencia_origen_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class ModeloCompetencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modelo_evaluacion_id", nullable = false)
    private ModeloEvaluacion modeloEvaluacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competencia_origen_id", nullable = false)
    private Competencia competenciaOrigen;

    @Column(name = "codigo_snapshot", nullable = false, length = 30)
    private String codigoSnapshot;

    @Column(name = "nombre_snapshot", nullable = false, length = 75)
    private String nombreSnapshot;

    @Column(name = "tipo_snapshot", nullable = false, length = 30)
    private String tipoSnapshot;

    @Column(name = "ponderacion_minima_snapshot", nullable = false, precision = 4, scale = 2)
    private BigDecimal ponderacionMinimaSnapshot;

    @OneToMany(mappedBy = "modeloCompetencia", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ModeloFactor> factores = new ArrayList<>();

    public ModeloCompetencia(ModeloEvaluacion modeloEvaluacion, Competencia origen, BigDecimal ponderacionMinima) {
        this.modeloEvaluacion = modeloEvaluacion;
        this.competenciaOrigen = origen;
        this.codigoSnapshot = origen.getCodigo();
        this.nombreSnapshot = origen.getNombre();
        this.tipoSnapshot = origen.getTipo().name();
        this.ponderacionMinimaSnapshot = ponderacionMinima;
    }

    public ModeloFactor agregarFactor(Factor origen) {
        ModeloFactor factor = new ModeloFactor(this, origen);
        factores.add(factor);
        return factor;
    }
}
