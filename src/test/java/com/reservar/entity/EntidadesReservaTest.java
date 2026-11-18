package com.reservar.entity;

import com.reservar.entity.enums.EstadoReserva;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Callbacks de ciclo de vida de las entidades de la Épica 3 (Reserva, HistorialReserva, Calificacion). */
class EntidadesReservaTest {

    private static final LocalDateTime ANTIGUA = LocalDateTime.of(2020, 1, 1, 0, 0);

    @Test
    void deberiaAsignarFechasYEstadoPendientePorDefectoAlCrearUnaReserva() {
        // Arrange
        Reserva reserva = new Reserva();

        // Act
        reserva.onCreate();

        // Assert
        assertAll(
                () -> assertNotNull(reserva.getFechaCreacion()),
                () -> assertEquals(reserva.getFechaCreacion(), reserva.getFechaActualizacion()),
                () -> assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado())
        );
    }

    @Test
    void noDeberiaSobrescribirElEstadoDefinidoAlCrearUnaReserva() {
        // Arrange
        Reserva reserva = new Reserva();
        reserva.setEstado(EstadoReserva.CONFIRMADA);

        // Act
        reserva.onCreate();

        // Assert
        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
    }

    @Test
    void deberiaRefrescarLaFechaDeActualizacionAlModificarUnaReserva() {
        // Arrange
        Reserva reserva = new Reserva();
        reserva.setFechaActualizacion(ANTIGUA);

        // Act
        reserva.onUpdate();

        // Assert
        assertTrue(reserva.getFechaActualizacion().isAfter(ANTIGUA));
    }

    @Test
    void deberiaAsignarLaFechaDeCambioDelHistorialSiNoSeDefinio() {
        // Arrange
        HistorialReserva historial = new HistorialReserva();

        // Act
        historial.onCreate();

        // Assert
        assertNotNull(historial.getFechaCambio());
    }

    @Test
    void noDeberiaSobrescribirLaFechaDeCambioDelHistorialDefinidaPorElServicio() {
        // Arrange
        HistorialReserva historial = new HistorialReserva();
        historial.setFechaCambio(ANTIGUA);

        // Act
        historial.onCreate();

        // Assert
        assertEquals(ANTIGUA, historial.getFechaCambio());
    }

    @Test
    void deberiaAsignarFechasAlCrearUnaCalificacion() {
        // Arrange
        Calificacion calificacion = new Calificacion();

        // Act
        calificacion.onCreate();

        // Assert
        assertAll(
                () -> assertNotNull(calificacion.getFechaCreacion()),
                () -> assertEquals(calificacion.getFechaCreacion(), calificacion.getFechaActualizacion())
        );
    }

    @Test
    void deberiaRefrescarLaFechaDeActualizacionAlEditarUnaCalificacion() {
        // Arrange
        Calificacion calificacion = new Calificacion();
        calificacion.setFechaCreacion(ANTIGUA);
        calificacion.setFechaActualizacion(ANTIGUA);

        // Act
        calificacion.onUpdate();

        // Assert
        assertAll(
                () -> assertEquals(ANTIGUA, calificacion.getFechaCreacion()),
                () -> assertTrue(calificacion.getFechaActualizacion().isAfter(ANTIGUA))
        );
    }
}
