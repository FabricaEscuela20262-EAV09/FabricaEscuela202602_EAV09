package com.reservar.service;

import com.reservar.dto.servicio.BloqueoDisponibilidadRequest;
import com.reservar.dto.servicio.DisponibilidadRequest;
import com.reservar.dto.servicio.DisponibilidadResponse;
import com.reservar.entity.BloqueoDisponibilidad;
import com.reservar.entity.DisponibilidadServicio;
import com.reservar.entity.OfertaServicio;
import com.reservar.entity.Proveedor;
import com.reservar.entity.Usuario;
import com.reservar.exception.BusinessException;
import com.reservar.repository.BloqueoDisponibilidadRepository;
import com.reservar.repository.DisponibilidadServicioRepository;
import com.reservar.repository.OfertaServicioRepository;
import com.reservar.repository.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DisponibilidadServicioService {
    private static final String ACTIVO = "ACTIVO";
    private static final Set<String> DIAS_VALIDOS = Set.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO");
    private static final Set<String> MOTIVOS_VALIDOS = Set.of("RESERVADO", "OTRO");
    private final DisponibilidadServicioRepository disponibilidadRepository;
    private final BloqueoDisponibilidadRepository bloqueoRepository;
    private final OfertaServicioRepository ofertaRepository;
    private final ProveedorRepository proveedorRepository;

    public DisponibilidadServicioService(
            DisponibilidadServicioRepository disponibilidadRepository,
            BloqueoDisponibilidadRepository bloqueoRepository,
            OfertaServicioRepository ofertaRepository,
            ProveedorRepository proveedorRepository) {
        this.disponibilidadRepository = disponibilidadRepository;
        this.bloqueoRepository = bloqueoRepository;
        this.ofertaRepository = ofertaRepository;
        this.proveedorRepository = proveedorRepository;
    }

    // AÑADIR DISPONIBILIDAD PARA UNA OFERTA DE SERVICIO
    @Transactional
    public DisponibilidadResponse agregarDisponibilidad(
            Usuario usuario,
            Long idServicio,
            DisponibilidadRequest request) {

        validarProveedorActivo(usuario);
        OfertaServicio oferta = obtenerOfertaPropiaActiva(usuario, idServicio);

        String dia = normalizarDia(request.getDiaSemana());
        validarIntervalo(request.getHoraInicio(), request.getHoraFin());

        List<DisponibilidadServicio> existentes =
                disponibilidadRepository
                        .findByServicioIdServicioAndDiaSemana(idServicio, dia);

        for (DisponibilidadServicio existente : existentes) {
            if (seTraslapan(
                    request.getHoraInicio(),
                    request.getHoraFin(),
                    existente.getHoraInicio(),
                    existente.getHoraFin())) {
                throw new BusinessException(
                        "DISPONIBILIDAD_TRASLAPADA",
                        "El intervalo se traslapa con otro horario del mismo día."
                );
            }
        }

        DisponibilidadServicio disponibilidad = new DisponibilidadServicio();
        disponibilidad.setServicio(oferta);
        disponibilidad.setDiaSemana(dia);
        disponibilidad.setHoraInicio(request.getHoraInicio());
        disponibilidad.setHoraFin(request.getHoraFin());
        return convertirDisponibilidad(
                disponibilidadRepository.save(disponibilidad));
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadResponse> listarDisponibilidad(Long idServicio) {obtenerOfertaActiva(idServicio);
        return disponibilidadRepository
                .findByServicioIdServicio(idServicio)
                .stream()
                .map(this::convertirDisponibilidad)
                .toList();
    }

    // ELIMINAR DISPONIBILIDAD DE UNA OFERTA
    @Transactional
    public void eliminarDisponibilidad(
            Usuario usuario,
            Long idServicio,
            Long idDisponibilidad) {

        validarProveedorActivo(usuario);
        obtenerOfertaPropiaActiva(usuario, idServicio);
        DisponibilidadServicio disponibilidad =
                disponibilidadRepository.findById(idDisponibilidad)
                        .orElseThrow(() -> new BusinessException(
                                "DISPONIBILIDAD_NO_ENCONTRADA",
                                "El intervalo de disponibilidad no existe."
                        ));
        if (!disponibilidad.getServicio().getIdServicio()
                .equals(idServicio)) {
            throw new BusinessException(
                    "DISPONIBILIDAD_NO_PERTENECE",
                    "El intervalo no pertenece a la oferta indicada."
            );
        }

        disponibilidadRepository.delete(disponibilidad);
    }

    @Transactional
    public DisponibilidadResponse crearBloqueo(
            Usuario usuario,
            Long idServicio,
            BloqueoDisponibilidadRequest request) {

        validarProveedorActivo(usuario);
        OfertaServicio oferta = obtenerOfertaPropiaActiva(usuario, idServicio);
        validarIntervalo(request.getHoraInicio(), request.getHoraFin());
        if (request.getFecha().isBefore(LocalDate.now())) {
            throw new BusinessException(
                    "FECHA_BLOQUEO_INVALIDA",
                    "No se puede crear un bloqueo para una fecha pasada."
            );
        }
        String motivo = request.getMotivo() == null
                ? "OTRO"
                : request.getMotivo().trim().toUpperCase(Locale.ROOT);
        if (!MOTIVOS_VALIDOS.contains(motivo)) {
            throw new BusinessException(
                    "MOTIVO_BLOQUEO_INVALIDO",
                    "El motivo debe ser RESERVADO u OTRO."
            );
        }
        List<BloqueoDisponibilidad> existentes =
                bloqueoRepository.findByServicioIdServicioAndFecha(
                        idServicio, request.getFecha());
        for (BloqueoDisponibilidad existente : existentes) {
            if (seTraslapan(
                    request.getHoraInicio(),
                    request.getHoraFin(),
                    existente.getHoraInicio(),
                    existente.getHoraFin())) {
                throw new BusinessException(
                        "BLOQUEO_TRASLAPADO",
                        "El bloqueo se traslapa con otro bloqueo de esa fecha."
                );
            }
        }
        BloqueoDisponibilidad bloqueo = new BloqueoDisponibilidad();
        bloqueo.setServicio(oferta);
        bloqueo.setFecha(request.getFecha());
        bloqueo.setHoraInicio(request.getHoraInicio());
        bloqueo.setHoraFin(request.getHoraFin());
        bloqueo.setMotivo(motivo);
        return convertirBloqueo(bloqueoRepository.save(bloqueo));
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadResponse> listarBloqueos(
            Long idServicio,
            LocalDate fechaInicio,
            LocalDate fechaFin) {
        obtenerOfertaActiva(idServicio);
        if (fechaInicio != null && fechaFin != null
                && fechaFin.isBefore(fechaInicio)) {
            throw new BusinessException(
                    "RANGO_FECHAS_INVALIDO",
                    "La fecha final no puede ser anterior a la inicial."
            );
        }

        List<BloqueoDisponibilidad> bloqueos;
        if (fechaInicio != null && fechaFin != null) {
            bloqueos = bloqueoRepository
                    .findByServicioIdServicioAndFechaBetween(
                            idServicio, fechaInicio, fechaFin);
        } else if (fechaInicio != null) {
            bloqueos = bloqueoRepository
                    .findByServicioIdServicioAndFechaBetween(
                            idServicio, fechaInicio, fechaInicio);
        } else if (fechaFin != null) {
            bloqueos = bloqueoRepository
                    .findByServicioIdServicioAndFechaBetween(
                            idServicio, fechaFin, fechaFin);
        } else {
            bloqueos = bloqueoRepository
                    .findByServicioIdServicio(idServicio);
        }

        return bloqueos.stream()
                .map(this::convertirBloqueo)
                .toList();
    }

    // ELIMINAR UN BLOQUEO
    @Transactional
    public void eliminarBloqueo(
            Usuario usuario,
            Long idServicio,
            Long idBloqueo) {

        validarProveedorActivo(usuario);
        obtenerOfertaPropiaActiva(usuario, idServicio);

        BloqueoDisponibilidad bloqueo =
                bloqueoRepository.findById(idBloqueo)
                        .orElseThrow(() -> new BusinessException(
                                "BLOQUEO_NO_ENCONTRADO",
                                "El bloqueo indicado no existe."
                        ));

        if (!bloqueo.getServicio().getIdServicio().equals(idServicio)) {
            throw new BusinessException(
                    "BLOQUEO_NO_PERTENECE",
                    "El bloqueo no pertenece a la oferta indicada."
            );
        }

        bloqueoRepository.delete(bloqueo);
    }

    // VALIDACIÓN DE USUARIO CON ROL DE PROVEEDOR
    private void validarProveedorActivo(Usuario usuario) {
        if (usuario == null
                || usuario.getRol() == null
                || !"PROVEEDOR".equals(usuario.getRol().name())) {
            throw new BusinessException(
                    "ROL_NO_AUTORIZADO",
                    "Solo los proveedores pueden gestionar disponibilidad."
            );
        }

        if (usuario.getEstado() == null
                || !"ACTIVO".equals(usuario.getEstado().name())) {
            throw new BusinessException(
                    "PROVEEDOR_NO_ACTIVO",
                    "La cuenta del proveedor debe estar activa."
            );
        }
    }

    // EXPLORAR OFERTAS PROPIAS
    private OfertaServicio obtenerOfertaPropiaActiva(
            Usuario usuario, Long idServicio) {

        Proveedor proveedor = proveedorRepository
                .findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new BusinessException(
                        "PROVEEDOR_NO_ENCONTRADO",
                        "No existe un perfil de proveedor asociado."
                ));

        OfertaServicio oferta = obtenerOfertaActiva(idServicio);

        if (!oferta.getProveedor().getIdProveedor()
                .equals(proveedor.getIdProveedor())) {
            throw new BusinessException(
                    "OFERTA_NO_PROPIA",
                    "No tienes permiso para modificar esta oferta."
            );
        }

        return oferta;
    }

    private OfertaServicio obtenerOfertaActiva(Long idServicio) {
        return ofertaRepository.findByIdServicioAndEstado(idServicio, ACTIVO)
                .filter(oferta ->
                        ACTIVO.equals(oferta.getTipoServicio().getEstado())
                                && ACTIVO.equals(
                                oferta.getTipoServicio().getCategoria().getEstado()))
                .orElseThrow(() -> new BusinessException(
                        "OFERTA_NO_DISPONIBLE",
                        "La oferta no existe o no está activa."
                ));
    }

    private String normalizarDia(String diaSemana) {
        String dia = diaSemana == null
                ? ""
                : diaSemana.trim().toUpperCase(Locale.ROOT);

        if (!DIAS_VALIDOS.contains(dia)) {
            throw new BusinessException(
                    "DIA_SEMANA_INVALIDO",
                    "El día debe ser LUNES, MARTES, MIERCOLES, JUEVES, "
                            + "VIERNES, SABADO o DOMINGO."
            );
        }

        return dia;
    }

    private void validarIntervalo(LocalTime inicio, LocalTime fin) {
        if (inicio == null || fin == null || !inicio.isBefore(fin)) {
            throw new BusinessException(
                    "INTERVALO_HORARIO_INVALIDO",
                    "La hora de inicio debe ser anterior a la hora de fin."
            );
        }
    }

    private boolean seTraslapan(
            LocalTime inicio1,
            LocalTime fin1,
            LocalTime inicio2,
            LocalTime fin2) {

        return inicio1.isBefore(fin2) && inicio2.isBefore(fin1);
    }

    private DisponibilidadResponse convertirDisponibilidad(
            DisponibilidadServicio disponibilidad) {

        DisponibilidadResponse response = new DisponibilidadResponse();
        response.setId(disponibilidad.getIdDisponibilidad());
        response.setIdServicio(
                disponibilidad.getServicio().getIdServicio());
        response.setTipo("SEMANAL");
        response.setDiaSemana(disponibilidad.getDiaSemana());
        response.setHoraInicio(disponibilidad.getHoraInicio());
        response.setHoraFin(disponibilidad.getHoraFin());

        return response;
    }

    private DisponibilidadResponse convertirBloqueo(
            BloqueoDisponibilidad bloqueo) {

        DisponibilidadResponse response = new DisponibilidadResponse();
        response.setId(bloqueo.getIdBloqueo());
        response.setIdServicio(bloqueo.getServicio().getIdServicio());
        response.setTipo("BLOQUEO");
        response.setFecha(bloqueo.getFecha());
        response.setMotivo(bloqueo.getMotivo());
        response.setHoraInicio(bloqueo.getHoraInicio());
        response.setHoraFin(bloqueo.getHoraFin());

        return response;
    }
}

