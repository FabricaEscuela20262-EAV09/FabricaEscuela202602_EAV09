package com.reservar.dto.reserva;

import com.reservar.entity.enums.EstadoReserva;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ReservaResponse {
    private Long idReserva;
    private Long idServicio;
    private String nombreServicio;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private EstadoReserva estado;
    private LocalDateTime fechaCreacion;
}