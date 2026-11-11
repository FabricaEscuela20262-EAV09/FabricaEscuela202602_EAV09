package com.reservar.dto.reserva;

import java.time.LocalDateTime;

public record CalificacionResponse(Long idCalificacion, Long idReserva, Short estrellas, String comentario,
                                   LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
}
