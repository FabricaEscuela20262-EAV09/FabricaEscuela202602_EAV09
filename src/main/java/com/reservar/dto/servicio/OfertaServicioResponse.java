package com.reservar.dto.servicio;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class OfertaServicioResponse {
    private Long idServicio;
    private Long idProveedor;
    private String nombreProveedor;
    private Long idTipoServicio;
    private String nombreTipoServicio;
    private Long idCategoria;
    private String nombreCategoria;
    private String nombre;
    private String descripcion;
    private Integer duracionMinutos;
    private String modalidad;
    private String ubicacion;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private List<DisponibilidadResponse> proximasDisponibilidades =
            new ArrayList<>();
}