package com.reservar.service;

import com.reservar.dto.auth.LoginRequest;
import com.reservar.dto.auth.LoginResponse;
import com.reservar.dto.auth.RegistroClienteRequest;
import com.reservar.dto.auth.RegistroProveedorRequest;
import com.reservar.dto.auth.RegistroResponse;
import com.reservar.entity.CodigoProveedor;
import com.reservar.entity.IntentoLogueo;
import com.reservar.entity.Proveedor;
import com.reservar.entity.Sesion;
import com.reservar.entity.Token;
import com.reservar.entity.Usuario;
import com.reservar.entity.enums.EstadoCodigoProveedor;
import com.reservar.entity.enums.EstadoUsuario;
import com.reservar.entity.enums.Rol;
import com.reservar.entity.enums.TipoToken;
import com.reservar.exception.BusinessException;
import com.reservar.repository.CodigoProveedorRepository;
import com.reservar.repository.IntentoLogueoRepository;
import com.reservar.repository.ProveedorRepository;
import com.reservar.repository.SesionRepository;
import com.reservar.repository.TokenRepository;
import com.reservar.repository.UsuarioRepository;
import com.reservar.security.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String BACKEND_URL = "http://backend.test";
    private static final String FRONTEND_URL = "http://frontend.test";
    private static final String CONTRASENA_VALIDA = "Clave123!";
    private static final String HASH_CONTRASENA = "$2a$hash";
    private static final UUID ID_USUARIO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String IP = "10.0.0.1";
    private static final String USER_AGENT = "JUnit";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CodigoProveedorRepository codigoProveedorRepository;
    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private IntentoLogueoRepository intentoLogueoRepository;
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private SesionRepository sesionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void configurarUrls() {
        ReflectionTestUtils.setField(authService, "backendUrl", BACKEND_URL);
        ReflectionTestUtils.setField(authService, "frontendUrl", FRONTEND_URL);
    }

    // ---------------------------------------------------------------- helpers

    private static Usuario usuario(EstadoUsuario estado, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(ID_USUARIO);
        usuario.setNombreCompleto("Ana Pérez");
        usuario.setCorreo("ana@correo.com");
        usuario.setHashContrasena(HASH_CONTRASENA);
        usuario.setRol(rol);
        usuario.setEstado(estado);
        usuario.setIntentosFallidos(0);
        return usuario;
    }

    private static Token token(Usuario usuario, TipoToken tipo, LocalDateTime expiracion) {
        Token token = new Token();
        token.setUsuario(usuario);
        token.setTipo(tipo);
        token.setFechaExpiracion(expiracion);
        return token;
    }

    private static void assertCercaDe(LocalDateTime esperado, LocalDateTime real) {
        // El servicio usa LocalDateTime.now() internamente: se admite una tolerancia de 1 minuto.
        assertNotNull(real);
        assertTrue(!real.isBefore(esperado.minusMinutes(1)) && !real.isAfter(esperado.plusMinutes(1)),
                () -> "Se esperaba una fecha cercana a " + esperado + " pero fue " + real);
    }

    private void simularAsignacionDeIdAlGuardarUsuario() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> {
            Usuario guardado = invocacion.getArgument(0);
            guardado.setIdUsuario(ID_USUARIO);
            return guardado;
        });
    }

    // ================================================================ HU-01 cliente

    @Nested
    class RegistrarCliente {

        private RegistroClienteRequest request(String contrasena) {
            RegistroClienteRequest request = new RegistroClienteRequest();
            request.setNombreCompleto("  Ana Pérez  ");
            request.setCorreo("  ana@correo.com  ");
            request.setContrasena(contrasena);
            return request;
        }

        @Test
        void deberiaRegistrarConRolClienteYEstadoPendienteCuandoLosDatosSonValidos() {
            // Arrange
            when(usuarioRepository.existsByCorreo("ana@correo.com")).thenReturn(false);
            when(passwordEncoder.encode(CONTRASENA_VALIDA)).thenReturn(HASH_CONTRASENA);
            simularAsignacionDeIdAlGuardarUsuario();
            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);

            // Act
            RegistroResponse response = authService.registrarCliente(request(CONTRASENA_VALIDA));

            // Assert
            verify(usuarioRepository).save(captor.capture());
            Usuario guardado = captor.getValue();
            assertAll(
                    () -> assertEquals("Ana Pérez", guardado.getNombreCompleto()),
                    () -> assertEquals("ana@correo.com", guardado.getCorreo()),
                    () -> assertEquals(HASH_CONTRASENA, guardado.getHashContrasena()),
                    () -> assertEquals(Rol.CLIENTE, guardado.getRol()),
                    () -> assertEquals(EstadoUsuario.PENDIENTE, guardado.getEstado()),
                    () -> assertEquals(0, guardado.getIntentosFallidos()),
                    () -> assertNotNull(guardado.getFechaCreacion()),
                    () -> assertEquals(ID_USUARIO, response.getIdUsuario()),
                    () -> assertEquals("PENDIENTE", response.getEstado()),
                    () -> assertEquals("Usuario registrado correctamente. Debes verificar tu correo.",
                            response.getMensaje())
            );
        }

        @Test
        void deberiaEnviarCorreoDeVerificacionConEnlaceAlBackendQueExpiraEn24HorasCuandoSeRegistra() {
            // Arrange (HU-04 escenario 1)
            when(usuarioRepository.existsByCorreo("ana@correo.com")).thenReturn(false);
            when(passwordEncoder.encode(CONTRASENA_VALIDA)).thenReturn(HASH_CONTRASENA);
            ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
            ArgumentCaptor<String> enlaceCaptor = ArgumentCaptor.forClass(String.class);

            // Act
            authService.registrarCliente(request(CONTRASENA_VALIDA));

            // Assert
            verify(tokenRepository).save(tokenCaptor.capture());
            verify(emailService).enviarCorreoVerificacion(eq("ana@correo.com"), enlaceCaptor.capture(), any());
            Token tokenGuardado = tokenCaptor.getValue();
            String enlace = enlaceCaptor.getValue();
            String tokenPlano = enlace.substring((BACKEND_URL + "/api/auth/verificar/").length());
            assertAll(
                    () -> assertTrue(enlace.startsWith(BACKEND_URL + "/api/auth/verificar/")),
                    () -> assertEquals(64, tokenPlano.length()),
                    () -> assertEquals(TipoToken.VERIFICACION, tokenGuardado.getTipo()),
                    () -> assertEquals(TokenHasher.hash(tokenPlano), tokenGuardado.getHashToken()),
                    () -> assertCercaDe(LocalDateTime.now().plusHours(24), tokenGuardado.getFechaExpiracion())
            );
        }

        @Test
        void deberiaLanzarCorreoExistenteCuandoElCorreoYaEstaRegistrado() {
            // Arrange
            when(usuarioRepository.existsByCorreo("ana@correo.com")).thenReturn(true);

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.registrarCliente(request(CONTRASENA_VALIDA)));

            // Assert
            assertAll(
                    () -> assertEquals("CORREO_EXISTENTE", ex.getCodigo()),
                    () -> assertEquals("Este correo ya está registrado", ex.getMessage()),
                    () -> verify(usuarioRepository, never()).save(any()),
                    () -> verifyNoInteractions(tokenRepository, emailService)
            );
        }

        @Test
        void deberiaLanzarContrasenaInvalidaSinConsultarLaBaseCuandoLaContrasenaNoCumpleRequisitos() {
            // Arrange
            RegistroClienteRequest request = request("clave");

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.registrarCliente(request));

            // Assert
            assertAll(
                    () -> assertEquals("CONTRASENA_INVALIDA", ex.getCodigo()),
                    () -> assertEquals("La contraseña debe tener mínimo 8 caracteres. "
                            + "La contraseña debe contener al menos una mayúscula. "
                            + "La contraseña debe contener al menos un número. "
                            + "La contraseña debe contener al menos un carácter especial", ex.getMessage()),
                    () -> verifyNoInteractions(usuarioRepository, passwordEncoder, tokenRepository, emailService)
            );
        }
    }

    // ================================================================ HU-01 proveedor

    @Nested
    class RegistrarProveedor {

        private RegistroProveedorRequest request(String contrasena) {
            RegistroProveedorRequest request = new RegistroProveedorRequest();
            request.setNombreCompleto(" Luis Gómez ");
            request.setCorreo(" luis@correo.com ");
            request.setContrasena(contrasena);
            request.setCodigoProveedor("  PROV-001 ");
            request.setDescripcion("Reparaciones");
            return request;
        }

        private CodigoProveedor codigo(LocalDateTime expiracion) {
            CodigoProveedor codigo = new CodigoProveedor();
            codigo.setCodigo("PROV-001");
            codigo.setEstado(EstadoCodigoProveedor.DISPONIBLE);
            codigo.setFechaExpiracion(expiracion);
            return codigo;
        }

        @Test
        void deberiaRegistrarConRolProveedorYMarcarCodigoComoUsadoCuandoElCodigoEsValido() {
            // Arrange
            CodigoProveedor codigo = codigo(LocalDateTime.now().plusDays(10));
            when(usuarioRepository.existsByCorreo("luis@correo.com")).thenReturn(false);
            when(codigoProveedorRepository.findByCodigoAndEstado("PROV-001", EstadoCodigoProveedor.DISPONIBLE))
                    .thenReturn(Optional.of(codigo));
            when(passwordEncoder.encode(CONTRASENA_VALIDA)).thenReturn(HASH_CONTRASENA);
            simularAsignacionDeIdAlGuardarUsuario();
            ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
            ArgumentCaptor<Proveedor> proveedorCaptor = ArgumentCaptor.forClass(Proveedor.class);

            // Act
            RegistroResponse response = authService.registrarProveedor(request(CONTRASENA_VALIDA));

            // Assert
            verify(usuarioRepository).save(usuarioCaptor.capture());
            verify(proveedorRepository).save(proveedorCaptor.capture());
            Usuario usuario = usuarioCaptor.getValue();
            Proveedor proveedor = proveedorCaptor.getValue();
            assertAll(
                    () -> assertEquals(Rol.PROVEEDOR, usuario.getRol()),
                    () -> assertEquals(EstadoUsuario.PENDIENTE, usuario.getEstado()),
                    () -> assertEquals("Luis Gómez", usuario.getNombreCompleto()),
                    () -> assertEquals("luis@correo.com", usuario.getCorreo()),
                    () -> assertEquals(usuario, proveedor.getUsuario()),
                    () -> assertEquals(codigo, proveedor.getCodigoProveedor()),
                    () -> assertEquals("Reparaciones", proveedor.getDescripcion()),
                    () -> assertEquals(EstadoCodigoProveedor.USADO, codigo.getEstado()),
                    () -> assertNotNull(codigo.getFechaUso()),
                    () -> verify(codigoProveedorRepository).save(codigo),
                    () -> verify(emailService).enviarCorreoVerificacion(eq("luis@correo.com"), anyString(), any()),
                    () -> assertEquals(ID_USUARIO, response.getIdUsuario()),
                    () -> assertEquals("PENDIENTE", response.getEstado()),
                    () -> assertEquals("Proveedor registrado correctamente. Debes verificar tu correo.",
                            response.getMensaje())
            );
        }

        @Test
        void deberiaRegistrarProveedorCuandoElCodigoNoTieneFechaDeExpiracion() {
            // Arrange
            CodigoProveedor codigo = codigo(null);
            when(usuarioRepository.existsByCorreo("luis@correo.com")).thenReturn(false);
            when(codigoProveedorRepository.findByCodigoAndEstado("PROV-001", EstadoCodigoProveedor.DISPONIBLE))
                    .thenReturn(Optional.of(codigo));

            // Act
            RegistroResponse response = authService.registrarProveedor(request(CONTRASENA_VALIDA));

            // Assert
            assertAll(
                    () -> assertEquals("PENDIENTE", response.getEstado()),
                    () -> assertEquals(EstadoCodigoProveedor.USADO, codigo.getEstado())
            );
        }

        @Test
        void deberiaLanzarCorreoExistenteSinConsultarCodigoCuandoElCorreoYaEstaRegistrado() {
            // Arrange
            when(usuarioRepository.existsByCorreo("luis@correo.com")).thenReturn(true);

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.registrarProveedor(request(CONTRASENA_VALIDA)));

            // Assert
            assertAll(
                    () -> assertEquals("CORREO_EXISTENTE", ex.getCodigo()),
                    () -> verifyNoInteractions(codigoProveedorRepository, proveedorRepository)
            );
        }

        @Test
        void deberiaLanzarCodigoProveedorInvalidoCuandoElCodigoNoExisteONoEstaDisponible() {
            // Arrange
            when(usuarioRepository.existsByCorreo("luis@correo.com")).thenReturn(false);
            when(codigoProveedorRepository.findByCodigoAndEstado("PROV-001", EstadoCodigoProveedor.DISPONIBLE))
                    .thenReturn(Optional.empty());

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.registrarProveedor(request(CONTRASENA_VALIDA)));

            // Assert
            assertAll(
                    () -> assertEquals("CODIGO_PROVEEDOR_INVALIDO", ex.getCodigo()),
                    () -> assertEquals("El código de proveedor no es válido o no está disponible", ex.getMessage()),
                    () -> verify(usuarioRepository, never()).save(any()),
                    () -> verifyNoInteractions(proveedorRepository)
            );
        }

        @Test
        void deberiaMarcarCodigoComoExpiradoYLanzarErrorCuandoElCodigoEstaVencido() {
            // Arrange
            CodigoProveedor codigo = codigo(LocalDateTime.now().minusDays(1));
            when(usuarioRepository.existsByCorreo("luis@correo.com")).thenReturn(false);
            when(codigoProveedorRepository.findByCodigoAndEstado("PROV-001", EstadoCodigoProveedor.DISPONIBLE))
                    .thenReturn(Optional.of(codigo));

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.registrarProveedor(request(CONTRASENA_VALIDA)));

            // Assert
            assertAll(
                    () -> assertEquals("CODIGO_PROVEEDOR_EXPIRADO", ex.getCodigo()),
                    () -> assertEquals(EstadoCodigoProveedor.EXPIRADO, codigo.getEstado()),
                    () -> verify(codigoProveedorRepository).save(codigo),
                    () -> verify(usuarioRepository, never()).save(any()),
                    () -> verifyNoInteractions(proveedorRepository, emailService)
            );
        }

        @Test
        void deberiaLanzarContrasenaInvalidaCuandoLaContrasenaNoTieneCaracterEspecial() {
            // Arrange
            RegistroProveedorRequest request = request("Clave1234");

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.registrarProveedor(request));

            // Assert
            assertAll(
                    () -> assertEquals("CONTRASENA_INVALIDA", ex.getCodigo()),
                    () -> assertEquals("La contraseña debe contener al menos un carácter especial", ex.getMessage()),
                    () -> verifyNoInteractions(usuarioRepository, codigoProveedorRepository)
            );
        }
    }

    // ================================================================ HU-02 login

    @Nested
    class Login {

        private LoginRequest request(String contrasena) {
            LoginRequest request = new LoginRequest();
            request.setCorreo("  ana@correo.com ");
            request.setContrasena(contrasena);
            return request;
        }

        @Test
        void deberiaIniciarSesionYRetornarTokenCuandoLasCredencialesSonCorrectas() {
            // Arrange (escenario 1)
            Usuario usuario = usuario(EstadoUsuario.ACTIVO, Rol.PROVEEDOR);
            usuario.setIntentosFallidos(2);
            when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(CONTRASENA_VALIDA, HASH_CONTRASENA)).thenReturn(true);
            ArgumentCaptor<Sesion> sesionCaptor = ArgumentCaptor.forClass(Sesion.class);

            // Act
            LoginResponse response = authService.login(request(CONTRASENA_VALIDA), IP, USER_AGENT);

            // Assert
            verify(sesionRepository).save(sesionCaptor.capture());
            Sesion sesion = sesionCaptor.getValue();
            assertAll(
                    () -> assertEquals(ID_USUARIO, response.getIdUsuario()),
                    () -> assertEquals("Ana Pérez", response.getNombreCompleto()),
                    () -> assertEquals("ana@correo.com", response.getCorreo()),
                    () -> assertEquals("PROVEEDOR", response.getRol()),
                    () -> assertEquals("ACTIVO", response.getEstado()),
                    () -> assertEquals("Inicio de sesión exitoso", response.getMensaje()),
                    () -> assertEquals(64, response.getToken().length()),
                    () -> assertEquals(TokenHasher.hash(response.getToken()), sesion.getHashToken()),
                    () -> assertEquals(usuario, sesion.getUsuario()),
                    () -> assertEquals(IP, sesion.getIpOrigen()),
                    () -> assertEquals(USER_AGENT, sesion.getUserAgent()),
                    () -> assertEquals(sesion.getFechaCreacion().plusHours(24), sesion.getFechaExpiracion()),
                    () -> assertEquals(0, usuario.getIntentosFallidos()),
                    () -> assertNull(usuario.getBloqueadoHasta())
            );
        }

        @Test
        void deberiaRegistrarIntentoExitosoEnElHistorialCuandoElLoginEsCorrecto() {
            // Arrange (escenario 1: historial)
            Usuario usuario = usuario(EstadoUsuario.ACTIVO, Rol.CLIENTE);
            when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(CONTRASENA_VALIDA, HASH_CONTRASENA)).thenReturn(true);
            ArgumentCaptor<IntentoLogueo> captor = ArgumentCaptor.forClass(IntentoLogueo.class);

            // Act
            authService.login(request(CONTRASENA_VALIDA), IP, USER_AGENT);

            // Assert
            verify(intentoLogueoRepository).save(captor.capture());
            IntentoLogueo intento = captor.getValue();
            assertAll(
                    () -> assertTrue(intento.getExitoso()),
                    () -> assertEquals(usuario, intento.getUsuario()),
                    () -> assertEquals(IP, intento.getIpOrigen()),
                    () -> assertEquals(USER_AGENT, intento.getUserAgent())
            );
        }

        @Test
        void deberiaLanzarCredencialesInvalidasYSumarIntentoCuandoLaContrasenaEsIncorrecta() {
            // Arrange (escenario 2)
            Usuario usuario = usuario(EstadoUsuario.ACTIVO, Rol.CLIENTE);
            when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("Incorrecta1!", HASH_CONTRASENA)).thenReturn(false);
            ArgumentCaptor<IntentoLogueo> captor = ArgumentCaptor.forClass(IntentoLogueo.class);

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.login(request("Incorrecta1!"), IP, USER_AGENT));

            // Assert
            verify(intentoLogueoRepository).save(captor.capture());
            assertAll(
                    () -> assertEquals("CREDENCIALES_INVALIDAS", ex.getCodigo()),
                    () -> assertEquals("Correo o contraseña incorrectos", ex.getMessage()),
                    () -> assertEquals(1, usuario.getIntentosFallidos()),
                    () -> assertNull(usuario.getBloqueadoHasta()),
                    () -> assertFalse(captor.getValue().getExitoso()),
                    () -> assertEquals(usuario, captor.getValue().getUsuario()),
                    () -> verifyNoInteractions(sesionRepository)
            );
        }

        @Test
        void deberiaLanzarElMismoMensajeGenericoYRegistrarIntentoSinUsuarioCuandoElCorreoNoExiste() {
            // Arrange (escenario 3: no revela si el correo existe)
            when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.empty());
            ArgumentCaptor<IntentoLogueo> captor = ArgumentCaptor.forClass(IntentoLogueo.class);

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.login(request(CONTRASENA_VALIDA), IP, USER_AGENT));

            // Assert
            verify(intentoLogueoRepository).save(captor.capture());
            assertAll(
                    () -> assertEquals("CREDENCIALES_INVALIDAS", ex.getCodigo()),
                    () -> assertEquals("Correo o contraseña incorrectos", ex.getMessage()),
                    () -> assertNull(captor.getValue().getUsuario()),
                    () -> assertFalse(captor.getValue().getExitoso()),
                    () -> verifyNoInteractions(passwordEncoder, sesionRepository)
            );
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "INACTIVO  | CUENTA_INACTIVA      | Esta cuenta está inactiva. Contacte al administrador",
                "PENDIENTE | CUENTA_NO_VERIFICADA | Debes verificar tu correo antes de iniciar sesión"
        })
        void deberiaRechazarConMensajeDiferenciadoCuandoLaCuentaNoEstaActiva(
                EstadoUsuario estado, String codigoEsperado, String mensajeEsperado) {
            // Arrange (escenario 4 / HU-04 escenario 4)
            Usuario usuario = usuario(estado, Rol.CLIENTE);
            when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.login(request(CONTRASENA_VALIDA), IP, USER_AGENT));

            // Assert
            assertAll(
                    () -> assertEquals(codigoEsperado, ex.getCodigo()),
                    () -> assertEquals(mensajeEsperado, ex.getMessage()),
                    () -> verify(intentoLogueoRepository).save(any(IntentoLogueo.class)),
                    () -> verifyNoInteractions(passwordEncoder, sesionRepository)
            );
        }

        @Test
        void deberiaBloquearLaCuenta15MinutosCuandoOcurreElQuintoIntentoFallidoConsecutivo() {
            // Arrange (escenario 6)
            Usuario usuario = usuario(EstadoUsuario.ACTIVO, Rol.CLIENTE);
            usuario.setIntentosFallidos(4);
            when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("Incorrecta1!", HASH_CONTRASENA)).thenReturn(false);

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.login(request("Incorrecta1!"), IP, USER_AGENT));

            // Assert
            assertAll(
                    () -> assertEquals("CREDENCIALES_INVALIDAS", ex.getCodigo()),
                    () -> assertCercaDe(LocalDateTime.now().plusMinutes(15), usuario.getBloqueadoHasta()),
                    () -> assertEquals(0, usuario.getIntentosFallidos()),
                    () -> verify(usuarioRepository).save(usuario)
            );
        }

        @Test
        void deberiaNoBloquearLaCuentaCuandoOcurreElCuartoIntentoFallido() {
            // Arrange (borde: un intento antes del bloqueo)
            Usuario usuario = usuario(EstadoUsuario.ACTIVO, Rol.CLIENTE);
            usuario.setIntentosFallidos(3);
            when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("Incorrecta1!", HASH_CONTRASENA)).thenReturn(false);

            // Act
            assertThrows(BusinessException.class,
                    () -> authService.login(request("Incorrecta1!"), IP, USER_AGENT));

            // Assert
            assertAll(
                    () -> assertEquals(4, usuario.getIntentosFallidos()),
                    () -> assertNull(usuario.getBloqueadoHasta())
            );
        }

        @Test
        void deberiaRechazarSinValidarContrasenaCuandoLaCuentaEstaBloqueada() {
            // Arrange
            Usuario usuario = usuario(EstadoUsuario.ACTIVO, Rol.CLIENTE);
            LocalDateTime bloqueo = LocalDateTime.now().plusMinutes(10);
            usuario.setBloqueadoHasta(bloqueo);
            when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.login(request(CONTRASENA_VALIDA), IP, USER_AGENT));

            // Assert
            assertAll(
                    () -> assertEquals("CUENTA_BLOQUEADA", ex.getCodigo()),
                    () -> assertEquals("La cuenta está temporalmente bloqueada. Intenta nuevamente más tarde",
                            ex.getMessage()),
                    () -> assertEquals(bloqueo, usuario.getBloqueadoHasta()),
                    () -> verify(intentoLogueoRepository).save(any(IntentoLogueo.class)),
                    () -> verifyNoInteractions(passwordEncoder, sesionRepository)
            );
        }

        @Test
        void deberiaDesbloquearYPermitirElLoginCuandoElBloqueoYaVencio() {
            // Arrange
            Usuario usuario = usuario(EstadoUsuario.ACTIVO, Rol.CLIENTE);
            usuario.setBloqueadoHasta(LocalDateTime.now().minusMinutes(1));
            when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(CONTRASENA_VALIDA, HASH_CONTRASENA)).thenReturn(true);

            // Act
            LoginResponse response = authService.login(request(CONTRASENA_VALIDA), IP, USER_AGENT);

            // Assert
            assertAll(
                    () -> assertEquals("Inicio de sesión exitoso", response.getMensaje()),
                    () -> assertNull(usuario.getBloqueadoHasta()),
                    () -> assertEquals(0, usuario.getIntentosFallidos())
            );
        }
    }

    // ================================================================ HU-04 verificación

    @Nested
    class VerificarCorreo {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void deberiaLanzarTokenInvalidoCuandoElTokenEsNuloOVacio(String tokenPlano) {
            // Arrange (valor parametrizado)

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.verificarCorreo(tokenPlano));

            // Assert
            assertAll(
                    () -> assertEquals("TOKEN_INVALIDO", ex.getCodigo()),
                    () -> verifyNoInteractions(tokenRepository)
            );
        }

        @Test
        void deberiaLanzarTokenInvalidoCuandoElTokenNoExisteOYaFueUsado() {
            // Arrange
            when(tokenRepository.findByHashTokenAndTipoAndFechaUsoIsNull(
                    TokenHasher.hash("desconocido"), TipoToken.VERIFICACION)).thenReturn(Optional.empty());

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.verificarCorreo("desconocido"));

            // Assert
            assertAll(
                    () -> assertEquals("TOKEN_INVALIDO", ex.getCodigo()),
                    () -> assertEquals("El token de verificación no es válido", ex.getMessage())
            );
        }

        @Test
        void deberiaLanzarTokenExpiradoYMantenerCuentaPendienteCuandoElEnlaceExpiro() {
            // Arrange (escenario 3)
            Usuario usuario = usuario(EstadoUsuario.PENDIENTE, Rol.CLIENTE);
            Token token = token(usuario, TipoToken.VERIFICACION, LocalDateTime.now().minusMinutes(1));
            when(tokenRepository.findByHashTokenAndTipoAndFechaUsoIsNull(
                    TokenHasher.hash("abc"), TipoToken.VERIFICACION)).thenReturn(Optional.of(token));

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.verificarCorreo("abc"));

            // Assert
            assertAll(
                    () -> assertEquals("TOKEN_EXPIRADO", ex.getCodigo()),
                    () -> assertEquals("El enlace de verificación ha expirado", ex.getMessage()),
                    () -> assertEquals(EstadoUsuario.PENDIENTE, usuario.getEstado()),
                    () -> assertNull(token.getFechaUso()),
                    () -> verifyNoInteractions(usuarioRepository)
            );
        }

        @Test
        void deberiaLanzarCuentaYaVerificadaCuandoElUsuarioYaEstaActivo() {
            // Arrange
            Usuario usuario = usuario(EstadoUsuario.ACTIVO, Rol.CLIENTE);
            Token token = token(usuario, TipoToken.VERIFICACION, LocalDateTime.now().plusHours(1));
            when(tokenRepository.findByHashTokenAndTipoAndFechaUsoIsNull(
                    TokenHasher.hash("abc"), TipoToken.VERIFICACION)).thenReturn(Optional.of(token));

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.verificarCorreo("abc"));

            // Assert
            assertEquals("CUENTA_YA_VERIFICADA", ex.getCodigo());
        }

        @Test
        void deberiaActivarLaCuentaYMarcarElTokenComoUsadoCuandoElEnlaceEsValido() {
            // Arrange (escenario 2)
            Usuario usuario = usuario(EstadoUsuario.PENDIENTE, Rol.CLIENTE);
            Token token = token(usuario, TipoToken.VERIFICACION, LocalDateTime.now().plusHours(23));
            when(tokenRepository.findByHashTokenAndTipoAndFechaUsoIsNull(
                    TokenHasher.hash("abc"), TipoToken.VERIFICACION)).thenReturn(Optional.of(token));

            // Act
            authService.verificarCorreo("abc");

            // Assert
            assertAll(
                    () -> assertEquals(EstadoUsuario.ACTIVO, usuario.getEstado()),
                    () -> assertNotNull(token.getFechaUso()),
                    () -> verify(usuarioRepository).save(usuario),
                    () -> verify(tokenRepository).save(token)
            );
        }
    }

    // ================================================================ HU-05 recuperación

    @Nested
    class ReenviarVerificacion {

        @Test
        void deberiaInvalidarTokensAnterioresYEnviarNuevoEnlaceQueExpiraEn24HorasCuandoLaCuentaEstaPendiente() {
            // Arrange (HU-04 escenario 4)
            Usuario usuario = usuario(EstadoUsuario.PENDIENTE, Rol.CLIENTE);
            Token anterior = token(usuario, TipoToken.VERIFICACION, LocalDateTime.now().minusHours(1));
            when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
            when(tokenRepository.findByUsuarioIdUsuarioAndTipoAndFechaUsoIsNull(ID_USUARIO, TipoToken.VERIFICACION))
                    .thenReturn(List.of(anterior));
            ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
            ArgumentCaptor<String> enlaceCaptor = ArgumentCaptor.forClass(String.class);

            // Act
            authService.reenviarVerificacion("  ana@correo.com  ");

            // Assert
            verify(tokenRepository).saveAll(List.of(anterior));
            verify(tokenRepository).save(tokenCaptor.capture());
            verify(emailService).enviarCorreoVerificacion(eq("ana@correo.com"), enlaceCaptor.capture(), any());
            Token nuevo = tokenCaptor.getValue();
            String prefijo = BACKEND_URL + "/api/auth/verificar/";
            String tokenPlano = enlaceCaptor.getValue().substring(prefijo.length());
            assertAll(
                    () -> assertNotNull(anterior.getFechaUso()),
                    () -> assertTrue(enlaceCaptor.getValue().startsWith(prefijo)),
                    () -> assertEquals(TipoToken.VERIFICACION, nuevo.getTipo()),
                    () -> assertEquals(TokenHasher.hash(tokenPlano), nuevo.getHashToken()),
                    () -> assertCercaDe(LocalDateTime.now().plusHours(24), nuevo.getFechaExpiracion()),
                    () -> assertEquals(EstadoUsuario.PENDIENTE, usuario.getEstado())
            );
        }

        @Test
        void deberiaTerminarSinErrorNiEnviarCorreoCuandoElCorreoNoExiste() {
            // Arrange (no revela si el correo está registrado)
            when(usuarioRepository.findByCorreo("nadie@correo.com")).thenReturn(Optional.empty());

            // Act
            authService.reenviarVerificacion("nadie@correo.com");

            // Assert
            verifyNoInteractions(tokenRepository, emailService);
        }

        @ParameterizedTest
        @ValueSource(strings = {"ACTIVO", "INACTIVO"})
        void deberiaTerminarSinErrorNiEnviarCorreoCuandoLaCuentaNoEstaPendiente(String estado) {
            // Arrange (valor parametrizado)
            Usuario usuario = usuario(EstadoUsuario.valueOf(estado), Rol.CLIENTE);
            when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));

            // Act
            authService.reenviarVerificacion("ana@correo.com");

            // Assert
            verifyNoInteractions(tokenRepository, emailService);
        }
    }

    @Nested
    class SolicitarRecuperacion {

        @Test
        void deberiaInvalidarLosTokensDeRecuperacionAnterioresAntesDeCrearUnoNuevo() {
            // Arrange (solo el último enlace de recuperación queda vigente)
            Usuario usuario = usuario(EstadoUsuario.ACTIVO, Rol.CLIENTE);
            Token anterior = token(usuario, TipoToken.RECUPERACION, LocalDateTime.now().plusMinutes(30));
            when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
            when(tokenRepository.findByUsuarioIdUsuarioAndTipoAndFechaUsoIsNull(ID_USUARIO, TipoToken.RECUPERACION))
                    .thenReturn(List.of(anterior));

            // Act
            authService.solicitarRecuperacion("ana@correo.com");

            // Assert
            assertAll(
                    () -> assertNotNull(anterior.getFechaUso()),
                    () -> verify(tokenRepository).saveAll(List.of(anterior)),
                    () -> verify(tokenRepository).save(any(Token.class))
            );
        }

        @Test
        void deberiaEnviarEnlaceAlFrontendConTokenQueExpiraEn1HoraCuandoElCorreoExiste() {
            // Arrange (escenario 1)
            Usuario usuario = usuario(EstadoUsuario.ACTIVO, Rol.CLIENTE);
            when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
            ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
            ArgumentCaptor<String> enlaceCaptor = ArgumentCaptor.forClass(String.class);

            // Act
            authService.solicitarRecuperacion("  ana@correo.com  ");

            // Assert
            verify(tokenRepository).save(tokenCaptor.capture());
            verify(emailService).enviarCorreoRecuperacion(eq("ana@correo.com"), enlaceCaptor.capture(), any());
            Token token = tokenCaptor.getValue();
            String prefijo = FRONTEND_URL + "/recuperar-contrasena?token=";
            String tokenPlano = enlaceCaptor.getValue().substring(prefijo.length());
            assertAll(
                    () -> assertTrue(enlaceCaptor.getValue().startsWith(prefijo)),
                    () -> assertEquals(TipoToken.RECUPERACION, token.getTipo()),
                    () -> assertEquals(usuario, token.getUsuario()),
                    () -> assertEquals(TokenHasher.hash(tokenPlano), token.getHashToken()),
                    () -> assertCercaDe(LocalDateTime.now().plusHours(1), token.getFechaExpiracion())
            );
        }

        @Test
        void deberiaTerminarSinErrorNiEnviarCorreoCuandoElCorreoNoExiste() {
            // Arrange (escenario 1: no revela si el correo existe)
            when(usuarioRepository.findByCorreo("nadie@correo.com")).thenReturn(Optional.empty());

            // Act
            authService.solicitarRecuperacion("nadie@correo.com");

            // Assert
            verifyNoInteractions(tokenRepository, emailService);
        }
    }

    @Nested
    class ConfirmarRecuperacion {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        void deberiaLanzarTokenInvalidoCuandoElTokenEsNuloOVacio(String tokenPlano) {
            // Arrange (valor parametrizado)

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.confirmarRecuperacion(tokenPlano, CONTRASENA_VALIDA));

            // Assert
            assertAll(
                    () -> assertEquals("TOKEN_INVALIDO", ex.getCodigo()),
                    () -> assertEquals("El token de recuperación no es válido", ex.getMessage())
            );
        }

        @Test
        void deberiaLanzarContrasenaInvalidaSinConsultarTokenCuandoLaNuevaContrasenaNoCumpleRequisitos() {
            // Arrange (escenario 2: mismos requisitos de HU-01)
            String contrasenaDebil = "clave123!";

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.confirmarRecuperacion("abc", contrasenaDebil));

            // Assert
            assertAll(
                    () -> assertEquals("CONTRASENA_INVALIDA", ex.getCodigo()),
                    () -> verifyNoInteractions(tokenRepository, usuarioRepository)
            );
        }

        @Test
        void deberiaLanzarTokenInvalidoCuandoElTokenNoExisteOYaFueUsado() {
            // Arrange
            when(tokenRepository.findByHashTokenAndTipoAndFechaUsoIsNull(
                    TokenHasher.hash("abc"), TipoToken.RECUPERACION)).thenReturn(Optional.empty());

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.confirmarRecuperacion("abc", CONTRASENA_VALIDA));

            // Assert
            assertEquals("TOKEN_INVALIDO", ex.getCodigo());
        }

        @Test
        void deberiaLanzarTokenExpiradoSinCambiarContrasenaCuandoElEnlaceExpiro() {
            // Arrange
            Usuario usuario = usuario(EstadoUsuario.ACTIVO, Rol.CLIENTE);
            Token token = token(usuario, TipoToken.RECUPERACION, LocalDateTime.now().minusSeconds(30));
            when(tokenRepository.findByHashTokenAndTipoAndFechaUsoIsNull(
                    TokenHasher.hash("abc"), TipoToken.RECUPERACION)).thenReturn(Optional.of(token));

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.confirmarRecuperacion("abc", CONTRASENA_VALIDA));

            // Assert
            assertAll(
                    () -> assertEquals("TOKEN_EXPIRADO", ex.getCodigo()),
                    () -> assertEquals("El enlace de recuperación ha expirado", ex.getMessage()),
                    () -> assertEquals(HASH_CONTRASENA, usuario.getHashContrasena()),
                    () -> verifyNoInteractions(passwordEncoder, sesionRepository)
            );
        }

        @Test
        void deberiaActualizarContrasenaEInvalidarSesionesActivasCuandoElEnlaceEsVigente() {
            // Arrange (escenario 2)
            Usuario usuario = usuario(EstadoUsuario.ACTIVO, Rol.CLIENTE);
            Token token = token(usuario, TipoToken.RECUPERACION, LocalDateTime.now().plusMinutes(30));
            Sesion sesion1 = new Sesion();
            Sesion sesion2 = new Sesion();
            List<Sesion> sesiones = List.of(sesion1, sesion2);
            when(tokenRepository.findByHashTokenAndTipoAndFechaUsoIsNull(
                    TokenHasher.hash("abc"), TipoToken.RECUPERACION)).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("NuevaClave1!")).thenReturn("$2a$nuevo");
            when(sesionRepository.findByUsuarioIdUsuarioAndFechaRevocacionIsNull(ID_USUARIO)).thenReturn(sesiones);

            // Act
            authService.confirmarRecuperacion("abc", "NuevaClave1!");

            // Assert
            assertAll(
                    () -> assertEquals("$2a$nuevo", usuario.getHashContrasena()),
                    () -> assertNotNull(token.getFechaUso()),
                    () -> assertNotNull(sesion1.getFechaRevocacion()),
                    () -> assertNotNull(sesion2.getFechaRevocacion()),
                    () -> verify(usuarioRepository).save(usuario),
                    () -> verify(tokenRepository).save(token),
                    () -> verify(sesionRepository).saveAll(sesiones)
            );
        }
    }
}
