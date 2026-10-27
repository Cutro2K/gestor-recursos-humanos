package com.deso.api.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "item_opcion",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_item_opcion_orden", columnNames = {"opcion_respuesta_id", "orden"}),
        @UniqueConstraint(name = "uq_item_opcion_descripcion", columnNames = {"opcion_respuesta_id", "descripcion"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemOpcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Many-to-One: El ítem pertenece obligatoriamente a una OpcionRespuesta
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opcion_respuesta_id", nullable = false)
    private OpcionRespuesta opcionRespuesta;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false, length = 150)
    private String descripcion;
}