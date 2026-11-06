package com.reservar.entity;

import com.reservar.entity.enums.EstadoReserva;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "\"HISTORIAL_RESERVA\"")
@Getter
@Setter
public class HistorialReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long idHistorial;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_reserva",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_historial_reserva")
    )
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_actor",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_historial_usuario")
    )
    private Usuario actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoReserva estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoReserva estadoNuevo;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;

    @Column(name = "motivo", length = 255)
    private String motivo;

    @PrePersist
    protected void onCreate() {
        if (fechaCambio == null) {
            fechaCambio = LocalDateTime.now();
        }
    }
}