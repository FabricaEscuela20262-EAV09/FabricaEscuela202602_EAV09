package com.reservar.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistroClienteRequest {

    @NotBlank(message = "Debe registrar su nombre completo")
    @Size(max = 100, message = "Su nombre completo no puede superar 100 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "Debe registrar correo electrónico")
    @Email(message = "El correo ingresado tiene un formato inválido")
    private String correo;

    @NotBlank(message = "Debe registrar contraseña")
    private String contrasena;
}
