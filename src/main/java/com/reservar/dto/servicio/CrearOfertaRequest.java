package com.reservar.dto.servicio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearOfertaRequest {
    @NotNull(message = "Debe ingresar tipo de servicio para la oferta")
    @Positive(message = "El tipo de servicio ingresado debe ser válido")
    private Long idTipoServicio;

    @NotBlank(message = "Debe ingresar nombre de la oferta")
    @Size(max = 150, message = "El nombre de la oferta no puede superar 150 caracteres")
    private String nombre;

    @NotBlank(message = "Debe ingresar descripción de la oferta")
    private String descripcion;

    @NotNull(message = "Debe ingresar duración para la oferta")
    @Positive(message = "La duración de la oferta debe ser mayor que cero")
    private Integer duracionMinutos;

    @NotBlank(message = "Debe ingresar una modalidad para la oferta")
    private String modalidad;

    @Size(max = 255, message = "La ubicación no puede superar 255 caracteres")
    private String ubicacion;
}