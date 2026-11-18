package com.reservar.service;

import com.reservar.entity.enums.EstadoReserva;
import com.reservar.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

/** HU-14/HU-15: el job programado pasa a COMPLETADA las reservas confirmadas cuya hora de fin ya pasó. */
@ExtendWith(MockitoExtension.class)
class FinalizacionReservaServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-10T15:00:00Z"), ZoneId.of("America/Bogota"));

    @Mock
    private ReservaRepository reservaRepository;

    @Test
    void deberiaCompletarLasConfirmadasFinalizadasSegunLaHoraDeBogota() {
        // Arrange
        FinalizacionReservaService service = new FinalizacionReservaService(reservaRepository, CLOCK);

        // Act
        service.completarFinalizadas();

        // Assert
        verify(reservaRepository).completarFinalizadas(EstadoReserva.CONFIRMADA, EstadoReserva.COMPLETADA,
                LocalDateTime.of(2026, 11, 10, 10, 0));
    }

    @Test
    void deberiaEstarProgramadoConUnaFrecuenciaFija() throws NoSuchMethodException {
        // Act
        Scheduled programacion = FinalizacionReservaService.class
                .getMethod("completarFinalizadas").getAnnotation(Scheduled.class);

        // Assert
        assertNotNull(programacion);
        assertTrue(programacion.fixedDelay() > 0);
    }
}
