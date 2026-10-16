package com.reservar.dto.servicio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class DisponibilidadRequest {
    @NotBlank(message = "Debe ingresar el día de la semana")
    private String diaSemana;

    @NotNull(message = "Debe ingresar la hora de inicio del servicio")
    private LocalTime horaInicio;

    @NotNull(message = "Debe ingresar la hora de finalización del servicio")
    private LocalTime horaFin;
}
