package com.reservar.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "\"BLOQUEO_DISPONIBILIDAD\"")
@Getter
@Setter
public class BloqueoDisponibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bloqueo")
    private Long idBloqueo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_servicio",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_bloqueo_servicio")
    )
    private OfertaServicio servicio;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "motivo", nullable = false, length = 20)
    private String motivo;

    @PrePersist
    protected void onCreate() {
        if (motivo == null) {
            motivo = "OTRO";
        }
    }
}