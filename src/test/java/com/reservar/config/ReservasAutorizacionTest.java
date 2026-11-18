package com.reservar.config;

import com.reservar.controller.GestionReservaController;
import com.reservar.controller.CalificacionController;
import com.reservar.exception.GlobalExceptionHandler;
import com.reservar.repository.SesionRepository;
import com.reservar.security.SesionAuthenticationFilter;
import com.reservar.service.GestionReservaService;
import com.reservar.service.CalificacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {GestionReservaController.class, CalificacionController.class})
@Import({SecurityConfig.class, SesionAuthenticationFilter.class, GlobalExceptionHandler.class})
class ReservasAutorizacionTest {
    @Autowired MockMvc mvc;
    @MockitoBean GestionReservaService gestion;
    @MockitoBean CalificacionService calificacion;
    @MockitoBean SesionRepository sesiones;

    @Test void todasLasRutasExigenSesion() throws Exception {
        mvc.perform(get("/api/reservas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reservas/historial")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reservas/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reservas/1/historial")).andExpect(status().isUnauthorized());
        for (String accion : new String[]{"confirmar", "rechazar", "cancelar"}) {
            mvc.perform(patch("/api/reservas/1/" + accion)).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/reservas/1/calificacion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"estrellas\":5}")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/reservas/1/calificacion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"estrellas\":4}")).andExpect(status().isUnauthorized());
    }
}
