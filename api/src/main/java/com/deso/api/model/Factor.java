package com.deso.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "factor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Factor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Many-to-One: Muchos factores pertenecen a una misma competencia
    // El FetchType.LAZY hace que los valores permanezcan nulos
    // Hasta que se haga uso de ellos. 
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "competencia_id", nullable = false)
    private Competencia competencia;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(nullable = false)
    private Integer orden;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Boolean eliminado = false;
}