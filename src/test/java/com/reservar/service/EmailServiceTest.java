package com.reservar.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    private static final String DESTINATARIO = "ana@correo.com";
    private static final String ENLACE = "http://enlace.test/abc123";
    private static final LocalDateTime EXPIRACION = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private SimpleMailMessage mensajeEnviado() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    @Test
    void deberiaEnviarCorreoDeVerificacionConEnlaceYExpiracionCuandoSeRegistraUnUsuario() {
        // Arrange (constantes)

        // Act
        emailService.enviarCorreoVerificacion(DESTINATARIO, ENLACE, EXPIRACION);

        // Assert
        SimpleMailMessage mensaje = mensajeEnviado();
        assertAll(
                () -> assertArrayEquals(new String[]{DESTINATARIO}, mensaje.getTo()),
                () -> assertEquals("Verifica tu cuenta - Reservar", mensaje.getSubject()),
                () -> assertTrue(mensaje.getText().contains(ENLACE)),
                () -> assertTrue(mensaje.getText().contains("2026-01-15T10:30"))
        );
    }

    @Test
    void deberiaEnviarCorreoDeRecuperacionConEnlaceYExpiracionCuandoSeSolicitaRecuperar() {
        // Arrange (constantes)

        // Act
        emailService.enviarCorreoRecuperacion(DESTINATARIO, ENLACE, EXPIRACION);

        // Assert
        SimpleMailMessage mensaje = mensajeEnviado();
        assertAll(
                () -> assertArrayEquals(new String[]{DESTINATARIO}, mensaje.getTo()),
                () -> assertEquals("Recuperación de contraseña - Reservar", mensaje.getSubject()),
                () -> assertTrue(mensaje.getText().contains(ENLACE)),
                () -> assertTrue(mensaje.getText().contains("2026-01-15T10:30"))
        );
    }

    @Test
    void deberiaEnviarCorreoDeActualizacionConEnlaceYExpiracionCuandoSeCambiaElCorreo() {
        // Arrange (constantes)

        // Act
        emailService.enviarCorreoActualizacion(DESTINATARIO, ENLACE, EXPIRACION);

        // Assert
        SimpleMailMessage mensaje = mensajeEnviado();
        assertAll(
                () -> assertArrayEquals(new String[]{DESTINATARIO}, mensaje.getTo()),
                () -> assertEquals("Confirmación de cambio de correo - Reservar", mensaje.getSubject()),
                () -> assertTrue(mensaje.getText().contains(ENLACE)),
                () -> assertTrue(mensaje.getText().contains("2026-01-15T10:30"))
        );
    }
}
