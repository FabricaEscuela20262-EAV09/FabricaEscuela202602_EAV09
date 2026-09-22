package com.reservar.exception;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void deberiaResponder400ConCodigoYMensajeCuandoOcurreUnaBusinessException() {
        // Arrange
        BusinessException ex = new BusinessException("CUENTA_BLOQUEADA", "Cuenta bloqueada");

        // Act
        ResponseEntity<ErrorResponse> respuesta = handler.handleBusinessException(ex);

        // Assert
        ErrorResponse body = respuesta.getBody();
        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode()),
                () -> assertEquals("CUENTA_BLOQUEADA", body.getCodigo()),
                () -> assertEquals("Cuenta bloqueada", body.getMensaje()),
                () -> assertNotNull(body.getFecha()),
                () -> assertNull(body.getErrores())
        );
    }

    @Test
    void deberiaResponder400ConDatosInvalidosCuandoOcurreUnaConstraintViolationException() {
        // Arrange
        ConstraintViolationException ex = new ConstraintViolationException("token: no debe estar vacío", Set.of());

        // Act
        ResponseEntity<ErrorResponse> respuesta = handler.handleConstraintViolation(ex);

        // Assert
        ErrorResponse body = respuesta.getBody();
        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode()),
                () -> assertEquals("DATOS_INVALIDOS", body.getCodigo()),
                () -> assertEquals("token: no debe estar vacío", body.getMensaje()),
                () -> assertNull(body.getErrores())
        );
    }
}
