package com.deso.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PreguntaItemOpcionId implements Serializable {

    @Column(name = "pregunta_id")
    private Long preguntaId;

    @Column(name = "item_opcion_id")
    private Long itemOpcionId;
}