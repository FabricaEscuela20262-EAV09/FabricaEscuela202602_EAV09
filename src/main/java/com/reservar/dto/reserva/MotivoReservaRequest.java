package com.reservar.dto.reserva;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MotivoReservaRequest {
    @Size(max = 255, message = "El motivo no puede superar 255 caracteres.")
    private String motivo;
}
