package com.reservar.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private UUID idUsuario;
    private String nombreCompleto;
    private String correo;
    private String rol;
    private String estado;
    private String token;
    private String mensaje;
}
