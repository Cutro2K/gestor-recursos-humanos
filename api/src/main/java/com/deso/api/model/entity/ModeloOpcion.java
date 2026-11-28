package com.deso.api.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Snapshot de una opción de respuesta de una pregunta del modelo, con la ponderación que
 * otorgaba en el catálogo (pregunta_item_opcion) al generar la evaluación.
 */
@Entity
@Table(name = "modelo_opcion")
@Getter
@Setter
@NoArgsConstructor
public class ModeloOpcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modelo_pregunta_id", nullable = false)
    private ModeloPregunta modeloPregunta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_opcion_origen_id", nullable = false)
    private ItemOpcion itemOpcionOrigen;

    @Column(name = "orden_snapshot", nullable = false)
    private Integer ordenSnapshot;

    @Column(name = "descripcion_snapshot", nullable = false, length = 150)
    private String descripcionSnapshot;

    @Column(name = "ponderacion_snapshot", nullable = false, precision = 4, scale = 2)
    private BigDecimal ponderacionSnapshot;

    public ModeloOpcion(ModeloPregunta modeloPregunta, ItemOpcion origen, BigDecimal ponderacion) {
        this.modeloPregunta = modeloPregunta;
        this.itemOpcionOrigen = origen;
        this.ordenSnapshot = origen.getOrden();
        this.descripcionSnapshot = origen.getDescripcion();
        this.ponderacionSnapshot = ponderacion;
    }
}
