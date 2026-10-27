package com.deso.api.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "registro_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistroAuditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Many-to-One: Consultor que ejecutó la acción auditada
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultor_id", nullable = false)
    private Consultor consultor;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Column(nullable = false, length = 50)
    private String accion;

    @Column(nullable = false, length = 50)
    private String entidad;

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    @Column(columnDefinition = "TEXT")
    private String detalle;
}
