package com.reservar.entity;

import com.reservar.entity.enums.EstadoCodigoProveedor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Callbacks de ciclo de vida de las entidades de la Épica 1 (registro, sesión, tokens y auditoría de logueo). */
class EntidadesAutenticacionTest {

    private static final LocalDateTime ANTIGUA = LocalDateTime.of(2020, 1, 1, 0, 0);

    @Test
    void deberiaAsignarFechasAlCrearUnUsuario() {
        // Arrange
        Usuario usuario = new Usuario();

        // Act
        usuario.onCreate();

        // Assert
        assertAll(
                () -> assertNotNull(usuario.getFechaCreacion()),
                () -> assertEquals(usuario.getFechaCreacion(), usuario.getFechaActualizacion()),
                () -> assertEquals(0, usuario.getIntentosFallidos())
        );
    }

    @Test
    void deberiaRefrescarSoloLaFechaDeActualizacionAlModificarUnUsuario() {
        // Arrange
        Usuario usuario = new Usuario();
        usuario.setFechaCreacion(ANTIGUA);
        usuario.setFechaActualizacion(ANTIGUA);

        // Act
        usuario.onUpdate();

        // Assert
        assertAll(
                () -> assertEquals(ANTIGUA, usuario.getFechaCreacion()),
                () -> assertTrue(usuario.getFechaActualizacion().isAfter(ANTIGUA))
        );
    }

    @Test
    void deberiaAsignarFechaYEstadoDisponiblePorDefectoAlCrearUnCodigoDeProveedor() {
        // Arrange
        CodigoProveedor codigo = new CodigoProveedor();

        // Act
        codigo.onCreate();

        // Assert
        assertAll(
                () -> assertNotNull(codigo.getFechaCreacion()),
                () -> assertEquals(EstadoCodigoProveedor.DISPONIBLE, codigo.getEstado())
        );
    }

    @Test
    void noDeberiaSobrescribirFechaNiEstadoDefinidosEnUnCodigoDeProveedor() {
        // Arrange
        CodigoProveedor codigo = new CodigoProveedor();
        codigo.setFechaCreacion(ANTIGUA);
        codigo.setEstado(EstadoCodigoProveedor.USADO);

        // Act
        codigo.onCreate();

        // Assert
        assertAll(
                () -> assertEquals(ANTIGUA, codigo.getFechaCreacion()),
                () -> assertEquals(EstadoCodigoProveedor.USADO, codigo.getEstado())
        );
    }

    @Test
    void deberiaAsignarLaFechaDeCreacionDeLaSesionSoloSiNoSeDefinio() {
        // Arrange
        Sesion nueva = new Sesion();
        Sesion existente = new Sesion();
        existente.setFechaCreacion(ANTIGUA);

        // Act
        nueva.onCreate();
        existente.onCreate();

        // Assert
        assertAll(
                () -> assertNotNull(nueva.getFechaCreacion()),
                () -> assertEquals(ANTIGUA, existente.getFechaCreacion())
        );
    }

    @Test
    void deberiaAsignarLaFechaDeCreacionDelTokenSoloSiNoSeDefinio() {
        // Arrange
        Token nuevo = new Token();
        Token existente = new Token();
        existente.setFechaCreacion(ANTIGUA);

        // Act
        nuevo.onCreate();
        existente.onCreate();

        // Assert
        assertAll(
                () -> assertNotNull(nuevo.getFechaCreacion()),
                () -> assertEquals(ANTIGUA, existente.getFechaCreacion())
        );
    }

    @Test
    void deberiaAsignarLaFechaDelIntentoDeLogueoSoloSiNoSeDefinio() {
        // Arrange
        IntentoLogueo nuevo = new IntentoLogueo();
        IntentoLogueo existente = new IntentoLogueo();
        existente.setFechaIntento(ANTIGUA);

        // Act
        nuevo.onCreate();
        existente.onCreate();

        // Assert
        assertAll(
                () -> assertNotNull(nuevo.getFechaIntento()),
                () -> assertEquals(ANTIGUA, existente.getFechaIntento())
        );
    }
}
