package com.reservar.dto.usuario;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PerfilResponse {

    private String nombreCompleto;
    private String correo;
    private String correoPendiente;
    private String telefono;
    private String rol;
    private String tipoServicio;
    private String descripcion;
}