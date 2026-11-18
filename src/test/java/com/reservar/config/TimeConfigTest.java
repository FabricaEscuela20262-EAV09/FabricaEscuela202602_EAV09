package com.reservar.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Bug #16: toda la lógica de fechas y horas (HU-08, HU-10, HU-11 a HU-15) usa la zona de Colombia. */
class TimeConfigTest {

    @Test
    void deberiaExponerUnRelojEnLaZonaHorariaDeBogota() {
        // Act
        Clock clock = new TimeConfig().clock();

        // Assert
        assertEquals(ZoneId.of("America/Bogota"), clock.getZone());
    }
}
