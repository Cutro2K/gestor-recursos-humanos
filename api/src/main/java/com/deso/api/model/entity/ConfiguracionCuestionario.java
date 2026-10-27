package com.deso.api.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_cuestionario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionCuestionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vigencia_desde", nullable = false)
    private LocalDateTime vigenciaDesde;

    @Column(name = "vigencia_hasta")
    private LocalDateTime vigenciaHasta;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String instrucciones;

    @Column(name = "cantidad_maxima_accesos", nullable = false)
    private Integer cantidadMaximaAccesos;

    @Column(name = "minutos_maximos_activo", nullable = false)
    private Integer minutosMaximosActivo;

    @Column(name = "minutos_maximos_resolucion", nullable = false)
    private Integer minutosMaximosResolucion;

    @Column(name = "preguntas_por_bloque", nullable = false)
    private Integer preguntasPorBloque;

    @Column(nullable = false)
    private Boolean activa = true;
}
