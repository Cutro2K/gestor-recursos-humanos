package com.deso.api.model.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "puesto")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
public class Puesto {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, unique = true, length = 75)
    private String nombre;

    @Column(nullable = true, unique = false, columnDefinition = "TEXT")
    private String descripcion;
    
    @Column(nullable = false, unique = false, length = 100)
    private String empresa;

    @Column(nullable = false)
    private Boolean eliminado = false;
}
    