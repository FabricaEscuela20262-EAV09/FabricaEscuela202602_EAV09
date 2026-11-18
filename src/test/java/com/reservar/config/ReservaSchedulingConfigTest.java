package com.reservar.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El job que completa reservas finalizadas (FinalizacionReservaService) solo corre si el scheduling está activo. */
class ReservaSchedulingConfigTest {

    @Test
    void deberiaHabilitarLaProgramacionDeTareas() {
        // Act
        ReservaSchedulingConfig config = new ReservaSchedulingConfig();

        // Assert
        assertNotNull(config);
        assertTrue(ReservaSchedulingConfig.class.isAnnotationPresent(Configuration.class));
        assertTrue(ReservaSchedulingConfig.class.isAnnotationPresent(EnableScheduling.class));
    }
}
