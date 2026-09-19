package com.reservar.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistroProveedorRequest {

    @NotBlank(message = "Debe registrar su nombre completo")
    @Size(max = 100, message = "Su nombre completo no puede superar 100 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "Debe registrar correo electrónico")
    @Email(message = "El correo ingresado tiene un formato inválido")
    private String correo;

    @NotBlank(message = "Debe registrar contraseña")
    private String contrasena;

    @NotBlank(message = "Debe registrar código de proveedor")
    @Size(max = 50, message = "El código de proveedor no puede superar 50 caracteres")
    private String codigoProveedor;

    @NotBlank(message = "Debe registrar tipo de servicio")
    @Size(max = 100, message = "El tipo de servicio no puede superar 100 caracteres")
    private String tipoServicio;

    @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
    private String descripcion;
}
