package com.deso.api.model;

import com.deso.api.model.enums.TipoCompetencia;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity 
@Table(name = "competencia")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
public class Competencia {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, unique = true, length = 75)
    private String nombre;

    @Column(nullable = true, unique = false, columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = false, length = 20)
    private TipoCompetencia tipo;

    @Column(nullable = false)
    private Boolean eliminada = false;
}
