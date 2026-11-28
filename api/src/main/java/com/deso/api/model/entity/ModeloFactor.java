package com.deso.api.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot de un factor evaluable (con al menos 2 preguntas) de una competencia del modelo.
 */
@Entity
@Table(
    name = "modelo_factor",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_modelo_factor", columnNames = {"modelo_competencia_id", "factor_origen_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class ModeloFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modelo_competencia_id", nullable = false)
    private ModeloCompetencia modeloCompetencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factor_origen_id", nullable = false)
    private Factor factorOrigen;

    @Column(name = "codigo_snapshot", nullable = false, length = 30)
    private String codigoSnapshot;

    @Column(name = "nombre_snapshot", nullable = false, length = 50)
    private String nombreSnapshot;

    @Column(name = "orden_snapshot", nullable = false)
    private Integer ordenSnapshot;

    @OneToMany(mappedBy = "modeloFactor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ModeloPregunta> preguntas = new ArrayList<>();

    public ModeloFactor(ModeloCompetencia modeloCompetencia, Factor origen) {
        this.modeloCompetencia = modeloCompetencia;
        this.factorOrigen = origen;
        this.codigoSnapshot = origen.getCodigo();
        this.nombreSnapshot = origen.getNombre();
        this.ordenSnapshot = origen.getOrden();
    }

    public ModeloPregunta agregarPregunta(Pregunta origen) {
        ModeloPregunta pregunta = new ModeloPregunta(this, origen);
        preguntas.add(pregunta);
        return pregunta;
    }
}
