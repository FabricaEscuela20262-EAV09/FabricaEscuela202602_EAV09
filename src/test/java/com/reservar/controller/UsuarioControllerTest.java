package com.reservar.controller;

import com.reservar.dto.usuario.ActualizarPerfilRequest;
import com.reservar.dto.usuario.PerfilResponse;
import com.reservar.exception.BusinessException;
import com.reservar.exception.GlobalExceptionHandler;
import com.reservar.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController usuarioController;

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(usuarioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void deberiaResponder200ConElPerfilCuandoSeConsulta() throws Exception {
        // Arrange
        when(usuarioService.obtenerPerfil()).thenReturn(new PerfilResponse(
                "Luis", "luis@correo.com", null, "3001234567", "PROVEEDOR", "Plomería", "Reparaciones"));

        // Act + Assert
        mockMvc.perform(get("/api/usuario/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("PROVEEDOR"))
                .andExpect(jsonPath("$.tipoServicio").value("Plomería"));
    }

    @Test
    void deberiaResponder200ConConfirmacionCuandoElPerfilSeActualiza() throws Exception {
        // Arrange (HU-03 escenario 1)
        String body = """
                {"nombreCompleto":"Ana","telefono":"+57 300 1234567","correo":"ana@correo.com"}
                """;

        // Act + Assert
        mockMvc.perform(put("/api/usuario/perfil").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("Perfil actualizado correctamente")));
        verify(usuarioService).actualizarPerfil(any(ActualizarPerfilRequest.class));
    }

    @Test
    void deberiaResponder400ConMensajeDeNombreCuandoElNombreEstaVacio() throws Exception {
        // Arrange (HU-03 escenario 2)
        String body = """
                {"nombreCompleto":"","correo":"ana@correo.com"}
                """;

        // Act + Assert
        mockMvc.perform(put("/api/usuario/perfil").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("nombreCompleto: Debe registrar su nombre completo"));
        verifyNoInteractions(usuarioService);
    }

    @Test
    void deberiaResponder400ConMensajeDeTelefonoCuandoElTelefonoNoEsColombiano() throws Exception {
        // Arrange (HU-03 escenario 2)
        String body = """
                {"nombreCompleto":"Ana","telefono":"12345","correo":"ana@correo.com"}
                """;

        // Act + Assert
        mockMvc.perform(put("/api/usuario/perfil").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]")
                        .value("telefono: El teléfono debe tener un formato válido para Colombia"));
        verifyNoInteractions(usuarioService);
    }

    @Test
    void deberiaResponder400ConCorreoEnUsoCuandoElServicioRechazaElCambio() throws Exception {
        // Arrange (HU-03 escenario 3)
        doThrow(new BusinessException("CORREO_EN_USO", "Este correo ya está en uso"))
                .when(usuarioService).actualizarPerfil(any(ActualizarPerfilRequest.class));
        String body = """
                {"nombreCompleto":"Ana","correo":"otro@correo.com"}
                """;

        // Act + Assert
        mockMvc.perform(put("/api/usuario/perfil").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Este correo ya está en uso"));
    }

    @Test
    void deberiaResponder200CuandoSeConfirmaElCambioDeCorreo() throws Exception {
        // Arrange (sin stubs: método void)

        // Act + Assert
        mockMvc.perform(get("/api/usuario/correo/confirmar/tok123"))
                .andExpect(status().isOk())
                .andExpect(content().string("Correo actualizado correctamente"));
        verify(usuarioService).confirmarCambioCorreo("tok123");
    }
}
