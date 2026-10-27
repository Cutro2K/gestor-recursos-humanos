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
@Table(name = "evaluacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Evaluacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Many-to-One: Consultor que inició la evaluación
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultor_id", nullable = false)
    private Consultor consultor;

    // Relación Many-to-One: Puesto para el cual se evalúan los candidatos
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puesto_id", nullable = false)
    private Puesto puesto;

    // Relación Many-to-One: Configuración de cuestionarios vigente aplicada
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "configuracion_id", nullable = false)
    private ConfiguracionCuestionario configuracion;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "version_modelo", nullable = false, length = 30)
    private String versionModelo;

    @Column(name = "nombre_archivo_excel", length = 255)
    private String nombreArchivoExcel;

    // Relación One-to-Many: Una evaluación genera un cuestionario por cada candidato evaluado
    @OneToMany(mappedBy = "evaluacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cuestionario> cuestionarios = new ArrayList<>();
}
