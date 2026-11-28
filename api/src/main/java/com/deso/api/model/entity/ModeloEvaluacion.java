package com.deso.api.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot del modelo de evaluación (CU 25): congela competencias, factores, preguntas y
 * opciones del puesto al generar la evaluación, para que todos sus cuestionarios usen el
 * mismo modelo aunque después cambie el catálogo.
 */
@Entity
@Table(name = "modelo_evaluacion")
@Getter
@Setter
@NoArgsConstructor
public class ModeloEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluacion_id", nullable = false, unique = true)
    private Evaluacion evaluacion;

    @Column(name = "fecha_congelamiento", nullable = false)
    private LocalDateTime fechaCongelamiento;

    @Column(nullable = false, length = 30)
    private String version;

    @OneToMany(mappedBy = "modeloEvaluacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ModeloCompetencia> competencias = new ArrayList<>();

    public ModeloEvaluacion(Evaluacion evaluacion, LocalDateTime fechaCongelamiento, String version) {
        this.evaluacion = evaluacion;
        this.fechaCongelamiento = fechaCongelamiento;
        this.version = version;
    }

    public ModeloCompetencia agregarCompetencia(Competencia origen, BigDecimal ponderacionMinima) {
        ModeloCompetencia competencia = new ModeloCompetencia(this, origen, ponderacionMinima);
        competencias.add(competencia);
        return competencia;
    }
}
