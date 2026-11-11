package com.reservar.service;

import com.reservar.dto.reserva.ReservaResponse;
import com.reservar.dto.reserva.HistorialReservaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import com.reservar.entity.HistorialReserva;
import com.reservar.entity.Reserva;
import com.reservar.entity.Usuario;
import com.reservar.entity.enums.EstadoReserva;
import com.reservar.entity.enums.EstadoUsuario;
import com.reservar.entity.enums.Rol;
import com.reservar.exception.BusinessException;
import com.reservar.repository.HistorialReservaRepository;
import com.reservar.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class GestionReservaService {
    private final ReservaRepository reservaRepository;
    private final HistorialReservaRepository historialRepository;
    private final Clock clock;

    public GestionReservaService(ReservaRepository reservaRepository,
                                 HistorialReservaRepository historialRepository, Clock clock) {
        this.reservaRepository = reservaRepository;
        this.historialRepository = historialRepository;
        this.clock = clock;
    }

    @Transactional
    public ReservaResponse confirmar(Usuario usuario, Long idReserva) {
        Reserva reserva = obtenerReservaPropiaProveedor(usuario, idReserva);
        validarPendiente(reserva);
        registrarTransicion(reserva, usuario, EstadoReserva.CONFIRMADA, null);
        return convertirRespuesta(reserva);
    }

    @Transactional
    public ReservaResponse rechazar(Usuario usuario, Long idReserva, String motivo) {
        Reserva reserva = obtenerReservaPropiaProveedor(usuario, idReserva);
        validarPendiente(reserva);
        registrarTransicion(reserva, usuario, EstadoReserva.RECHAZADA, motivo);
        return convertirRespuesta(reserva);
    }

    @Transactional
    public ReservaResponse cancelar(Usuario usuario, Long idReserva, String motivo) {
        validarUsuarioActivo(usuario);
        Reserva reserva = obtenerParaActualizar(idReserva);
        boolean cliente = usuario.getRol() == Rol.CLIENTE
                && usuario.getIdUsuario().equals(reserva.getCliente().getIdUsuario());
        boolean proveedor = usuario.getRol() == Rol.PROVEEDOR && esProveedor(usuario, reserva);
        if (!cliente && !proveedor) {
            throw new BusinessException("RESERVA_NO_PROPIA", "No tienes permiso para cancelar esta reserva.");
        }
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA
                && !(cliente && reserva.getEstado() == EstadoReserva.PENDIENTE)) {
            throw estadoNoPermitido(reserva);
        }
        if (!reserva.getFechaFin().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException("ESTADO_RESERVA_INVALIDO", "No se puede cancelar una reserva que ya finalizó.");
        }
        registrarTransicion(reserva, usuario, EstadoReserva.CANCELADA, motivo);
        return convertirRespuesta(reserva);
    }

    @Transactional
    public Page<ReservaResponse> historial(Usuario usuario,
            EstadoReserva estado, Pageable pageable) {
        validarUsuarioActivo(usuario);
        completarFinalizadas();
        return reservaRepository.findHistorialPropio(usuario.getIdUsuario(), estado, pageable)
                .map(this::convertirRespuesta);
    }

    @Transactional
    public ReservaResponse detalle(Usuario usuario, Long idReserva) {
        validarUsuarioActivo(usuario);
        completarFinalizadas();
        Reserva reserva = obtenerParaActualizar(idReserva);
        validarParticipante(usuario, reserva);
        return convertirRespuesta(reserva);
    }

    @Transactional(readOnly = true)
    public List<HistorialReservaResponse> auditoria(Usuario usuario, Long idReserva) {
        validarUsuarioActivo(usuario);
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new BusinessException("RESERVA_NO_ENCONTRADA", "La reserva indicada no existe."));
        validarParticipante(usuario, reserva);
        return historialRepository.findByReservaIdReservaOrderByFechaCambioAscIdHistorialAsc(idReserva).stream()
                .map(h -> new HistorialReservaResponse(h.getIdHistorial(),
                        h.getActor().getIdUsuario(), h.getEstadoAnterior(), h.getEstadoNuevo(),
                        h.getFechaCambio(), h.getMotivo())).toList();
    }

    private void completarFinalizadas() {
        reservaRepository.completarFinalizadas(EstadoReserva.CONFIRMADA,
                EstadoReserva.COMPLETADA, LocalDateTime.now(clock));
    }

    private void validarParticipante(Usuario usuario, Reserva reserva) {
        if (!usuario.getIdUsuario().equals(reserva.getCliente().getIdUsuario()) && !esProveedor(usuario, reserva)) {
            throw new BusinessException("RESERVA_NO_PROPIA", "No tienes permiso para consultar esta reserva.");
        }
    }

    private Reserva obtenerReservaPropiaProveedor(Usuario usuario, Long idReserva) {
        validarUsuarioActivo(usuario);
        if (usuario.getRol() != Rol.PROVEEDOR) {
            throw new BusinessException("ROL_NO_AUTORIZADO", "Solo el proveedor puede confirmar o rechazar reservas.");
        }
        Reserva reserva = obtenerParaActualizar(idReserva);
        if (!esProveedor(usuario, reserva)) {
            throw new BusinessException("RESERVA_NO_PROPIA", "No tienes permiso para gestionar esta reserva.");
        }
        return reserva;
    }

    private Reserva obtenerParaActualizar(Long idReserva) {
        return reservaRepository.findByIdParaActualizar(idReserva)
                .orElseThrow(() -> new BusinessException("RESERVA_NO_ENCONTRADA", "La reserva indicada no existe."));
    }

    private void validarUsuarioActivo(Usuario usuario) {
        if (usuario == null || usuario.getIdUsuario() == null) {
            throw new BusinessException("USUARIO_NO_AUTENTICADO", "Debes iniciar sesión para realizar esta operación.");
        }
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new BusinessException("CUENTA_NO_VERIFICADA", "Debes tener la cuenta activa para gestionar reservas.");
        }
    }

    private boolean esProveedor(Usuario usuario, Reserva reserva) {
        return usuario.getIdUsuario().equals(reserva.getServicio().getProveedor().getUsuario().getIdUsuario());
    }

    private void validarPendiente(Reserva reserva) {
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw estadoNoPermitido(reserva);
        }
    }

    private BusinessException estadoNoPermitido(Reserva reserva) {
        return new BusinessException("ESTADO_RESERVA_INVALIDO",
                "El estado actual " + reserva.getEstado() + " no permite esta operación.");
    }

    private void registrarTransicion(Reserva reserva, Usuario actor, EstadoReserva nuevo, String motivo) {
        if (motivo != null && motivo.length() > 255) {
            throw new BusinessException("MOTIVO_INVALIDO", "El motivo no puede superar 255 caracteres.");
        }
        HistorialReserva historial = new HistorialReserva();
        historial.setReserva(reserva);
        historial.setActor(actor);
        historial.setEstadoAnterior(reserva.getEstado());
        historial.setEstadoNuevo(nuevo);
        historial.setFechaCambio(LocalDateTime.now(clock));
        historial.setMotivo(motivo == null || motivo.isBlank() ? null : motivo.trim());
        reserva.setEstado(nuevo);
        reservaRepository.save(reserva);
        historialRepository.save(historial);
    }

    private ReservaResponse convertirRespuesta(Reserva reserva) {
        ReservaResponse response = new ReservaResponse();
        response.setIdReserva(reserva.getIdReserva());
        response.setIdServicio(reserva.getServicio().getIdServicio());
        response.setNombreServicio(reserva.getServicio().getNombre());
        response.setFechaInicio(reserva.getFechaInicio());
        response.setFechaFin(reserva.getFechaFin());
        response.setEstado(reserva.getEstado());
        response.setFechaCreacion(reserva.getFechaCreacion());
        return response;
    }
}
