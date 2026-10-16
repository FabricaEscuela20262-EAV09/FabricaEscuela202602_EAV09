package com.reservar.service;

import com.reservar.dto.usuario.ActualizarPerfilRequest;
import com.reservar.dto.usuario.PerfilResponse;
import com.reservar.entity.Proveedor;
import com.reservar.entity.Token;
import com.reservar.entity.Usuario;
import com.reservar.entity.enums.EstadoUsuario;
import com.reservar.entity.enums.Rol;
import com.reservar.entity.enums.TipoToken;
import com.reservar.exception.BusinessException;
import com.reservar.repository.ProveedorRepository;
import com.reservar.repository.TokenRepository;
import com.reservar.repository.UsuarioRepository;
import com.reservar.security.TokenHasher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
class UsuarioServiceTest {

    private static final String BACKEND_URL = "http://backend.test";
    private static final UUID ID_USUARIO = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private UsuarioService usuarioService;

    @BeforeEach
    void configurarUrl() {
        ReflectionTestUtils.setField(usuarioService, "backendUrl", BACKEND_URL);
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------- helpers

    private static Usuario usuario(Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(ID_USUARIO);
        usuario.setNombreCompleto("Ana Pérez");
        usuario.setCorreo("ana@correo.com");
        usuario.setTelefono("3001234567");
        usuario.setRol(rol);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        return usuario;
    }

    private static void autenticar(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private static Proveedor proveedor() {
        Proveedor proveedor = new Proveedor();
        proveedor.setDescripcion("Reparaciones");
        return proveedor;
    }

    private static ActualizarPerfilRequest request(String nombre, String telefono, String correo) {
        ActualizarPerfilRequest request = new ActualizarPerfilRequest();
        request.setNombreCompleto(nombre);
        request.setTelefono(telefono);
        request.setCorreo(correo);
        return request;
    }

    // ================================================================ obtenerPerfil

    @Nested
    class ObtenerPerfil {

        @Test
        void deberiaRetornarDatosSinServicioCuandoElUsuarioEsCliente() {
            // Arrange
            Usuario usuario = usuario(Rol.CLIENTE);
            usuario.setCorreoPendiente("nuevo@correo.com");
            autenticar(usuario);

            // Act
            PerfilResponse perfil = usuarioService.obtenerPerfil();

            // Assert
            assertAll(
                    () -> assertEquals("Ana Pérez", perfil.getNombreCompleto()),
                    () -> assertEquals("ana@correo.com", perfil.getCorreo()),
                    () -> assertEquals("nuevo@correo.com", perfil.getCorreoPendiente()),
                    () -> assertEquals("3001234567", perfil.getTelefono()),
                    () -> assertEquals("CLIENTE", perfil.getRol()),
                    () -> assertNull(perfil.getTipoServicio()),
                    () -> assertNull(perfil.getDescripcion()),
                    () -> verifyNoInteractions(proveedorRepository)
            );
        }

        @Test
        void deberiaIncluirDescripcionCuandoElUsuarioEsProveedor() {
            // Arrange
            autenticar(usuario(Rol.PROVEEDOR));
            when(proveedorRepository.findByUsuarioIdUsuario(ID_USUARIO)).thenReturn(Optional.of(proveedor()));

            // Act
            PerfilResponse perfil = usuarioService.obtenerPerfil();

            // Assert
            assertAll(
                    () -> assertEquals("PROVEEDOR", perfil.getRol()),
                    () -> assertEquals("Reparaciones", perfil.getDescripcion())
            );
        }

        @Test
        void deberiaRetornarServicioNuloCuandoElProveedorNoTieneRegistroDeProveedor() {
            // Arrange
            autenticar(usuario(Rol.PROVEEDOR));
            when(proveedorRepository.findByUsuarioIdUsuario(ID_USUARIO)).thenReturn(Optional.empty());

            // Act
            PerfilResponse perfil = usuarioService.obtenerPerfil();

            // Assert
            assertAll(
                    () -> assertNull(perfil.getTipoServicio()),
                    () -> assertNull(perfil.getDescripcion())
            );
        }

        @Test
        void deberiaLanzarUsuarioNoAutenticadoCuandoNoHayAutenticacion() {
            // Arrange
            SecurityContextHolder.clearContext();

            // Act
            BusinessException ex = assertThrows(BusinessException.class, () -> usuarioService.obtenerPerfil());

            // Assert
            assertAll(
                    () -> assertEquals("USUARIO_NO_AUTENTICADO", ex.getCodigo()),
                    () -> assertEquals("El usuario no está autenticado", ex.getMessage())
            );
        }

        @Test
        void deberiaLanzarUsuarioNoAutenticadoCuandoElPrincipalNoEsUnUsuario() {
            // Arrange
            autenticar("anonymousUser");

            // Act
            BusinessException ex = assertThrows(BusinessException.class, () -> usuarioService.obtenerPerfil());

            // Assert
            assertEquals("USUARIO_NO_AUTENTICADO", ex.getCodigo());
        }
    }

    // ================================================================ HU-03 actualizarPerfil

    @Nested
    class ActualizarPerfil {

        @Test
        void deberiaActualizarNombreYTelefonoSinTocarElCorreoCuandoElCorreoNoCambia() {
            // Arrange (escenario 1)
            Usuario usuario = usuario(Rol.CLIENTE);
            autenticar(usuario);

            // Act
            usuarioService.actualizarPerfil(request("  Ana María  ", " 3109876543 ", "ana@correo.com"));

            // Assert
            assertAll(
                    () -> assertEquals("Ana María", usuario.getNombreCompleto()),
                    () -> assertEquals("3109876543", usuario.getTelefono()),
                    () -> assertEquals("ana@correo.com", usuario.getCorreo()),
                    () -> assertNull(usuario.getCorreoPendiente()),
                    () -> verify(usuarioRepository).save(usuario),
                    () -> verify(usuarioRepository, never()).existsByCorreo(anyString()),
                    () -> verifyNoInteractions(tokenRepository, emailService, proveedorRepository)
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void deberiaDejarTelefonoNuloCuandoSeEnviaVacioONulo(String telefono) {
            // Arrange
            Usuario usuario = usuario(Rol.CLIENTE);
            autenticar(usuario);

            // Act
            usuarioService.actualizarPerfil(request("Ana Pérez", telefono, "ana@correo.com"));

            // Assert
            assertNull(usuario.getTelefono());
        }

        @Test
        void deberiaTratarComoMismoCorreoCuandoSoloCambianMayusculas() {
            // Arrange
            Usuario usuario = usuario(Rol.CLIENTE);
            autenticar(usuario);

            // Act
            usuarioService.actualizarPerfil(request("Ana Pérez", null, "  ANA@Correo.com "));

            // Assert
            assertAll(
                    () -> assertNull(usuario.getCorreoPendiente()),
                    () -> verifyNoInteractions(tokenRepository, emailService)
            );
        }

        @Test
        void deberiaLanzarCorreoEnUsoCuandoElNuevoCorreoYaPerteneceAOtroUsuario() {
            // Arrange (escenario 3)
            Usuario usuario = usuario(Rol.CLIENTE);
            autenticar(usuario);
            when(usuarioRepository.existsByCorreo("otro@correo.com")).thenReturn(true);

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> usuarioService.actualizarPerfil(request("Ana Pérez", null, "otro@correo.com")));

            // Assert
            assertAll(
                    () -> assertEquals("CORREO_EN_USO", ex.getCodigo()),
                    () -> assertEquals("Este correo ya está en uso", ex.getMessage()),
                    () -> assertNull(usuario.getCorreoPendiente()),
                    () -> verify(usuarioRepository, never()).save(any()),
                    () -> verifyNoInteractions(tokenRepository, emailService)
            );
        }

        @Test
        void deberiaDejarCorreoPendienteYEnviarEnlaceAlNuevoCorreoCuandoElCorreoEstaDisponible() {
            // Arrange (escenario 4)
            Usuario usuario = usuario(Rol.CLIENTE);
            autenticar(usuario);
            when(usuarioRepository.existsByCorreo("nuevo@correo.com")).thenReturn(false);
            ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
            ArgumentCaptor<String> enlaceCaptor = ArgumentCaptor.forClass(String.class);

            // Act
            usuarioService.actualizarPerfil(request("Ana Pérez", null, " nuevo@correo.com "));

            // Assert
            verify(tokenRepository).save(tokenCaptor.capture());
            verify(emailService).enviarCorreoActualizacion(eq("nuevo@correo.com"), enlaceCaptor.capture(), any());
            Token token = tokenCaptor.getValue();
            String prefijo = BACKEND_URL + "/api/usuario/correo/confirmar/";
            String tokenPlano = enlaceCaptor.getValue().substring(prefijo.length());
            assertAll(
                    () -> assertEquals("ana@correo.com", usuario.getCorreo()),
                    () -> assertEquals("nuevo@correo.com", usuario.getCorreoPendiente()),
                    () -> verify(tokenRepository).deleteByUsuarioIdUsuarioAndTipo(ID_USUARIO, TipoToken.ACTUALIZACION),
                    () -> assertTrue(enlaceCaptor.getValue().startsWith(prefijo)),
                    () -> assertEquals(TipoToken.ACTUALIZACION, token.getTipo()),
                    () -> assertEquals(TokenHasher.hash(tokenPlano), token.getHashToken()),
                    () -> assertTrue(token.getFechaExpiracion().isAfter(LocalDateTime.now().plusMinutes(59))),
                    () -> assertTrue(token.getFechaExpiracion().isBefore(LocalDateTime.now().plusMinutes(61))),
                    () -> verify(usuarioRepository).save(usuario)
            );
        }

        @Test
        void deberiaActualizarTipoServicioYDescripcionCuandoElUsuarioEsProveedor() {
            // Arrange (escenario 5)
            Usuario usuario = usuario(Rol.PROVEEDOR);
            autenticar(usuario);
            Proveedor proveedor = proveedor();
            when(proveedorRepository.findByUsuarioIdUsuario(ID_USUARIO)).thenReturn(Optional.of(proveedor));
            ActualizarPerfilRequest request = request("Ana Pérez", null, "ana@correo.com");
            request.setTipoServicio("  Electricidad ");
            request.setDescripcion("  Instalaciones  ");

            // Act
            usuarioService.actualizarPerfil(request);

            // Assert
            assertAll(
                    () -> assertEquals("Instalaciones", proveedor.getDescripcion()),
                    () -> assertEquals(Rol.PROVEEDOR, usuario.getRol()),
                    () -> verify(proveedorRepository).save(proveedor)
            );
        }

        @Test
        void deberiaConservarDatosDelProveedorCuandoTipoServicioEsVacioYDescripcionEsNula() {
            // Arrange
            autenticar(usuario(Rol.PROVEEDOR));
            Proveedor proveedor = proveedor();
            when(proveedorRepository.findByUsuarioIdUsuario(ID_USUARIO)).thenReturn(Optional.of(proveedor));
            ActualizarPerfilRequest request = request("Ana Pérez", null, "ana@correo.com");
            request.setTipoServicio("   ");
            request.setDescripcion(null);

            // Act
            usuarioService.actualizarPerfil(request);

            // Assert
            assertAll(
                    () -> assertEquals("Reparaciones", proveedor.getDescripcion())
            );
        }

        @Test
        void deberiaLanzarProveedorNoEncontradoCuandoElProveedorNoTieneRegistro() {
            // Arrange
            autenticar(usuario(Rol.PROVEEDOR));
            when(proveedorRepository.findByUsuarioIdUsuario(ID_USUARIO)).thenReturn(Optional.empty());

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> usuarioService.actualizarPerfil(request("Ana Pérez", null, "ana@correo.com")));

            // Assert
            assertAll(
                    () -> assertEquals("PROVEEDOR_NO_ENCONTRADO", ex.getCodigo()),
                    () -> verify(usuarioRepository, never()).save(any())
            );
        }

        @Test
        void deberiaLanzarUsuarioNoAutenticadoCuandoNoHaySesion() {
            // Arrange
            SecurityContextHolder.clearContext();

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> usuarioService.actualizarPerfil(request("Ana", null, "ana@correo.com")));

            // Assert
            assertEquals("USUARIO_NO_AUTENTICADO", ex.getCodigo());
        }
    }

    // ================================================================ HU-03 confirmarCambioCorreo

    @Nested
    class ConfirmarCambioCorreo {

        private Token token(Usuario usuario, LocalDateTime expiracion) {
            Token token = new Token();
            token.setUsuario(usuario);
            token.setTipo(TipoToken.ACTUALIZACION);
            token.setFechaExpiracion(expiracion);
            return token;
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        void deberiaLanzarTokenInvalidoCuandoElTokenEsNuloOVacio(String tokenPlano) {
            // Arrange (valor parametrizado)

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> usuarioService.confirmarCambioCorreo(tokenPlano));

            // Assert
            assertAll(
                    () -> assertEquals("TOKEN_INVALIDO", ex.getCodigo()),
                    () -> assertEquals("El token de actualización es obligatorio", ex.getMessage())
            );
        }

        @Test
        void deberiaLanzarTokenInvalidoCuandoElTokenNoExisteOYaFueUsado() {
            // Arrange
            when(tokenRepository.findByHashTokenAndTipoAndFechaUsoIsNull(
                    TokenHasher.hash("abc"), TipoToken.ACTUALIZACION)).thenReturn(Optional.empty());

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> usuarioService.confirmarCambioCorreo("abc"));

            // Assert
            assertAll(
                    () -> assertEquals("TOKEN_INVALIDO", ex.getCodigo()),
                    () -> assertEquals("El token de actualización no es válido", ex.getMessage())
            );
        }

        @Test
        void deberiaLanzarTokenExpiradoCuandoElEnlaceExpiro() {
            // Arrange
            Usuario usuario = usuario(Rol.CLIENTE);
            usuario.setCorreoPendiente("nuevo@correo.com");
            when(tokenRepository.findByHashTokenAndTipoAndFechaUsoIsNull(
                    TokenHasher.hash("abc"), TipoToken.ACTUALIZACION))
                    .thenReturn(Optional.of(token(usuario, LocalDateTime.now().minusMinutes(1))));

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> usuarioService.confirmarCambioCorreo("abc"));

            // Assert
            assertAll(
                    () -> assertEquals("TOKEN_EXPIRADO", ex.getCodigo()),
                    () -> assertEquals("ana@correo.com", usuario.getCorreo())
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        void deberiaLanzarCambioNoPendienteCuandoNoHayCorreoPendiente(String correoPendiente) {
            // Arrange
            Usuario usuario = usuario(Rol.CLIENTE);
            usuario.setCorreoPendiente(correoPendiente);
            when(tokenRepository.findByHashTokenAndTipoAndFechaUsoIsNull(
                    TokenHasher.hash("abc"), TipoToken.ACTUALIZACION))
                    .thenReturn(Optional.of(token(usuario, LocalDateTime.now().plusMinutes(30))));

            // Act
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> usuarioService.confirmarCambioCorreo("abc"));

            // Assert
            assertEquals("CAMBIO_CORREO_NO_PENDIENTE", ex.getCodigo());
        }

        @Test
        void deberiaReemplazarCorreoYMarcarTokenUsadoCuandoElTokenEsValido() {
            // Arrange
            Usuario usuario = usuario(Rol.CLIENTE);
            usuario.setCorreoPendiente("nuevo@correo.com");
            Token token = token(usuario, LocalDateTime.now().plusMinutes(30));
            when(tokenRepository.findByHashTokenAndTipoAndFechaUsoIsNull(
                    TokenHasher.hash("abc"), TipoToken.ACTUALIZACION)).thenReturn(Optional.of(token));

            // Act
            usuarioService.confirmarCambioCorreo("abc");

            // Assert
            assertAll(
                    () -> assertEquals("nuevo@correo.com", usuario.getCorreo()),
                    () -> assertNull(usuario.getCorreoPendiente()),
                    () -> assertNotNull(token.getFechaUso()),
                    () -> verify(usuarioRepository).save(usuario),
                    () -> verify(tokenRepository).save(token)
            );
        }
    }
}
