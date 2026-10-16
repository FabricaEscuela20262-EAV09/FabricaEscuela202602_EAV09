package com.reservar.dto.servicio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

// Página de resultados de la búsqueda (mismos campos que Page) más un mensaje
// informativo, que solo se informa cuando la búsqueda no encuentra ninguna oferta.
public class BusquedaOfertasResponse extends PageImpl<OfertaServicioResponse> {

    private final String mensaje;

    public BusquedaOfertasResponse(Page<OfertaServicioResponse> pagina, String mensaje) {
        super(pagina.getContent(), pagina.getPageable(), pagina.getTotalElements());
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }
}
