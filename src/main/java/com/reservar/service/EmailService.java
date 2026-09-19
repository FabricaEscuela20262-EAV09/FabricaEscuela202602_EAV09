package com.reservar.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void enviarCorreoVerificacion(
            String destinatario,
            String enlace,
            LocalDateTime expiracion
    ) {
        SimpleMailMessage mensaje = new SimpleMailMessage();

        mensaje.setTo(destinatario);
        mensaje.setSubject("Verifica tu cuenta - Reservar");

        mensaje.setText("""
                Hola,

                Gracias por registrarte en Reservar.

                Para activar tu cuenta, haz clic en el siguiente enlace:

                %s

                Este enlace es válido hasta:
                %s

                El enlace solo puede utilizarse una vez.

                Si no creaste esta cuenta, puedes ignorar este mensaje.

                Saludos,
                Equipo Reservar
                """.formatted(enlace, expiracion));

        mailSender.send(mensaje);
    }

    public void enviarCorreoRecuperacion(
            String destinatario,
            String enlace,
            LocalDateTime expiracion
    ) {
        SimpleMailMessage mensaje = new SimpleMailMessage();

        mensaje.setTo(destinatario);
        mensaje.setSubject("Recuperación de contraseña - Reservar");

        mensaje.setText("""
                Hola,

                Se ha solicitado una recuperación de contraseña para tu cuenta en Reservar.

                Para establecer una nueva contraseña, utiliza el siguiente enlace:

                %s

                Este enlace es válido hasta:
                %s

                El enlace solo puede utilizarse una vez.

                Si no solicitaste esta recuperación, puedes ignorar este mensaje.

                Saludos,
                Equipo Reservar
                """.formatted(enlace, expiracion));

        mailSender.send(mensaje);
    }

    public void enviarCorreoActualizacion(
            String destinatario,
            String enlace,
            LocalDateTime expiracion
    ) {
        SimpleMailMessage mensaje = new SimpleMailMessage();

        mensaje.setTo(destinatario);
        mensaje.setSubject("Confirmación de cambio de correo - Reservar");

        mensaje.setText("""
                Hola,

                Se ha solicitado un cambio de correo electrónico para tu cuenta en Reservar.

                Para confirmar el nuevo correo, utiliza el siguiente enlace:

                %s

                Este enlace es válido hasta:
                %s

                El enlace solo puede utilizarse una vez.

                Si no solicitaste este cambio, puedes ignorar este mensaje.

                Saludos,
                Equipo Reservar
                """.formatted(enlace, expiracion));

        mailSender.send(mensaje);
    }
}