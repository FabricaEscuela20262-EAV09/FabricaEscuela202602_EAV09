package com.reservar.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmarRecuperacionRequest {

    @NotBlank(message = "Debe proporcionar el token de recuperación")
    private String token;

    @NotBlank(message = "Debe proporcionar la nueva contraseña")
    private String nuevaContrasena;
}