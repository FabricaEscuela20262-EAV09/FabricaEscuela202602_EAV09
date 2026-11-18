package com.reservar.dto.reserva;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Validaciones de formato de los DTO de la Épica 3 (HU-11 a HU-15). */
class ReservaRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    private static Set<String> camposInvalidos(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static CrearReservaRequest reserva(Long idServicio, LocalDateTime inicio) {
        CrearReservaRequest request = new CrearReservaRequest();
        request.setIdServicio(idServicio);
        request.setFechaInicio(inicio);
        return request;
    }

    private static CalificacionRequest calificacion(Short estrellas, String comentario) {
        CalificacionRequest request = new CalificacionRequest();
        request.setEstrellas(estrellas);
        request.setComentario(comentario);
        return request;
    }

    @Test
    void deberiaAceptarUnaReservaFutura() {
        // Act + Assert
        assertTrue(camposInvalidos(reserva(10L, LocalDateTime.now().plusDays(1))).isEmpty());
    }

    @Test
    void deberiaExigirServicioYFechaEnLaReserva() {
        // Act + Assert
        assertEquals(Set.of("idServicio", "fechaInicio"), camposInvalidos(reserva(null, null)));
    }

    @Test
    void deberiaRechazarUnaReservaEnElPasado() {
        // Act + Assert
        assertEquals(Set.of("fechaInicio"), camposInvalidos(reserva(10L, LocalDateTime.now().minusMinutes(5))));
    }

    @ParameterizedTest
    @ValueSource(shorts = {1, 3, 5})
    void deberiaAceptarEstrellasEntreUnoYCinco(short estrellas) {
        // Act + Assert
        assertTrue(camposInvalidos(calificacion(estrellas, "Muy bien")).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(shorts = {0, 6, -1})
    void deberiaRechazarEstrellasFueraDeRango(short estrellas) {
        // Act + Assert
        assertEquals(Set.of("estrellas"), camposInvalidos(calificacion(estrellas, null)));
    }

    @Test
    void deberiaExigirLasEstrellas() {
        // Act + Assert
        assertEquals(Set.of("estrellas"), camposInvalidos(calificacion(null, null)));
    }

    @Test
    void deberiaLimitarElComentarioA500Caracteres() {
        // Act + Assert
        assertTrue(camposInvalidos(calificacion((short) 4, "x".repeat(500))).isEmpty());
        assertEquals(Set.of("comentario"), camposInvalidos(calificacion((short) 4, "x".repeat(501))));
    }

    @Test
    void deberiaLimitarElMotivoA255Caracteres() {
        // Arrange
        MotivoReservaRequest valido = new MotivoReservaRequest();
        valido.setMotivo("x".repeat(255));
        MotivoReservaRequest largo = new MotivoReservaRequest();
        largo.setMotivo("x".repeat(256));

        // Act + Assert
        assertTrue(camposInvalidos(valido).isEmpty());
        assertTrue(camposInvalidos(new MotivoReservaRequest()).isEmpty());
        assertEquals(Set.of("motivo"), camposInvalidos(largo));
    }
}
