package com.reservar.controller;

import com.reservar.dto.reserva.CrearReservaRequest;
import com.reservar.dto.reserva.ReservaResponse;
import com.reservar.entity.Usuario;
import com.reservar.entity.enums.EstadoReserva;
import com.reservar.entity.enums.EstadoUsuario;
import com.reservar.entity.enums.Rol;
import com.reservar.exception.BusinessException;
import com.reservar.exception.GlobalExceptionHandler;
import com.reservar.service.ReservaService;
import com.reservar.support.ServicioFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-11: contrato HTTP de POST /api/reservas. */
@ExtendWith(MockitoExtension.class)
class ReservaControllerTest {

    private static final String FUTURA = LocalDateTime.now().plusDays(3).withNano(0).toString();

    @Mock
    private ReservaService service;

    private MockMvc mvc;
    private Usuario cliente;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ReservaController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        cliente = ServicioFixtures.usuario(Rol.CLIENTE, EstadoUsuario.ACTIVO);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(cliente, null, List.of()));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static String cuerpo(String idServicio, String fechaInicio) {
        return "{\"idServicio\":" + idServicio + ",\"fechaInicio\":" + fechaInicio + "}";
    }

    @Test
    void deberiaResponder201ConLaReservaCreada() throws Exception {
        // Arrange
        ReservaResponse respuesta = new ReservaResponse();
        respuesta.setIdReserva(99L);
        respuesta.setEstado(EstadoReserva.PENDIENTE);
        when(service.crearReserva(eq(cliente), any(CrearReservaRequest.class))).thenReturn(respuesta);

        // Act
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("10", "\"" + FUTURA + "\"")))
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idReserva").value(99))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        ArgumentCaptor<CrearReservaRequest> captor = ArgumentCaptor.forClass(CrearReservaRequest.class);
        verify(service).crearReserva(eq(cliente), captor.capture());
        assertEquals(10L, captor.getValue().getIdServicio());
        assertEquals(LocalDateTime.parse(FUTURA), captor.getValue().getFechaInicio());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "sin servicio | null | FUTURA",
            "sin fecha | 10 | null",
            "fecha pasada | 10 | \"2020-01-01T09:00:00\""
    })
    void deberiaResponder400CuandoLaSolicitudEsInvalida(String caso, String idServicio, String fecha) throws Exception {
        // Arrange
        String fechaJson = "FUTURA".equals(fecha) ? "\"" + FUTURA + "\"" : fecha;

        // Act + Assert
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(idServicio, fechaJson)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "HORARIO_NO_DISPONIBLE,409",
            "HORARIO_BLOQUEADO,409",
            "FUERA_DE_DISPONIBILIDAD,409",
            "RESERVA_FUERA_DE_HORARIO,409",
            "OFERTA_NO_DISPONIBLE,404",
            "ROL_NO_AUTORIZADO,403",
            "CUENTA_NO_VERIFICADA,403",
            "AUTORRESERVA_NO_PERMITIDA,400",
            "CUENTA_INACTIVA,400"
    })
    void deberiaTraducirLosErroresDeNegocioAlEstadoHttp(String codigo, int estado) throws Exception {
        // Arrange
        when(service.crearReserva(eq(cliente), any())).thenThrow(new BusinessException(codigo, "mensaje"));

        // Act + Assert
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("10", "\"" + FUTURA + "\"")))
                .andExpect(status().is(estado))
                .andExpect(jsonPath("$.codigo").value(codigo));
    }

    @Test
    void deberiaResponder401SinAutenticacion() throws Exception {
        // Arrange
        SecurityContextHolder.clearContext();

        // Act + Assert
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("10", "\"" + FUTURA + "\"")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("USUARIO_NO_AUTENTICADO"));
        verifyNoInteractions(service);
    }

    @Test
    void deberiaResponder401ConUnPrincipalQueNoEsUsuario() throws Exception {
        // Arrange
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "clave", "anonimo", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        // Act + Assert
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("10", "\"" + FUTURA + "\"")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void deberiaResponder401ConUnaAutenticacionNoValidada() throws Exception {
        // Arrange
        UsernamePasswordAuthenticationToken noAutenticada = new UsernamePasswordAuthenticationToken(cliente, null);
        SecurityContextHolder.getContext().setAuthentication(noAutenticada);

        // Act + Assert
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("10", "\"" + FUTURA + "\"")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
