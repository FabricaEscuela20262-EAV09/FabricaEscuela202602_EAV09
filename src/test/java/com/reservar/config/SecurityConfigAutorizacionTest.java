package com.reservar.config;

import com.reservar.controller.CatalogoServicioController;
import com.reservar.controller.DisponibilidadServicioController;
import com.reservar.controller.OfertaServicioController;
import com.reservar.dto.servicio.BusquedaOfertasResponse;
import com.reservar.entity.Sesion;
import com.reservar.exception.GlobalExceptionHandler;
import com.reservar.repository.SesionRepository;
import com.reservar.security.SesionAuthenticationFilter;
import com.reservar.service.CatalogoServicioService;
import com.reservar.service.DisponibilidadServicioService;
import com.reservar.service.OfertaServicioService;
import com.reservar.support.ServicioFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Requisito transversal de la Épica 2: las consultas son públicas, pero crear/editar/desactivar servicios
 * y disponibilidad exigen sesión (401) y la autorización de dueño se resuelve en el servidor.
 */
@WebMvcTest(controllers = {
        OfertaServicioController.class,
        DisponibilidadServicioController.class,
        CatalogoServicioController.class})
@Import({SecurityConfig.class, SesionAuthenticationFilter.class, GlobalExceptionHandler.class})
class SecurityConfigAutorizacionTest {

    private static final String JSON_OFERTA = """
            {"idTipoServicio":2,"nombre":"X","descripcion":"d","duracionMinutos":30,"modalidad":"REMOTO"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OfertaServicioService ofertaServicio;
    @MockitoBean
    private DisponibilidadServicioService disponibilidadServicio;
    @MockitoBean
    private CatalogoServicioService catalogoServicio;
    @MockitoBean
    private SesionRepository sesionRepository;

    @Test
    void deberiaPermitirSinSesionLasConsultasPublicas() throws Exception {
        // Act
        when(ofertaServicio.buscarOfertas(any(), any(), any())).thenReturn(
                new BusquedaOfertasResponse(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0), "vacío"));

        // Act + Assert
        mockMvc.perform(get("/api/ofertas")).andExpect(status().isOk());
        mockMvc.perform(get("/api/ofertas/10")).andExpect(status().isOk());
        mockMvc.perform(get("/api/ofertas/10/disponibilidad")).andExpect(status().isOk());
        mockMvc.perform(get("/api/ofertas/10/bloqueos")).andExpect(status().isOk());
        mockMvc.perform(get("/api/catalogo/categorias")).andExpect(status().isOk());
        mockMvc.perform(get("/api/catalogo/tipos-servicio")).andExpect(status().isOk());
    }

    @Test
    void deberiaResponder401EnLasOperacionesDeEscrituraSinSesion() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/api/ofertas").contentType(MediaType.APPLICATION_JSON).content(JSON_OFERTA))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/ofertas/10").contentType(MediaType.APPLICATION_JSON).content(JSON_OFERTA))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/ofertas/10/desactivar")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/ofertas/10/disponibilidad").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diaSemana\":\"LUNES\",\"horaInicio\":\"09:00\",\"horaFin\":\"10:00\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/ofertas/10/disponibilidad/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/ofertas/10/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2030-12-25\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/ofertas/10/bloqueos/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void deberiaResponder401AlConsultarLasOfertasPropiasSinSesion() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/api/ofertas/mis-ofertas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/ofertas/mis-ofertas/historial")).andExpect(status().isUnauthorized());
    }

    @Test
    void deberiaResponder401CuandoElBearerTokenNoCorrespondeAUnaSesionVigente() throws Exception {
        // Arrange
        when(sesionRepository.findByHashTokenAndFechaRevocacionIsNullAndFechaExpiracionAfter(any(), any()))
                .thenReturn(Optional.empty());

        // Act + Assert
        mockMvc.perform(patch("/api/ofertas/10/desactivar").header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deberiaDejarPasarAlControladorCuandoHayUnaSesionVigente() throws Exception {
        // Arrange
        Sesion sesion = new Sesion();
        sesion.setUsuario(ServicioFixtures.proveedorActivo());
        sesion.setFechaExpiracion(LocalDateTime.now().plusHours(1));
        when(sesionRepository.findByHashTokenAndFechaRevocacionIsNullAndFechaExpiracionAfter(any(), any()))
                .thenReturn(Optional.of(sesion));

        // Act + Assert
        mockMvc.perform(patch("/api/ofertas/10/desactivar").header("Authorization", "Bearer valido"))
                .andExpect(status().isNoContent());
    }
}
