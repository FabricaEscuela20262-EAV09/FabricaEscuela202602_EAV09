package com.reservar.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolicitudRecuperacionRequest {

    @NotBlank(message = "Debe registrar su correo electrónico")
    @Email(message = "El correo ingresado tiene un formato inválido")
    private String correo;
}