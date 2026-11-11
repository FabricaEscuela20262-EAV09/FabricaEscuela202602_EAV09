package com.reservar.dto.reserva;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalificacionRequest {
    @NotNull(message = "Las estrellas son obligatorias.")
    @Min(value = 1, message = "Las estrellas deben estar entre 1 y 5.")
    @Max(value = 5, message = "Las estrellas deben estar entre 1 y 5.")
    private Short estrellas;

    @Size(max = 500, message = "El comentario no puede superar 500 caracteres.")
    private String comentario;
}
