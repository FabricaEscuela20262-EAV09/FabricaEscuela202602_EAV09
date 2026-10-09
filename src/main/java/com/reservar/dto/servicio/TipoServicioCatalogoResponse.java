package com.reservar.dto.servicio;

public record TipoServicioCatalogoResponse(
        Long idTipoServicio,
        Long idCategoria,
        String categoria,
        String nombre,
        String descripcion
) {}