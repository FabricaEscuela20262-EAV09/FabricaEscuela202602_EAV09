package com.reservar.service;

import com.reservar.dto.reserva.CrearReservaRequest;
import com.reservar.dto.reserva.ReservaResponse;
import com.reservar.entity.BloqueoDisponibilidad;
import com.reservar.entity.DisponibilidadServicio;
import com.reservar.entity.HistorialReserva;
import com.reservar.entity.OfertaServicio;
import com.reservar.entity.Reserva;
import com.reservar.entity.Usuario;
import com.reservar.entity.enums.EstadoReserva;
import com.reservar.entity.enums.EstadoUsuario;
import com.reservar.entity.enums.Rol;
import com.reservar.exception.BusinessException;
import com.reservar.repository.BloqueoDisponibilidadRepository;
import com.reservar.repository.DisponibilidadServicioRepository;
import com.reservar.repository.HistorialReservaRepository;
import com.reservar.repository.OfertaServicioRepository;
import com.reservar.repository.ReservaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
public class ReservaService {

    private static final String ACTIVO = "ACTIVO";

    private static final List<EstadoReserva> ESTADOS_BLOQUEANTES =
            List.of(
                    EstadoReserva.PENDIENTE,
                    EstadoReserva.CONFIRMADA
            );

    private static final Map<DayOfWeek, String> DIAS_SEMANA = Map.of(
            DayOfWeek.MONDAY, "LUNES",
            DayOfWeek.TUESDAY, "MARTES",
            DayOfWeek.WEDNESDAY, "MIERCOLES",
            DayOfWeek.THURSDAY, "JUEVES",
            DayOfWeek.FRIDAY, "VIERNES",
            DayOfWeek.SATURDAY, "SABADO",
            DayOfWeek.SUNDAY, "DOMINGO"
    );

    private final ReservaRepository reservaRepository;
    private final HistorialReservaRepository historialReservaRepository;
    private final OfertaServicioRepository ofertaServicioRepository;
    private final DisponibilidadServicioRepository disponibilidadRepository;
    private final BloqueoDisponibilidadRepository bloqueoRepository;

    public ReservaService(
            ReservaRepository reservaRepository,
            HistorialReservaRepository historialReservaRepository,
            OfertaServicioRepository ofertaServicioRepository,
            DisponibilidadServicioRepository disponibilidadRepository,
            BloqueoDisponibilidadRepository bloqueoRepository) {

        this.reservaRepository = reservaRepository;
        this.historialReservaRepository = historialReservaRepository;
        this.ofertaServicioRepository = ofertaServicioRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.bloqueoRepository = bloqueoRepository;
    }

    @Transactional
    public ReservaResponse crearReserva(
            Usuario usuario,
            CrearReservaRequest request) {

        validarCliente(usuario);

        OfertaServicio oferta = obtenerOfertaActiva(request.getIdServicio());

        validarQueNoSeaSuPropiaOferta(usuario, oferta);

        LocalDateTime fechaInicio = request.getFechaInicio();
        validarFechaFutura(fechaInicio);

        Integer duracion = oferta.getDuracionMinutos();

        if (duracion == null || duracion <= 0) {
            throw new BusinessException(
                    "DURACION_SERVICIO_INVALIDA",
                    "La duración configurada para el servicio no es válida."
            );
        }

        LocalDateTime fechaFin = fechaInicio.plusMinutes(duracion);

        // TEMPORAL: una reserva debe comenzar y terminar el mismo día.
        if (!fechaInicio.toLocalDate().equals(fechaFin.toLocalDate())) {
            throw new BusinessException(
                    "RESERVA_FUERA_DE_HORARIO",
                    "La reserva no puede extenderse hasta el día siguiente."
            );
        }

        validarDisponibilidad(oferta, fechaInicio, fechaFin);
        validarBloqueos(oferta, fechaInicio, fechaFin);
        validarReservasExistentes(oferta, fechaInicio, fechaFin);

        Reserva reserva = new Reserva();
        reserva.setCliente(usuario);
        reserva.setServicio(oferta);
        reserva.setFechaInicio(fechaInicio);
        reserva.setFechaFin(fechaFin);
        reserva.setEstado(EstadoReserva.PENDIENTE);

        try {
            // Aqui se fuerza la operación INSERT para detectar la restricción de PostgreSQL.
            reserva = reservaRepository.saveAndFlush(reserva);

            HistorialReserva historial = new HistorialReserva();
            historial.setReserva(reserva);
            historial.setActor(usuario);
            historial.setEstadoAnterior(null);
            historial.setEstadoNuevo(EstadoReserva.PENDIENTE);
            historial.setFechaCambio(LocalDateTime.now());
            historial.setMotivo("Creación de reserva");

            historialReservaRepository.save(historial);

        } catch (DataIntegrityViolationException ex) {
            if (esConflictoDeHorario(ex)) {
                throw new BusinessException(
                        "HORARIO_NO_DISPONIBLE",
                        "El horario acaba de ser reservado. "
                                + "Selecciona otro horario."
                );
            }
            throw ex;
        }

        return convertirRespuesta(reserva);
    }

