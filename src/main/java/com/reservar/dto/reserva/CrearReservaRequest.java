package com.reservar.dto.reserva;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class CrearReservaRequest {

    @NotNull(message = "El servicio es obligatorio.")
    private Long idServicio;

    @NotNull(message = "La fecha y hora de inicio son obligatorias.")
    @Future(message = "La fecha de inicio debe ser futura.")
    private LocalDateTime fechaInicio;
}