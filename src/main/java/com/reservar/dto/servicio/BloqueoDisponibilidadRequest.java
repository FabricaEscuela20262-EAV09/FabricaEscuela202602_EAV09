package com.reservar.dto.servicio;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class BloqueoDisponibilidadRequest {
    @NotNull(message = "Debe ingresar la fecha del bloqueo")
    private LocalDate fecha;

    @NotNull(message = "Debe ingresar la hora de inicio del bloqueo")
    private LocalTime horaInicio;

    @NotNull(message = "Debe ingresar la hora de finalización del bloqueo")
    private LocalTime horaFin;

    @NotBlank(message = "Debe ingresar el motivo del bloqueo")
    private String motivo;
}