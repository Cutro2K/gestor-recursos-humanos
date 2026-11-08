package com.deso.api.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Sesión abierta por un login (CU 01). Pertenece a un consultor o al cuestionario de un
 * candidato, nunca a ambos. {@code seq} cambia en cada refresco: el refresh token que lo
 * lleva queda viejo apenas se usa.
 */
@Entity
@Table(name = "sesion")
@Getter
@Setter
@NoArgsConstructor
public class Sesion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultor_id")
    private Consultor consultor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuestionario_id")
    private Cuestionario cuestionario;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(nullable = false)
    private Long seq = 0L;
}
