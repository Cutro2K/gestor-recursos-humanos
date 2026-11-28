package com.deso.api.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "pregunta_item_opcion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PreguntaItemOpcion {

    @EmbeddedId
    private PreguntaItemOpcionId id = new PreguntaItemOpcionId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("preguntaId")
    @JoinColumn(name = "pregunta_id", nullable = false)
    private Pregunta pregunta;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("itemOpcionId")
    @JoinColumn(name = "item_opcion_id", nullable = false)
    private ItemOpcion itemOpcion;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal ponderacion;
}