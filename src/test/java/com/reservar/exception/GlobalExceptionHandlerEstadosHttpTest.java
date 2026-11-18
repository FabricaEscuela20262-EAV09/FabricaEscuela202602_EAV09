package com.reservar.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Correspondencia código de negocio → estado HTTP exigida por las historias de la Épica 2. */
class GlobalExceptionHandlerEstadosHttpTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "USUARIO_NO_AUTENTICADO,401",
            "ROL_NO_AUTORIZADO,403",
            "OFERTA_NO_PROPIA,403",
            "OFERTA_NO_ENCONTRADA,404",
            "OFERTA_NO_DISPONIBLE,404",
            "PROVEEDOR_NO_ACTIVO,400",
            "TIPO_SERVICIO_NO_ENCONTRADO,400",
            "CATEGORIA_NO_ENCONTRADA,400",
            "DISPONIBILIDAD_TRASLAPADA,400",
            "INTERVALO_HORARIO_INVALIDO,400",
            "BLOQUEO_TRASLAPADO,400",
            // Épica 3 (HU-11 a HU-15)
            "CUENTA_NO_VERIFICADA,403",
            "RESERVA_NO_PROPIA,403",
            "RESERVA_NO_ENCONTRADA,404",
            "CALIFICACION_NO_ENCONTRADA,404",
            "CALIFICACION_EXISTENTE,409",
            "HORARIO_NO_DISPONIBLE,409",
            "HORARIO_BLOQUEADO,409",
            "FUERA_DE_DISPONIBILIDAD,409",
            "RESERVA_FUERA_DE_HORARIO,409",
            "ESTADO_RESERVA_INVALIDO,400",
            "FECHA_RESERVA_INVALIDA,400",
            "ESTRELLAS_INVALIDAS,400",
            "MOTIVO_INVALIDO,400",
            "AUTORRESERVA_NO_PERMITIDA,400"
    })
    void deberiaMapearElCodigoDeNegocioAlEstadoHttpEsperado(String codigo, int estadoEsperado) {
        // Arrange
        BusinessException ex = new BusinessException(codigo, "mensaje");

        // Act
        ResponseEntity<ErrorResponse> respuesta = handler.handleBusinessException(ex);

        // Assert
        assertEquals(estadoEsperado, respuesta.getStatusCode().value());
        assertEquals(codigo, respuesta.getBody().getCodigo());
    }

    @Test
    void deberiaListarCadaCampoInvalidoCuandoFallaLaValidacionDeLaSolicitud() throws Exception {
        // Arrange
        BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "crearOfertaRequest");
        resultado.addError(new org.springframework.validation.FieldError(
                "crearOfertaRequest", "nombre", "Debe ingresar nombre de la oferta"));
        resultado.addError(new org.springframework.validation.FieldError(
                "crearOfertaRequest", "duracionMinutos", "Debe ingresar duración para la oferta"));
        Method metodo = getClass().getDeclaredMethod("deberiaListarCadaCampoInvalidoCuandoFallaLaValidacionDeLaSolicitud");
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(new MethodParameter(metodo, -1), resultado);

        // Act
        ResponseEntity<ErrorResponse> respuesta = handler.handleValidationException(ex);

        // Assert
        ErrorResponse body = respuesta.getBody();
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("DATOS_INVALIDOS", body.getCodigo());
        assertEquals("Hay errores en los datos enviados", body.getMensaje());
        assertEquals(List.of(
                "nombre: Debe ingresar nombre de la oferta",
                "duracionMinutos: Debe ingresar duración para la oferta"), body.getErrores());
        assertNotNull(body.getFecha());
    }
}