    private void validarCliente(Usuario usuario) {

        if (usuario.getRol() != Rol.CLIENTE) {
            throw new BusinessException(
                    "ROL_NO_AUTORIZADO",
                    "Solo los clientes pueden crear reservas."
            );
        }

        if (usuario.getEstado() == EstadoUsuario.PENDIENTE) {
            throw new BusinessException(
                    "CUENTA_NO_VERIFICADA",
                    "Debes verificar tu correo antes de iniciar sesión"
            );
        }

        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new BusinessException(
                    "CUENTA_INACTIVA",
                    "Tu cuenta no se encuentra activa."
            );
        }
    }

    private OfertaServicio obtenerOfertaActiva(Long idServicio) {

        return ofertaServicioRepository
                .findByIdServicioAndEstado(idServicio, ACTIVO)
                .filter(oferta ->
                        ACTIVO.equals(oferta.getTipoServicio().getEstado())
                                && ACTIVO.equals(
                                oferta.getTipoServicio()
                                        .getCategoria()
                                        .getEstado()
                        )
                )
                .orElseThrow(() -> new BusinessException(
                        "OFERTA_NO_DISPONIBLE",
                        "La oferta no existe o no está activa."
                ));
    }

    private void validarQueNoSeaSuPropiaOferta(
            Usuario usuario,
            OfertaServicio oferta) {

        if (oferta.getProveedor().getUsuario()
                .getIdUsuario().equals(usuario.getIdUsuario())) {

            throw new BusinessException(
                    "AUTORRESERVA_NO_PERMITIDA",
                    "No puedes reservar un servicio que tú mismo ofreces."
            );
        }
    }

    private void validarFechaFutura(LocalDateTime fechaInicio) {

        if (fechaInicio == null || !fechaInicio.isAfter(LocalDateTime.now())) {
            throw new BusinessException(
                    "FECHA_RESERVA_INVALIDA",
                    "La fecha y hora de inicio deben ser futuras."
            );
        }
    }

    private void validarDisponibilidad(
            OfertaServicio oferta,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin) {

        String dia = DIAS_SEMANA.get(
                fechaInicio.getDayOfWeek()
        );

        LocalTime horaInicio = fechaInicio.toLocalTime();
        LocalTime horaFin = fechaFin.toLocalTime();

        List<DisponibilidadServicio> horarios =
                disponibilidadRepository
                        .findByServicioIdServicioAndDiaSemana(
                                oferta.getIdServicio(),
                                dia
                        );

        boolean dentroDeHorario = horarios.stream().anyMatch(
                horario ->
                        !horaInicio.isBefore(horario.getHoraInicio())
                                && !horaFin.isAfter(horario.getHoraFin())
        );

        if (!dentroDeHorario) {
            throw new BusinessException(
                    "FUERA_DE_DISPONIBILIDAD",
                    "El horario solicitado está fuera de la disponibilidad "
                            + "configurada para el servicio."
            );
        }
    }

    private void validarBloqueos(
            OfertaServicio oferta,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin) {

        List<BloqueoDisponibilidad> bloqueos =
                bloqueoRepository.findByServicioIdServicioAndFecha(
                        oferta.getIdServicio(),
                        fechaInicio.toLocalDate()
                );

        LocalTime horaInicio = fechaInicio.toLocalTime();
        LocalTime horaFin = fechaFin.toLocalTime();

        boolean bloqueado = bloqueos.stream().anyMatch(
                bloqueo ->
                        horaInicio.isBefore(bloqueo.getHoraFin())
                                && bloqueo.getHoraInicio().isBefore(horaFin)
        );

        if (bloqueado) {
            throw new BusinessException(
                    "HORARIO_BLOQUEADO",
                    "El horario solicitado coincide con un bloqueo "
                            + "de disponibilidad del proveedor."
            );
        }
    }

    private void validarReservasExistentes(
            OfertaServicio oferta,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin) {

        boolean existe =
                reservaRepository.existeReservaSuperpuesta(
                        oferta.getIdServicio(),
                        fechaInicio,
                        fechaFin,
                        ESTADOS_BLOQUEANTES
                );

        if (existe) {
            throw new BusinessException(
                    "HORARIO_NO_DISPONIBLE",
                    "Ya existe una reserva para el servicio "
                            + "en el horario solicitado."
            );
        }
    }

    private boolean esConflictoDeHorario(Throwable ex) {

        Throwable actual = ex;

        while (actual != null) {
            if (actual instanceof SQLException sqlException
                    && "23P01".equals(sqlException.getSQLState())) {
                return true;
            }

            actual = actual.getCause();
        }

        return false;
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
