package com.reservar.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerificacionRequest {

    @NotBlank(message = "Debe proporcionar el token de verificación")
    private String token;
}

