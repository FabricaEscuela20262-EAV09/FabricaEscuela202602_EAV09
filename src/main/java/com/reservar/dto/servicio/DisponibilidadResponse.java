package com.reservar.dto.servicio;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class DisponibilidadResponse {
    private Long id;
    private Long idServicio;
    private String tipo;

    // Disponibilidad semanal
    private String diaSemana;

    // Bloqueo puntual
    private LocalDate fecha;
    private String motivo;

    // Comunes a ambos
    private LocalTime horaInicio;
    private LocalTime horaFin;
}
