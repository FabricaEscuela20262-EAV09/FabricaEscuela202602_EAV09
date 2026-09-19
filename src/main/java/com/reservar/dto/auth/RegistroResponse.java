package com.reservar.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class RegistroResponse {

    private UUID idUsuario;
    private String mensaje;
    private String estado;
}
