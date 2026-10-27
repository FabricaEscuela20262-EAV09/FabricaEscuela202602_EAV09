package com.reservar.dto.servicio;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusquedaOfertasResponseTest {

    @Test
    void deberiaConservarElContenidoYLaPaginacionDeLaPaginaOriginal() {
        // Arrange
        OfertaServicioResponse oferta = new OfertaServicioResponse();
        oferta.setNombre("Limpieza dental");
        PageImpl<OfertaServicioResponse> pagina = new PageImpl<>(List.of(oferta), PageRequest.of(1, 1), 3);

        // Act
        BusquedaOfertasResponse respuesta = new BusquedaOfertasResponse(pagina, null);

        // Assert
        assertAll(
                () -> assertEquals(1, respuesta.getContent().size()),
                () -> assertEquals("Limpieza dental", respuesta.getContent().get(0).getNombre()),
                () -> assertEquals(3, respuesta.getTotalElements()),
                () -> assertEquals(3, respuesta.getTotalPages()),
                () -> assertEquals(1, respuesta.getNumber()),
                () -> assertNull(respuesta.getMensaje())
        );
    }

    @Test
    void deberiaExponerElMensajeInformativoCuandoNoHayResultados() {
        // Arrange
        PageImpl<OfertaServicioResponse> vacia = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        // Act
        BusquedaOfertasResponse respuesta = new BusquedaOfertasResponse(vacia, "Sin resultados");

        // Assert
        assertAll(
                () -> assertTrue(respuesta.getContent().isEmpty()),
                () -> assertEquals("Sin resultados", respuesta.getMensaje())
        );
    }

    @Test
    void deberiaInicializarLasProximasDisponibilidadesComoListaVacia() {
        // Act + Assert
        assertTrue(new OfertaServicioResponse().getProximasDisponibilidades().isEmpty());
    }
}
