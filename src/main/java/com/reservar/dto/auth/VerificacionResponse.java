package com.reservar.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VerificacionResponse {

    private String mensaje;
    private String estado;
}
