package com.reservar.dto.reserva;

import com.reservar.entity.enums.EstadoReserva;
import java.time.LocalDateTime;
import java.util.UUID;

public record HistorialReservaResponse(Long idHistorial, UUID idActor, EstadoReserva estadoAnterior,
                                      EstadoReserva estadoNuevo, LocalDateTime fechaCambio, String motivo) {
}
