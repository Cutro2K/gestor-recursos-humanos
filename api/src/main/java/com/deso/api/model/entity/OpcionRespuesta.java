package com.deso.api.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "opcion_respuesta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpcionRespuesta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Boolean eliminada = false;

    // Relación One-to-Many: Una opción de respuesta se compone de varios ítems 
    // cascade = CascadeType.ALL permite que al guardar la OpcionRespuesta se guarden automáticamente sus ítems
    @OneToMany(mappedBy = "opcionRespuesta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemOpcion> items = new ArrayList<>();
}