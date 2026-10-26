package com.reservar.controller;

import com.reservar.dto.auth.LoginRequest;
import com.reservar.dto.auth.LoginResponse;
import com.reservar.dto.auth.RegistroClienteRequest;
import com.reservar.dto.auth.RegistroProveedorRequest;
import com.reservar.dto.auth.RegistroResponse;
import com.reservar.exception.BusinessException;
import com.reservar.exception.GlobalExceptionHandler;
import com.reservar.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final UUID ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void deberiaResponder201ConElRegistroCuandoSeRegistraUnCliente() throws Exception {
        // Arrange
        when(authService.registrarCliente(any(RegistroClienteRequest.class)))
                .thenReturn(new RegistroResponse(ID, "Usuario registrado correctamente. Debes verificar tu correo.",
                        "PENDIENTE"));
        String body = """
                {"nombreCompleto":"Ana Pérez","correo":"ana@correo.com","contrasena":"Clave123!"}
                """;

        // Act + Assert
        mockMvc.perform(post("/api/auth/registro/cliente").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").value(ID.toString()))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void deberiaResponder400ConErroresDeCampoCuandoElRegistroDeClienteTieneDatosInvalidos() throws Exception {
        // Arrange
        String body = """
                {"nombreCompleto":"","correo":"no-es-correo","contrasena":"Clave123!"}
                """;

        // Act + Assert
        mockMvc.perform(post("/api/auth/registro/cliente").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.errores.length()").value(2));
        verifyNoInteractions(authService);
    }

    @Test
    void deberiaResponder400ConCodigoDeNegocioCuandoElCorreoYaEstaRegistrado() throws Exception {
        // Arrange
        when(authService.registrarCliente(any(RegistroClienteRequest.class)))
                .thenThrow(new BusinessException("CORREO_EXISTENTE", "Este correo ya está registrado"));
        String body = """
                {"nombreCompleto":"Ana","correo":"ana@correo.com","contrasena":"Clave123!"}
                """;

        // Act + Assert
        mockMvc.perform(post("/api/auth/registro/cliente").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CORREO_EXISTENTE"))
                .andExpect(jsonPath("$.mensaje").value("Este correo ya está registrado"));
    }

    @Test
    void deberiaResponder201CuandoSeRegistraUnProveedor() throws Exception {
        // Arrange
        when(authService.registrarProveedor(any(RegistroProveedorRequest.class)))
                .thenReturn(new RegistroResponse(ID, "Proveedor registrado correctamente. Debes verificar tu correo.",
                        "PENDIENTE"));
        String body = """
                {"nombreCompleto":"Luis","correo":"luis@correo.com","contrasena":"Clave123!",
                 "codigoProveedor":"PROV-001"}
                """;

        // Act + Assert
        mockMvc.perform(post("/api/auth/registro/proveedor").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Proveedor registrado correctamente. Debes verificar tu correo."));
    }

    @Test
    void deberiaResponder400CuandoElProveedorNoEnviaCodigo() throws Exception {
        // Arrange
        String body = """
                {"nombreCompleto":"Luis","correo":"luis@correo.com","contrasena":"Clave123!"}
                """;

        // Act + Assert
        mockMvc.perform(post("/api/auth/registro/proveedor").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.length()").value(1));
        verifyNoInteractions(authService);
    }

    @Test
    void deberiaResponder200CuandoElTokenDeVerificacionEsValido() throws Exception {
        // Arrange (sin stubs: verificarCorreo es void)

        // Act + Assert
        mockMvc.perform(get("/api/auth/verificar/abc123"))
                .andExpect(status().isOk())
                .andExpect(content().string("Correo verificado correctamente. Ya puedes iniciar sesión."));
        verify(authService).verificarCorreo("abc123");
    }

    @Test
    void deberiaResponder400ConTokenExpiradoCuandoElEnlaceDeVerificacionExpiro() throws Exception {
        // Arrange
        doThrow(new BusinessException("TOKEN_EXPIRADO", "El enlace de verificación ha expirado"))
                .when(authService).verificarCorreo("viejo");

        // Act + Assert
        mockMvc.perform(get("/api/auth/verificar/viejo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TOKEN_EXPIRADO"));
    }

    @Test
    void deberiaUsarLaPrimeraIpDeXForwardedForCuandoLaCabeceraExiste() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class), eq("203.0.113.5"), eq("Navegador/1.0")))
                .thenReturn(new LoginResponse(ID, "Ana", "ana@correo.com", "CLIENTE", "ACTIVO", "tok",
                        "Inicio de sesión exitoso"));
        String body = """
                {"correo":"ana@correo.com","contrasena":"Clave123!"}
                """;

        // Act + Assert
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("X-Forwarded-For", " 203.0.113.5 , 10.0.0.1")
                        .header("User-Agent", "Navegador/1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("tok"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    void deberiaUsarLaIpRemotaCuandoNoHayCabeceraXForwardedFor() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class), eq("192.168.1.20"), eq(null)))
                .thenReturn(new LoginResponse(ID, "Ana", "ana@correo.com", "CLIENTE", "ACTIVO", "tok",
                        "Inicio de sesión exitoso"));
        String body = """
                {"correo":"ana@correo.com","contrasena":"Clave123!"}
                """;

        // Act + Assert
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(req -> {
                            req.setRemoteAddr("192.168.1.20");
                            return req;
                        }))
                .andExpect(status().isOk());
    }

    @Test
    void deberiaUsarLaIpRemotaCuandoXForwardedForEstaEnBlanco() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class), eq("192.168.1.20"), eq(null)))
                .thenReturn(new LoginResponse(ID, "Ana", "ana@correo.com", "CLIENTE", "ACTIVO", "tok",
                        "Inicio de sesión exitoso"));
        String body = """
                {"correo":"ana@correo.com","contrasena":"Clave123!"}
                """;

        // Act + Assert
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("X-Forwarded-For", "   ")
                        .with(req -> {
                            req.setRemoteAddr("192.168.1.20");
                            return req;
                        }))
                .andExpect(status().isOk());
    }

    @Test
    void deberiaResponder400SinLlamarAlServicioCuandoElLoginTieneCamposVacios() throws Exception {
        // Arrange (HU-02 escenario 5: también se valida en el servidor)
        String body = """
                {"correo":"","contrasena":""}
                """;

        // Act + Assert
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        verifyNoInteractions(authService);
    }

    @Test
    void deberiaResponderMensajeGenericoCuandoSeSolicitaElReenvioDeVerificacion() throws Exception {
        // Arrange (HU-04 escenario 4: la respuesta no revela si la cuenta existe)
        String body = """
                {"correo":"ana@correo.com"}
                """;

        // Act + Assert
        mockMvc.perform(post("/api/auth/verificar/reenviar").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Si la cuenta existe y tiene verificación pendiente, "
                                + "se enviarán las instrucciones correspondientes."));
        verify(authService).reenviarVerificacion("ana@correo.com");
    }

    @Test
    void deberiaResponder400SinLlamarAlServicioCuandoElCorreoDelReenvioEsInvalido() throws Exception {
        // Arrange
        String body = """
                {"correo":"no-es-correo"}
                """;

        // Act + Assert
        mockMvc.perform(post("/api/auth/verificar/reenviar").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }

    @Test
    void deberiaResponderMensajeGenericoCuandoSeSolicitaRecuperacion() throws Exception {
        // Arrange
        String body = """
                {"correo":"ana@correo.com"}
                """;

        // Act + Assert
        mockMvc.perform(post("/api/auth/recuperacion/solicitar").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Si el correo está registrado, se ha generado un enlace de recuperación."));
        verify(authService).solicitarRecuperacion("ana@correo.com");
    }

    @Test
    void deberiaResponder200YDelegarTokenYContrasenaCuandoSeConfirmaLaRecuperacion() throws Exception {
        // Arrange
        String body = """
                {"token":"abc","nuevaContrasena":"NuevaClave1!"}
                """;
        ArgumentCaptor<String> contrasena = ArgumentCaptor.forClass(String.class);

        // Act
        mockMvc.perform(post("/api/auth/recuperacion/confirmar").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Contraseña actualizada correctamente. Las sesiones anteriores han sido invalidadas."));

        // Assert
        verify(authService).confirmarRecuperacion(eq("abc"), contrasena.capture());
        assertAll(() -> assertEquals("NuevaClave1!", contrasena.getValue()));
    }
}
