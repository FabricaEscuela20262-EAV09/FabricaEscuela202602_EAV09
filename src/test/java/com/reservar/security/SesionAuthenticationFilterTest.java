package com.reservar.security;

import com.reservar.entity.Sesion;
import com.reservar.entity.Usuario;
import com.reservar.entity.enums.Rol;
import com.reservar.repository.SesionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SesionAuthenticationFilterTest {

    @Mock
    private SesionRepository sesionRepository;

    @InjectMocks
    private SesionAuthenticationFilter filtro;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deberiaAutenticarConRolDelUsuarioCuandoElBearerTokenCorrespondeAUnaSesionVigente() throws Exception {
        // Arrange
        Usuario usuario = new Usuario();
        usuario.setRol(Rol.PROVEEDOR);
        Sesion sesion = new Sesion();
        sesion.setUsuario(usuario);
        when(sesionRepository.findByHashTokenAndFechaRevocacionIsNullAndFechaExpiracionAfter(
                eq(TokenHasher.hash("mi-token")), any(LocalDateTime.class))).thenReturn(Optional.of(sesion));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer  mi-token ");
        MockFilterChain cadena = new MockFilterChain();

        // Act
        filtro.doFilter(request, new MockHttpServletResponse(), cadena);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertAll(
                () -> assertNotNull(auth),
                () -> assertEquals(usuario, auth.getPrincipal()),
                () -> assertEquals("ROLE_PROVEEDOR", auth.getAuthorities().iterator().next().getAuthority()),
                () -> assertNotNull(auth.getDetails()),
                () -> assertNotNull(cadena.getRequest())
        );
    }

    @Test
    void deberiaContinuarSinAutenticarCuandoLaSesionNoExisteEstaRevocadaOExpiro() throws Exception {
        // Arrange
        when(sesionRepository.findByHashTokenAndFechaRevocacionIsNullAndFechaExpiracionAfter(
                eq(TokenHasher.hash("token-viejo")), any(LocalDateTime.class))).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-viejo");
        MockFilterChain cadena = new MockFilterChain();

        // Act
        filtro.doFilter(request, new MockHttpServletResponse(), cadena);

        // Assert
        assertAll(
                () -> assertNull(SecurityContextHolder.getContext().getAuthentication()),
                () -> assertNotNull(cadena.getRequest())
        );
    }

    @Test
    void deberiaContinuarSinConsultarSesionesCuandoNoHayCabeceraAuthorization() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockFilterChain cadena = new MockFilterChain();

        // Act
        filtro.doFilter(request, new MockHttpServletResponse(), cadena);

        // Assert
        assertAll(
                () -> assertNull(SecurityContextHolder.getContext().getAuthentication()),
                () -> assertNotNull(cadena.getRequest()),
                () -> verifyNoInteractions(sesionRepository)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"Basic dXN1YXJpbzpjbGF2ZQ==", "bearer abc", "Bearerabc"})
    void deberiaIgnorarLaCabeceraCuandoNoUsaElEsquemaBearer(String cabecera) throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", cabecera);
        MockFilterChain cadena = new MockFilterChain();

        // Act
        filtro.doFilter(request, new MockHttpServletResponse(), cadena);

        // Assert
        assertAll(
                () -> assertNull(SecurityContextHolder.getContext().getAuthentication()),
                () -> verifyNoInteractions(sesionRepository)
        );
    }
}
