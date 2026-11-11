package com.reservar.controller;

import com.reservar.dto.reserva.CalificacionResponse;
import com.reservar.entity.Usuario;
import com.reservar.entity.enums.*;
import com.reservar.exception.*;
import com.reservar.service.CalificacionService;
import com.reservar.support.ServicioFixtures;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class CalificacionControllerTest {
    @Mock CalificacionService service;
    MockMvc mvc;
    Usuario cliente;
    String url = "/api/reservas/1/calificacion";
    @BeforeEach void preparar() {
        cliente = ServicioFixtures.usuario(Rol.CLIENTE, EstadoUsuario.ACTIVO);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(cliente, null, List.of()));
        mvc = MockMvcBuilders.standaloneSetup(new CalificacionController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }
    @Test void creaYEdita() throws Exception {
        var response = new CalificacionResponse(1L, 1L, (short) 5, null, null, null);
        when(service.crear(eq(cliente), eq(1L), any())).thenReturn(response);
        when(service.editar(eq(cliente), eq(1L), any())).thenReturn(response);
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"estrellas\":5}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.idCalificacion").value(1));
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content("{\"estrellas\":5}"))
                .andExpect(status().isOk());
    }
    @ParameterizedTest @ValueSource(strings = {"{}", "{\"estrellas\":0}", "{\"estrellas\":6}"})
    void validaEstrellas(String body) throws Exception {
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        verifyNoInteractions(service);
    }
    @Test void validaComentario() throws Exception {
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estrellas\":5,\"comentario\":\"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void duplicadaEs409() throws Exception {
        when(service.crear(eq(cliente), eq(1L), any())).thenThrow(new BusinessException("CALIFICACION_EXISTENTE", "Editar"));
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"estrellas\":5}"))
                .andExpect(status().isConflict());
    }
    @Test void ajenaEs403() throws Exception {
        when(service.crear(eq(cliente), eq(1L), any())).thenThrow(new BusinessException("RESERVA_NO_PROPIA", "Ajena"));
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"estrellas\":5}"))
                .andExpect(status().isForbidden());
    }
    @Test void sinSesion() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"estrellas\":5}"))
                .andExpect(status().isUnauthorized());
    }
}
