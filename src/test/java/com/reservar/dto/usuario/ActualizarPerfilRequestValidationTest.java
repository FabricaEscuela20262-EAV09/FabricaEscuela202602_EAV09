package com.reservar.dto.usuario;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-03 escenario 2: validación de nombre y teléfono colombiano
 * (válidos: 3001234567 o +57 300 1234567).
 */
class ActualizarPerfilRequestValidationTest {

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

    private static ActualizarPerfilRequest request(String nombre, String telefono, String correo) {
        ActualizarPerfilRequest request = new ActualizarPerfilRequest();
        request.setNombreCompleto(nombre);
        request.setTelefono(telefono);
        request.setCorreo(correo);
        return request;
    }

    @ParameterizedTest
    @ValueSource(strings = {"3001234567", "+57 300 1234567", "+573001234567", "300 123 4567", ""})
    void deberiaAceptarElTelefonoCuandoTieneFormatoColombianoOEstaVacio(String telefono) {
        // Arrange
        ActualizarPerfilRequest request = request("Ana", telefono, "ana@correo.com");

        // Act
        Set<ConstraintViolation<ActualizarPerfilRequest>> violaciones = validator.validate(request);

        // Assert
        assertTrue(violaciones.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2001234567", "300123456", "30012345678", "+58 300 1234567", "57 3001234567", "abc"})
    void deberiaRechazarElTelefonoCuandoNoEsUnNumeroColombianoValido(String telefono) {
        // Arrange
        ActualizarPerfilRequest request = request("Ana", telefono, "ana@correo.com");

        // Act
        Set<ConstraintViolation<ActualizarPerfilRequest>> violaciones = validator.validate(request);

        // Assert
        assertAll(
                () -> assertEquals(1, violaciones.size()),
                () -> assertEquals("El teléfono debe tener un formato válido para Colombia",
                        violaciones.iterator().next().getMessage())
        );
    }

    @Test
    void deberiaRechazarCuandoElNombreEstaVacio() {
        // Arrange
        ActualizarPerfilRequest request = request("  ", null, "ana@correo.com");

        // Act
        Set<ConstraintViolation<ActualizarPerfilRequest>> violaciones = validator.validate(request);

        // Assert
        assertAll(
                () -> assertEquals(1, violaciones.size()),
                () -> assertEquals("Debe registrar su nombre completo", violaciones.iterator().next().getMessage())
        );
    }

    @Test
    void deberiaRechazarCuandoElNombreSupera100Caracteres() {
        // Arrange
        ActualizarPerfilRequest request = request("a".repeat(101), null, "ana@correo.com");

        // Act
        Set<ConstraintViolation<ActualizarPerfilRequest>> violaciones = validator.validate(request);

        // Assert
        assertEquals("El nombre completo no puede superar los 100 caracteres",
                violaciones.iterator().next().getMessage());
    }

    @Test
    void deberiaAceptarCuandoElNombreTieneExactamente100Caracteres() {
        // Arrange
        ActualizarPerfilRequest request = request("a".repeat(100), null, "ana@correo.com");

        // Act
        Set<ConstraintViolation<ActualizarPerfilRequest>> violaciones = validator.validate(request);

        // Assert
        assertTrue(violaciones.isEmpty());
    }

    @Test
    void deberiaRechazarCuandoElCorreoNoTieneFormatoValido() {
        // Arrange
        ActualizarPerfilRequest request = request("Ana", null, "ana-correo.com");

        // Act
        Set<ConstraintViolation<ActualizarPerfilRequest>> violaciones = validator.validate(request);

        // Assert
        assertEquals("El correo debe tener un formato válido", violaciones.iterator().next().getMessage());
    }
}
