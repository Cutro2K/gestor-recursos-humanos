package com.deso.api.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "bloque_cuestionario",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_bloque_cuestionario_numero", columnNames = {"cuestionario_id", "numero"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BloqueCuestionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Many-to-One: El bloque pertenece a un cuestionario determinado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuestionario_id", nullable = false)
    private Cuestionario cuestionario;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false)
    private Boolean completado = false;

    @Column(name = "fecha_completado")
    private LocalDateTime fechaCompletado;

    // Relación One-to-Many: Un bloque contiene un conjunto de preguntas asignadas
    @OneToMany(mappedBy = "bloqueCuestionario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PreguntaCuestionario> preguntas = new ArrayList<>();
}
