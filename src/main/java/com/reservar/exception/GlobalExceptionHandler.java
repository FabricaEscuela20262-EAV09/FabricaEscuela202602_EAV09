package com.reservar.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex) {

        HttpStatus status = switch (ex.getCodigo()) {
            case "USUARIO_NO_AUTENTICADO" -> HttpStatus.UNAUTHORIZED;
            case "ROL_NO_AUTORIZADO", "OFERTA_NO_PROPIA", "CUENTA_NO_VERIFICADA" -> HttpStatus.FORBIDDEN;
            case "OFERTA_NO_ENCONTRADA", "OFERTA_NO_DISPONIBLE" -> HttpStatus.NOT_FOUND;
            case "HORARIO_NO_DISPONIBLE", "HORARIO_BLOQUEADO", "FUERA_DE_DISPONIBILIDAD", "RESERVA_FUERA_DE_HORARIO" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };

        ErrorResponse response = new ErrorResponse(
                ex.getCodigo(),
                ex.getMessage(),
                LocalDateTime.now(),
                null
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {

        List<String> errores = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        ErrorResponse response = new ErrorResponse(
                "DATOS_INVALIDOS",
                "Hay errores en los datos enviados",
                LocalDateTime.now(),
                errores
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex) {

        ErrorResponse response = new ErrorResponse(
                "DATOS_INVALIDOS",
                ex.getMessage(),
                LocalDateTime.now(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        ErrorResponse response = new ErrorResponse(
                "PARAMETRO_INVALIDO",
                "El identificador del servicio no es válido.",
                LocalDateTime.now(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
}

