package com.reservar.dto.servicio;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class BloqueoDisponibilidadRequest {
    @NotNull(message = "Debe ingresar la fecha del bloqueo")
    private LocalDate fecha;

    // Opcionales: si ambas se omiten, se bloquea el día completo.
    private LocalTime horaInicio;

    private LocalTime horaFin;

    // Opcional: si se omite, se registra como OTRO.
    private String motivo;
}