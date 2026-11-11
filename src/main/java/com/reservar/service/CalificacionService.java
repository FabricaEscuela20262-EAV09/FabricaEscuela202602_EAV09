package com.reservar.service;

import com.reservar.dto.reserva.CalificacionRequest;
import com.reservar.dto.reserva.CalificacionResponse;
import com.reservar.entity.Calificacion;
import com.reservar.entity.Reserva;
import com.reservar.entity.Usuario;
import com.reservar.entity.enums.EstadoReserva;
import com.reservar.entity.enums.EstadoUsuario;
import com.reservar.entity.enums.Rol;
import com.reservar.exception.BusinessException;
import com.reservar.repository.CalificacionRepository;
import com.reservar.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class CalificacionService {
    private final CalificacionRepository calificaciones;
    private final ReservaRepository reservas;
    private final Clock clock;

    public CalificacionService(CalificacionRepository calificaciones, ReservaRepository reservas, Clock clock) {
        this.calificaciones = calificaciones;
        this.reservas = reservas;
        this.clock = clock;
    }

    @Transactional
    public CalificacionResponse crear(Usuario usuario, Long idReserva, CalificacionRequest request) {
        Reserva reserva = obtenerReservaCalificable(usuario, idReserva);
        if (calificaciones.findByReservaIdReserva(idReserva).isPresent()) {
            throw new BusinessException("CALIFICACION_EXISTENTE", "Esta reserva ya tiene calificación. Puedes editarla.");
        }
        Calificacion calificacion = new Calificacion();
        calificacion.setReserva(reserva);
        return guardar(calificacion, request);
    }

    @Transactional
    public CalificacionResponse editar(Usuario usuario, Long idReserva, CalificacionRequest request) {
        obtenerReservaCalificable(usuario, idReserva);
        Calificacion calificacion = calificaciones.findByReservaIdReserva(idReserva)
                .orElseThrow(() -> new BusinessException("CALIFICACION_NO_ENCONTRADA", "La reserva aún no tiene calificación."));
        return guardar(calificacion, request);
    }

    private Reserva obtenerReservaCalificable(Usuario usuario, Long idReserva) {
        if (usuario == null || usuario.getIdUsuario() == null) {
            throw new BusinessException("USUARIO_NO_AUTENTICADO", "Debes iniciar sesión para realizar esta operación.");
        }
        if (usuario.getRol() != Rol.CLIENTE) {
            throw new BusinessException("ROL_NO_AUTORIZADO", "Solo el cliente puede calificar el servicio recibido.");
        }
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new BusinessException("CUENTA_NO_VERIFICADA", "Debes tener la cuenta activa para calificar.");
        }
        // La misma fila se bloquea al crear y editar para serializar calificaciones concurrentes.
        Reserva reserva = reservas.findByIdParaActualizar(idReserva)
                .orElseThrow(() -> new BusinessException("RESERVA_NO_ENCONTRADA", "La reserva indicada no existe."));
        if (!usuario.getIdUsuario().equals(reserva.getCliente().getIdUsuario())) {
            throw new BusinessException("RESERVA_NO_PROPIA", "No puedes calificar una reserva de otro cliente.");
        }
        if (reserva.getEstado() == EstadoReserva.CONFIRMADA
                && !reserva.getFechaFin().isAfter(LocalDateTime.now(clock))) {
            reserva.setEstado(EstadoReserva.COMPLETADA);
            reservas.save(reserva);
        }
        if (reserva.getEstado() != EstadoReserva.COMPLETADA) {
            throw new BusinessException("ESTADO_RESERVA_INVALIDO", "Solo puedes calificar reservas en estado COMPLETADA.");
        }
        return reserva;
    }

    private CalificacionResponse guardar(Calificacion calificacion, CalificacionRequest request) {
        if (request == null || request.getEstrellas() == null
                || request.getEstrellas() < 1 || request.getEstrellas() > 5) {
            throw new BusinessException("ESTRELLAS_INVALIDAS", "Las estrellas deben estar entre 1 y 5.");
        }
        if (request.getComentario() != null && request.getComentario().length() > 500) {
            throw new BusinessException("COMENTARIO_INVALIDO", "El comentario no puede superar 500 caracteres.");
        }
        calificacion.setEstrellas(request.getEstrellas());
        calificacion.setComentario(request.getComentario() == null || request.getComentario().isBlank()
                ? null : request.getComentario().trim());
        Calificacion guardada = calificaciones.saveAndFlush(calificacion);
        return new CalificacionResponse(guardada.getIdCalificacion(), guardada.getReserva().getIdReserva(),
                guardada.getEstrellas(), guardada.getComentario(), guardada.getFechaCreacion(), guardada.getFechaActualizacion());
    }
}
