package com.reservar.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-01 / HU-05: la contraseña debe tener mínimo 8 caracteres,
 * al menos una mayúscula, un número y un carácter especial.
 */
class PasswordValidatorTest {

    private static final String ERROR_LONGITUD = "La contraseña debe tener mínimo 8 caracteres";
    private static final String ERROR_MAYUSCULA = "La contraseña debe contener al menos una mayúscula";
    private static final String ERROR_NUMERO = "La contraseña debe contener al menos un número";
    private static final String ERROR_ESPECIAL = "La contraseña debe contener al menos un carácter especial";

    @ParameterizedTest
    @ValueSource(strings = {"Clave123!", "Abcdef1$", "MUYSEGURA2026#", "P4ssw0rd with space"})
    void deberiaRetornarListaVaciaCuandoLaContrasenaCumpleTodasLasReglas(String contrasena) {
        // Arrange (valor parametrizado)

        // Act
        List<String> errores = PasswordValidator.validate(contrasena);

        // Assert
        assertTrue(errores.isEmpty());
    }

    @Test
    void deberiaAceptarContrasenaDeExactamente8Caracteres() {
        // Arrange
        String contrasena = "Abcde1!x";

        // Act
        List<String> errores = PasswordValidator.validate(contrasena);

        // Assert
        assertTrue(errores.isEmpty());
    }

    @Test
    void deberiaRechazarPorLongitudCuandoTiene7Caracteres() {
        // Arrange
        String contrasena = "Abcd1!x";

        // Act
        List<String> errores = PasswordValidator.validate(contrasena);

        // Assert
        assertEquals(List.of(ERROR_LONGITUD), errores);
    }

    @ParameterizedTest
    @CsvSource({
            "clave123!,  La contraseña debe contener al menos una mayúscula",
            "Clavesss!,  La contraseña debe contener al menos un número",
            "Clave1234,  La contraseña debe contener al menos un carácter especial"
    })
    void deberiaReportarSoloLaReglaIncumplidaCuandoFaltaUnRequisito(String contrasena, String errorEsperado) {
        // Arrange (valores parametrizados)

        // Act
        List<String> errores = PasswordValidator.validate(contrasena);

        // Assert
        assertEquals(List.of(errorEsperado), errores);
    }

    @Test
    void deberiaReportarLasCuatroReglasCuandoLaContrasenaEsNula() {
        // Arrange
        String contrasena = null;

        // Act
        List<String> errores = PasswordValidator.validate(contrasena);

        // Assert
        assertEquals(List.of(ERROR_LONGITUD, ERROR_MAYUSCULA, ERROR_NUMERO, ERROR_ESPECIAL), errores);
    }

    @Test
    void deberiaReportarLasCuatroReglasCuandoLaContrasenaEsVacia() {
        // Arrange
        String contrasena = "";

        // Act
        List<String> errores = PasswordValidator.validate(contrasena);

        // Assert
        assertEquals(List.of(ERROR_LONGITUD, ERROR_MAYUSCULA, ERROR_NUMERO, ERROR_ESPECIAL), errores);
    }

    @Test
    void deberiaReportarVariasReglasCuandoSoloTieneMinusculas() {
        // Arrange
        String contrasena = "abcdefgh";

        // Act
        List<String> errores = PasswordValidator.validate(contrasena);

        // Assert
        assertAll(
                () -> assertEquals(3, errores.size()),
                () -> assertTrue(errores.contains(ERROR_MAYUSCULA)),
                () -> assertTrue(errores.contains(ERROR_NUMERO)),
                () -> assertTrue(errores.contains(ERROR_ESPECIAL))
        );
    }
}
