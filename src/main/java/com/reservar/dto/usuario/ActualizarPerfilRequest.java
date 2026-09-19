package com.reservar.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarPerfilRequest {

    @NotBlank(message = "Debe registrar su nombre completo")
    @Size(max = 100, message = "El nombre completo no puede superar los 100 caracteres")
    private String nombreCompleto;

    @Pattern(
            regexp = "^$|^(\\+57\\s?)?3\\d{2}\\s?\\d{3}\\s?\\d{4}$",
            message = "El teléfono debe tener un formato válido para Colombia"
    )
    private String telefono;

    @NotBlank(message = "Debe registrar su correo electrónico")
    @Email(message = "El correo debe tener un formato válido")
    @Size(max = 255, message = "El correo no puede superar los 255 caracteres")
    private String correo;

    private String tipoServicio;

    private String descripcion;
}