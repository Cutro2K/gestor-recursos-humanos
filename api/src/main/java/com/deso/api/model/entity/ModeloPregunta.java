package com.deso.api.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot de una pregunta activa de un factor del modelo: es el universo del que el
 * CU 26 sortea las preguntas de cada cuestionario (PreguntaCuestionario.modeloPreguntaId).
 */
@Entity
@Table(name = "modelo_pregunta")
@Getter
@Setter
@NoArgsConstructor
public class ModeloPregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modelo_factor_id", nullable = false)
    private ModeloFactor modeloFactor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pregunta_origen_id", nullable = false)
    private Pregunta preguntaOrigen;

    @Column(name = "nombre_snapshot", nullable = false, length = 50)
    private String nombreSnapshot;

    @Column(name = "enunciado_snapshot", nullable = false, length = 255)
    private String enunciadoSnapshot;

    @Column(name = "descripcion_snapshot", columnDefinition = "TEXT")
    private String descripcionSnapshot;

    @OneToMany(mappedBy = "modeloPregunta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ModeloOpcion> opciones = new ArrayList<>();

    public ModeloPregunta(ModeloFactor modeloFactor, Pregunta origen) {
        this.modeloFactor = modeloFactor;
        this.preguntaOrigen = origen;
        this.nombreSnapshot = origen.getNombre();
        this.enunciadoSnapshot = origen.getEnunciado();
        this.descripcionSnapshot = origen.getDescripcion();
    }

    public void agregarOpcion(ItemOpcion origen, BigDecimal ponderacion) {
        opciones.add(new ModeloOpcion(this, origen, ponderacion));
    }
}
