package com.reservar.controller;

import com.reservar.dto.reserva.ReservaResponse;
import com.reservar.entity.Usuario;
import com.reservar.entity.enums.EstadoReserva;
import com.reservar.exception.BusinessException;
import com.reservar.exception.GlobalExceptionHandler;
import com.reservar.service.GestionReservaService;
import com.reservar.support.ServicioFixtures;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GestionReservaControllerTest {
    @Mock GestionReservaService service;
    MockMvc mvc;
    Usuario proveedor;

    @BeforeEach void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new GestionReservaController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver()).build();
        proveedor = ServicioFixtures.proveedorActivo();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(proveedor, null, List.of()));
    }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }

    @Test void confirma() throws Exception {
        ReservaResponse response = new ReservaResponse();
        response.setEstado(EstadoReserva.CONFIRMADA);
        when(service.confirmar(proveedor, 1L)).thenReturn(response);
        mvc.perform(patch("/api/reservas/1/confirmar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }
    @Test void rechazaSinCuerpo() throws Exception {
        mvc.perform(patch("/api/reservas/1/rechazar")).andExpect(status().isOk());
        verify(service).rechazar(proveedor, 1L, null);
    }
    @Test void rechazaConMotivo() throws Exception {
        mvc.perform(patch("/api/reservas/1/rechazar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Ausencia\"}")).andExpect(status().isOk());
        verify(service).rechazar(proveedor, 1L, "Ausencia");
    }
    @Test void validaMotivo() throws Exception {
        mvc.perform(patch("/api/reservas/1/rechazar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"" + "x".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        verifyNoInteractions(service);
    }
    @Test void ajenaEs403() throws Exception {
        when(service.confirmar(proveedor, 1L)).thenThrow(new BusinessException("RESERVA_NO_PROPIA", "Ajena"));
        mvc.perform(patch("/api/reservas/1/confirmar")).andExpect(status().isForbidden());
    }
    @Test void inexistenteEs404() throws Exception {
        when(service.confirmar(proveedor, 1L)).thenThrow(new BusinessException("RESERVA_NO_ENCONTRADA", "No existe"));
        mvc.perform(patch("/api/reservas/1/confirmar")).andExpect(status().isNotFound());
    }
    @Test void cancela() throws Exception {
        ReservaResponse response = new ReservaResponse();
        response.setEstado(EstadoReserva.CANCELADA);
        when(service.cancelar(proveedor, 1L, null)).thenReturn(response);
        mvc.perform(patch("/api/reservas/1/cancelar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }
    @Test void cancelarAjenaEs403() throws Exception {
        when(service.cancelar(proveedor, 1L, null)).thenThrow(new BusinessException("RESERVA_NO_PROPIA", "Ajena"));
        mvc.perform(patch("/api/reservas/1/cancelar")).andExpect(status().isForbidden());
    }
    @Test void historialPaginadoFiltrado() throws Exception {
        when(service.historial(eq(proveedor), eq(EstadoReserva.CANCELADA), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(),
                        org.springframework.data.domain.PageRequest.of(0, 5), 0));
        mvc.perform(get("/api/reservas?estado=CANCELADA&page=0&size=5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
    }
    @Test void estadoFiltroInvalido() throws Exception {
        mvc.perform(get("/api/reservas?estado=INVENTADO")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void consultaAjenaEs403() throws Exception {
        when(service.detalle(proveedor, 1L)).thenThrow(new BusinessException("RESERVA_NO_PROPIA", "Ajena"));
        mvc.perform(get("/api/reservas/1")).andExpect(status().isForbidden());
    }
    @Test void auditoria() throws Exception {
        when(service.auditoria(proveedor, 1L)).thenReturn(List.of());
        mvc.perform(get("/api/reservas/1/historial")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
    @Test void sinSesionEs401() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(patch("/api/reservas/1/confirmar")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
