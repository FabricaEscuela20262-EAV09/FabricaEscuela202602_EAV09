package com.reservar.controller;

import com.reservar.dto.servicio.BloqueoDisponibilidadRequest;
import com.reservar.dto.servicio.DisponibilidadRequest;
import com.reservar.dto.servicio.DisponibilidadResponse;
import com.reservar.entity.Usuario;
import com.reservar.exception.BusinessException;
import com.reservar.exception.GlobalExceptionHandler;
import com.reservar.service.DisponibilidadServicioService;
import com.reservar.support.ServicioFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-08 a nivel HTTP: franjas semanales, bloqueos y errores 400/401/403. */
@ExtendWith(MockitoExtension.class)
class DisponibilidadServicioControllerTest {

    private static final String BASE = "/api/ofertas/10";
    private static final String FRANJA = """
            {"diaSemana":"LUNES","horaInicio":"09:00","horaFin":"12:00"}
            """;

    @Mock
    private DisponibilidadServicioService disponibilidadServicio;

    @InjectMocks
    private DisponibilidadServicioController controller;

    private MockMvc mockMvc;
    private Usuario proveedor;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        proveedor = ServicioFixtures.proveedorActivo();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(proveedor, null, List.of()));
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private static DisponibilidadResponse semanal() {
        DisponibilidadResponse response = new DisponibilidadResponse();
        response.setId(1L);
        response.setIdServicio(10L);
        response.setTipo("SEMANAL");
        response.setDiaSemana("LUNES");
        response.setHoraInicio(LocalTime.of(9, 0));
        response.setHoraFin(LocalTime.of(12, 0));
        return response;
    }

    private static DisponibilidadResponse bloqueo() {
        DisponibilidadResponse response = new DisponibilidadResponse();
        response.setId(2L);
        response.setIdServicio(10L);
        response.setTipo("BLOQUEO");
        response.setFecha(LocalDate.of(2030, 12, 25));
        response.setMotivo("OTRO");
        response.setDiaCompleto(true);
        return response;
    }

    @Test
    void deberiaResponder201CuandoSeDefineUnaFranjaSemanal() throws Exception {
        // Arrange
        when(disponibilidadServicio.agregarDisponibilidad(eq(proveedor), eq(10L), any(DisponibilidadRequest.class)))
                .thenReturn(semanal());

        // Act + Assert
        mockMvc.perform(post(BASE + "/disponibilidad").contentType(MediaType.APPLICATION_JSON).content(FRANJA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("SEMANAL"))
                .andExpect(jsonPath("$.diaSemana").value("LUNES"));

        ArgumentCaptor<DisponibilidadRequest> captor = ArgumentCaptor.forClass(DisponibilidadRequest.class);
        verify(disponibilidadServicio).agregarDisponibilidad(eq(proveedor), eq(10L), captor.capture());
        assertEquals(LocalTime.of(9, 0), captor.getValue().getHoraInicio());
        assertEquals(LocalTime.of(12, 0), captor.getValue().getHoraFin());
    }

    @Test
    void deberiaResponder400IndicandoLosCamposFaltantesDeLaFranja() throws Exception {
        // Act + Assert
        mockMvc.perform(post(BASE + "/disponibilidad").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.errores.length()").value(3));
        verifyNoInteractions(disponibilidadServicio);
    }

    @Test
    void deberiaResponder400ConMensajeEspecificoCuandoLaFranjaTerminaAntesDeEmpezar() throws Exception {
        // Arrange
        when(disponibilidadServicio.agregarDisponibilidad(any(), eq(10L), any())).thenThrow(new BusinessException(
                "INTERVALO_HORARIO_INVALIDO", "La hora de inicio debe ser anterior a la hora de fin."));

        // Act + Assert
        mockMvc.perform(post(BASE + "/disponibilidad").contentType(MediaType.APPLICATION_JSON).content(FRANJA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("INTERVALO_HORARIO_INVALIDO"))
                .andExpect(jsonPath("$.mensaje").value("La hora de inicio debe ser anterior a la hora de fin."));
    }

    @Test
    void deberiaResponder400ConMensajeEspecificoCuandoLaFranjaSeTraslapa() throws Exception {
        // Arrange
        when(disponibilidadServicio.agregarDisponibilidad(any(), eq(10L), any())).thenThrow(new BusinessException(
                "DISPONIBILIDAD_TRASLAPADA", "El intervalo se traslapa con otro horario del mismo día."));

        // Act + Assert
        mockMvc.perform(post(BASE + "/disponibilidad").contentType(MediaType.APPLICATION_JSON).content(FRANJA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DISPONIBILIDAD_TRASLAPADA"));
    }

    @Test
    void deberiaResponder403CuandoQuienModificaNoEsElProveedorDueno() throws Exception {
        // Arrange
        when(disponibilidadServicio.agregarDisponibilidad(any(), eq(10L), any())).thenThrow(new BusinessException(
                "OFERTA_NO_PROPIA", "No tienes permiso para modificar esta oferta."));

        // Act + Assert
        mockMvc.perform(post(BASE + "/disponibilidad").contentType(MediaType.APPLICATION_JSON).content(FRANJA))
                .andExpect(status().isForbidden());
    }

    @Test
    void deberiaResponder401CuandoNoHayUsuarioAutenticadoEnLasOperacionesDeEscritura() throws Exception {
        // Arrange
        SecurityContextHolder.clearContext();

        // Act + Assert
        mockMvc.perform(post(BASE + "/disponibilidad").contentType(MediaType.APPLICATION_JSON).content(FRANJA))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BASE + "/disponibilidad/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2030-12-25\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BASE + "/bloqueos/1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(disponibilidadServicio);
    }

    @Test
    void deberiaListarLasFranjasSemanalesSinRequerirSesion() throws Exception {
        // Arrange
        SecurityContextHolder.clearContext();
        when(disponibilidadServicio.listarDisponibilidad(10L)).thenReturn(List.of(semanal()));

        // Act + Assert
        mockMvc.perform(get(BASE + "/disponibilidad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].horaInicio").value("09:00:00"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deberiaResponder404CuandoSeListaLaDisponibilidadDeUnServicioNoDisponible() throws Exception {
        // Arrange
        when(disponibilidadServicio.listarDisponibilidad(77L)).thenThrow(
                new BusinessException("OFERTA_NO_DISPONIBLE", "La oferta no existe o no está activa."));

        // Act + Assert
        mockMvc.perform(get("/api/ofertas/77/disponibilidad")).andExpect(status().isNotFound());
    }

    @Test
    void deberiaResponder204CuandoSeEliminaUnaFranja() throws Exception {
        // Act + Assert
        mockMvc.perform(delete(BASE + "/disponibilidad/4")).andExpect(status().isNoContent());

        verify(disponibilidadServicio).eliminarDisponibilidad(proveedor, 10L, 4L);
    }

    @Test
    void deberiaResponder400CuandoLaFranjaAEliminarNoExiste() throws Exception {
        // Arrange
        doThrow(new BusinessException("DISPONIBILIDAD_NO_ENCONTRADA", "El intervalo de disponibilidad no existe."))
                .when(disponibilidadServicio).eliminarDisponibilidad(any(), eq(10L), eq(4L));

        // Act + Assert
        mockMvc.perform(delete(BASE + "/disponibilidad/4")).andExpect(status().isBadRequest());
    }

    @Test
    void deberiaResponder201CuandoSeBloqueaUnDiaCompleto() throws Exception {
        // Arrange
        when(disponibilidadServicio.crearBloqueo(eq(proveedor), eq(10L), any(BloqueoDisponibilidadRequest.class)))
                .thenReturn(bloqueo());

        // Act + Assert
        mockMvc.perform(post(BASE + "/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2030-12-25\",\"motivo\":\"OTRO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("BLOQUEO"))
                .andExpect(jsonPath("$.diaCompleto").value(true));

        ArgumentCaptor<BloqueoDisponibilidadRequest> captor = ArgumentCaptor.forClass(BloqueoDisponibilidadRequest.class);
        verify(disponibilidadServicio).crearBloqueo(eq(proveedor), eq(10L), captor.capture());
        assertEquals(LocalDate.of(2030, 12, 25), captor.getValue().getFecha());
        assertEquals("OTRO", captor.getValue().getMotivo());
    }

    @Test
    void deberiaResponder400CuandoElBloqueoNoTraeFecha() throws Exception {
        // Act + Assert
        mockMvc.perform(post(BASE + "/bloqueos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("fecha: Debe ingresar la fecha del bloqueo"));
        verifyNoInteractions(disponibilidadServicio);
    }

    @Test
    void deberiaResponder400CuandoElBloqueoEsParaUnaFechaPasada() throws Exception {
        // Arrange
        when(disponibilidadServicio.crearBloqueo(any(), eq(10L), any())).thenThrow(new BusinessException(
                "FECHA_BLOQUEO_INVALIDA", "No se puede crear un bloqueo para una fecha pasada."));

        // Act + Assert
        mockMvc.perform(post(BASE + "/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2000-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("FECHA_BLOQUEO_INVALIDA"));
    }

    @Test
    void deberiaListarLosBloqueosSinFiltroDeFechas() throws Exception {
        // Arrange
        SecurityContextHolder.clearContext();
        when(disponibilidadServicio.listarBloqueos(10L, null, null)).thenReturn(List.of(bloqueo()));

        // Act + Assert
        mockMvc.perform(get(BASE + "/bloqueos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fecha").value("2030-12-25"));
    }

    @Test
    void deberiaConvertirLasFechasDelRangoCuandoSeListanBloqueos() throws Exception {
        // Arrange
        when(disponibilidadServicio.listarBloqueos(eq(10L), eq(LocalDate.of(2030, 12, 1)), eq(LocalDate.of(2030, 12, 31))))
                .thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get(BASE + "/bloqueos?fechaInicio=2030-12-01&fechaFin=2030-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deberiaResponder400CuandoElRangoDeFechasEstaInvertido() throws Exception {
        // Arrange
        when(disponibilidadServicio.listarBloqueos(eq(10L), any(), isNull())).thenThrow(new BusinessException(
                "RANGO_FECHAS_INVALIDO", "La fecha final no puede ser anterior a la inicial."));

        // Act + Assert
        mockMvc.perform(get(BASE + "/bloqueos?fechaInicio=2030-12-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("RANGO_FECHAS_INVALIDO"));
    }

    @Test
    void deberiaResponder204CuandoSeEliminaUnBloqueo() throws Exception {
        // Act + Assert
        mockMvc.perform(delete(BASE + "/bloqueos/9")).andExpect(status().isNoContent());

        verify(disponibilidadServicio).eliminarBloqueo(proveedor, 10L, 9L);
    }
}
