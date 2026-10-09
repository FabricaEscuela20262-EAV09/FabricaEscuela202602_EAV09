package com.reservar.service;

import com.reservar.dto.servicio.ActualizarOfertaRequest;
import com.reservar.dto.servicio.CrearOfertaRequest;
import com.reservar.dto.servicio.OfertaServicioResponse;
import com.reservar.dto.servicio.DisponibilidadResponse;
import com.reservar.entity.BloqueoDisponibilidad;
import com.reservar.entity.DisponibilidadServicio;
import com.reservar.entity.CategoriaServicio;
import com.reservar.entity.OfertaServicio;
import com.reservar.entity.Proveedor;
import com.reservar.entity.TipoServicio;
import com.reservar.entity.Usuario;
import com.reservar.exception.BusinessException;
import com.reservar.repository.CategoriaServicioRepository;
import com.reservar.repository.OfertaServicioRepository;
import com.reservar.repository.ProveedorRepository;
import com.reservar.repository.TipoServicioRepository;
import com.reservar.repository.BloqueoDisponibilidadRepository;
import com.reservar.repository.DisponibilidadServicioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class OfertaServicioService {
    private static final String ACTIVO = "ACTIVO";
    private static final String INACTIVO = "INACTIVO";
    private static final String PRESENCIAL = "PRESENCIAL";
    private static final String REMOTO = "REMOTO";
    private final OfertaServicioRepository ofertaRepository;
    private final ProveedorRepository proveedorRepository;
    private final TipoServicioRepository tipoServicioRepository;
    private final CategoriaServicioRepository categoriaRepository;
    private final DisponibilidadServicioRepository disponibilidadRepository;
    private final BloqueoDisponibilidadRepository bloqueoRepository;

    public OfertaServicioService(
            OfertaServicioRepository ofertaRepository,
            ProveedorRepository proveedorRepository,
            TipoServicioRepository tipoServicioRepository,
            CategoriaServicioRepository categoriaRepository,
            DisponibilidadServicioRepository disponibilidadRepository,
            BloqueoDisponibilidadRepository bloqueoRepository) {

        this.ofertaRepository = ofertaRepository;
        this.proveedorRepository = proveedorRepository;
        this.tipoServicioRepository = tipoServicioRepository;
        this.categoriaRepository = categoriaRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.bloqueoRepository = bloqueoRepository;
    }

    // CREAR NUEVA OFERTA DE SERVICIO
    @Transactional
    public OfertaServicioResponse crearOferta(Usuario usuarioAutenticado, CrearOfertaRequest request) {

        validarProveedorActivo(usuarioAutenticado);
        Proveedor proveedor = obtenerProveedor(usuarioAutenticado);
        TipoServicio tipoServicio = obtenerTipoServicioActivo(request.getIdTipoServicio());

        OfertaServicio oferta = new OfertaServicio();
        oferta.setProveedor(proveedor);
        oferta.setTipoServicio(tipoServicio);

        aplicarDatos(
                oferta,
                request.getNombre(),
                request.getDescripcion(),
                request.getDuracionMinutos(),
                request.getModalidad(),
                request.getUbicacion()
        );

        oferta.setEstado(ACTIVO);

        OfertaServicio guardada = ofertaRepository.save(oferta);
        return convertirRespuesta(guardada);
    }

    @Transactional
    public OfertaServicioResponse actualizarOferta(Usuario usuarioAutenticado, Long idServicio, ActualizarOfertaRequest request) {

        validarProveedorActivo(usuarioAutenticado);
        Proveedor proveedor = obtenerProveedor(usuarioAutenticado);

        OfertaServicio oferta = obtenerOfertaPropia(idServicio, proveedor);
        TipoServicio tipoServicio = obtenerTipoServicioActivo(request.getIdTipoServicio());
        oferta.setTipoServicio(tipoServicio);

        aplicarDatos(
                oferta,
                request.getNombre(),
                request.getDescripcion(),
                request.getDuracionMinutos(),
                request.getModalidad(),
                request.getUbicacion()
        );

        return convertirRespuesta(ofertaRepository.save(oferta));
    }

    // CAMBIAR EL ESTADO DE UNA OFERTA A INACTIVA
    @Transactional
    public void desactivarOferta(Usuario usuarioAutenticado, Long idServicio) {

        validarProveedorActivo(usuarioAutenticado);
        Proveedor proveedor = obtenerProveedor(usuarioAutenticado);

        OfertaServicio oferta = obtenerOfertaPropia(idServicio, proveedor);
        oferta.setEstado(INACTIVO);
        ofertaRepository.save(oferta);
    }

    // CONSULTAR DETALLES DE OFERTA
    @Transactional(readOnly = true)
    public OfertaServicioResponse obtenerDetalle(Long idServicio) {
        OfertaServicio oferta = ofertaRepository
                .findByIdServicioAndEstado(idServicio, ACTIVO)
                .orElseThrow(() -> new BusinessException(
                        "OFERTA_NO_ENCONTRADA",
                        "La oferta no existe o no está activa."
                ));
        validarCatalogoActivo(oferta);
        OfertaServicioResponse response = convertirRespuesta(oferta);
        response.setProximasDisponibilidades(
                calcularProximasDisponibilidades(oferta)
        );
        return response;
    }

    @Transactional(readOnly = true)
    public Page<OfertaServicioResponse> buscarOfertas(String texto, Long idCategoria, Pageable pageable) {

        String textoNormalizado = (texto == null || texto.isBlank())
                ? ""
                : texto.trim().toLowerCase(Locale.ROOT);

        // Validar que la categoría exista y esté activa.
        if (idCategoria != null) {
            CategoriaServicio categoria = categoriaRepository
                    .findById(idCategoria)
                    .orElseThrow(() -> new BusinessException(
                            "CATEGORIA_NO_ENCONTRADA",
                            "La categoría indicada no existe."
                    ));

            if (!ACTIVO.equals(categoria.getEstado())) {
                throw new BusinessException(
                        "CATEGORIA_INACTIVA",
                        "La categoría indicada no está activa."
                );
            }
        }

        return ofertaRepository
                .buscarOfertasActivas(
                        textoNormalizado,
                        idCategoria,
                        pageable
                )
                .map(this::convertirRespuesta);
    }

    @Transactional(readOnly = true)
    public Page<OfertaServicioResponse> listarMisOfertas(Usuario usuarioAutenticado, Pageable pageable) {

        validarProveedorActivo(usuarioAutenticado);
        Proveedor proveedor = obtenerProveedor(usuarioAutenticado);

        return ofertaRepository
                .findByProveedorIdProveedorAndEstado(
                        proveedor.getIdProveedor(),
                        ACTIVO,
                        pageable
                )
                .map(this::convertirRespuesta);
    }

    @Transactional(readOnly = true)
    public Page<OfertaServicioResponse> listarHistorialOfertas(Usuario usuarioAutenticado, Pageable pageable) {
        validarProveedorActivo(usuarioAutenticado);
        Proveedor proveedor = obtenerProveedor(usuarioAutenticado);

        return ofertaRepository
                .findByProveedorIdProveedor(
                        proveedor.getIdProveedor(),
                        pageable
                )
                .map(this::convertirRespuesta);
    }

    private void validarProveedorActivo(Usuario usuario) {
        if (usuario == null) {
            throw new BusinessException(
                    "USUARIO_NO_AUTENTICADO",
                    "Debes iniciar sesión para realizar esta operación."
            );
        }

        if (usuario.getRol() == null
                || !"PROVEEDOR".equals(usuario.getRol().name())) {
            throw new BusinessException(
                    "ROL_NO_AUTORIZADO",
                    "Solo los proveedores pueden gestionar ofertas."
            );
        }

        if (usuario.getEstado() == null
                || !"ACTIVO".equals(usuario.getEstado().name())) {
            throw new BusinessException(
                    "PROVEEDOR_NO_ACTIVO",
                    "Debes tener la cuenta activa para gestionar ofertas."
            );
        }
    }

    private Proveedor obtenerProveedor(Usuario usuario) {
        return proveedorRepository
                .findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new BusinessException(
                        "PROVEEDOR_NO_ENCONTRADO",
                        "No existe un perfil de proveedor asociado al usuario."
                ));
    }

    private TipoServicio obtenerTipoServicioActivo(Long idTipoServicio) {
        TipoServicio tipo = tipoServicioRepository
                .findById(idTipoServicio)
                .orElseThrow(() -> new BusinessException(
                        "TIPO_SERVICIO_NO_ENCONTRADO",
                        "El tipo de servicio indicado no existe."
                ));

        if (!ACTIVO.equals(tipo.getEstado())) {
            throw new BusinessException(
                    "TIPO_SERVICIO_INACTIVO",
                    "El tipo de servicio seleccionado no está activo."
            );
        }

        if (tipo.getCategoria() == null
                || !ACTIVO.equals(tipo.getCategoria().getEstado())) {
            throw new BusinessException(
                    "CATEGORIA_INACTIVA",
                    "La categoría del tipo de servicio no está activa."
            );
        }

        return tipo;
    }

    private OfertaServicio obtenerOfertaPropia(Long idServicio, Proveedor proveedor) {

        OfertaServicio oferta = ofertaRepository
                .findById(idServicio)
                .orElseThrow(() -> new BusinessException(
                        "OFERTA_NO_ENCONTRADA",
                        "La oferta indicada no existe."
                ));

        if (!oferta.getProveedor().getIdProveedor()
                .equals(proveedor.getIdProveedor())) {
            throw new BusinessException(
                    "OFERTA_NO_PROPIA",
                    "No tienes permiso para modificar esta oferta."
            );
        }

        return oferta;
    }

    private void aplicarDatos(
            OfertaServicio oferta,
            String nombre,
            String descripcion,
            Integer duracionMinutos,
            String modalidad,
            String ubicacion) {

        String modalidadNormalizada = modalidad == null
                ? ""
                : modalidad.trim().toUpperCase(Locale.ROOT);

        if (!PRESENCIAL.equals(modalidadNormalizada)
                && !REMOTO.equals(modalidadNormalizada)) {
            throw new BusinessException(
                    "MODALIDAD_INVALIDA",
                    "La modalidad debe ser PRESENCIAL o REMOTO."
            );
        }

        String ubicacionNormalizada =
                ubicacion == null || ubicacion.isBlank()
                        ? null
                        : ubicacion.trim();

        if (PRESENCIAL.equals(modalidadNormalizada)
                && ubicacionNormalizada == null) {
            throw new BusinessException(
                    "UBICACION_REQUERIDA",
                    "La ubicación es obligatoria para servicios presenciales."
            );
        }

        if (nombre == null || nombre.isBlank()) {
            throw new BusinessException(
                    "NOMBRE_REQUERIDO",
                    "El nombre de la oferta es obligatorio."
            );
        }

        if (descripcion == null || descripcion.isBlank()) {
            throw new BusinessException(
                    "DESCRIPCION_REQUERIDA",
                    "La descripción de la oferta es obligatoria."
            );
        }

        if (duracionMinutos == null || duracionMinutos <= 0) {
            throw new BusinessException(
                    "DURACION_INVALIDA",
                    "La duración debe ser mayor que cero."
            );
        }

        oferta.setNombre(nombre.trim());
        oferta.setDescripcion(descripcion.trim());
        oferta.setDuracionMinutos(duracionMinutos);
        oferta.setModalidad(modalidadNormalizada);
        oferta.setUbicacion(ubicacionNormalizada);
    }

    private void validarCatalogoActivo(OfertaServicio oferta) {
        TipoServicio tipo = oferta.getTipoServicio();

        if (tipo == null
                || !ACTIVO.equals(tipo.getEstado())
                || tipo.getCategoria() == null
                || !ACTIVO.equals(tipo.getCategoria().getEstado())) {
            throw new BusinessException(
                    "OFERTA_NO_DISPONIBLE",
                    "La oferta no está disponible actualmente."
            );
        }
    }

    private OfertaServicioResponse convertirRespuesta(
            OfertaServicio oferta) {

        OfertaServicioResponse response = new OfertaServicioResponse();

        response.setIdServicio(oferta.getIdServicio());
        response.setIdProveedor(oferta.getProveedor().getIdProveedor());
        response.setIdTipoServicio(oferta.getTipoServicio().getIdTipoServicio());
        response.setIdCategoria(oferta.getTipoServicio().getCategoria().getIdCategoria());

        response.setNombreProveedor(oferta.getProveedor().getUsuario().getNombreCompleto());
        response.setNombreTipoServicio(oferta.getTipoServicio().getNombre());
        response.setNombreCategoria(oferta.getTipoServicio().getCategoria().getNombre());

        response.setNombre(oferta.getNombre());
        response.setDescripcion(oferta.getDescripcion());
        response.setDuracionMinutos(oferta.getDuracionMinutos());
        response.setModalidad(oferta.getModalidad());
        response.setUbicacion(oferta.getUbicacion());
        response.setEstado(oferta.getEstado());
        response.setFechaCreacion(oferta.getFechaCreacion());
        response.setFechaActualizacion(oferta.getFechaActualizacion());

        return response;
    }

    private List<DisponibilidadResponse> calcularProximasDisponibilidades(
            OfertaServicio oferta) {

        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(6);
        Long idServicio = oferta.getIdServicio();
        List<DisponibilidadServicio> horarios = disponibilidadRepository.findByServicioIdServicio(idServicio);
        List<BloqueoDisponibilidad> bloqueos = bloqueoRepository.findByServicioIdServicioAndFechaBetween(
                        idServicio, hoy, limite);
        List<DisponibilidadResponse> resultado = new ArrayList<>();
        for (LocalDate fecha = hoy;
             !fecha.isAfter(limite);
             fecha = fecha.plusDays(1)) {

            String dia = convertirDiaSemana(fecha.getDayOfWeek());
            final LocalDate fechaActual = fecha;

            List<BloqueoDisponibilidad> bloqueosDelDia = bloqueos.stream()
                    .filter(b -> b.getFecha().equals(fechaActual))
                    .toList();

            for (DisponibilidadServicio horario : horarios) {
                if (!dia.equalsIgnoreCase(horario.getDiaSemana())) {
                    continue;
                }

                LocalTime inicio = horario.getHoraInicio();
                LocalTime fin = horario.getHoraFin();

                // No ofrecer horas que ya pasaron durante el día actual.
                if (fecha.equals(hoy)) {
                    LocalTime ahora = LocalTime.now()
                            .plusMinutes(1)
                            .withSecond(0)
                            .withNano(0);

                    if (ahora.isAfter(inicio)) {
                        inicio = ahora;
                    }
                }

                if (!inicio.isBefore(fin)) {
                    continue;
                }

                List<FranjaHoraria> franjas = new ArrayList<>();
                franjas.add(new FranjaHoraria(inicio, fin));

                for (BloqueoDisponibilidad bloqueo : bloqueosDelDia) {
                    franjas = descontarBloqueo(
                            franjas,
                            bloqueo.getHoraInicio(),
                            bloqueo.getHoraFin()
                    );
                }

                for (FranjaHoraria franja : franjas) {
                    DisponibilidadResponse response =
                            new DisponibilidadResponse();

                    response.setIdServicio(idServicio);
                    response.setTipo("DISPONIBLE");
                    response.setDiaSemana(dia);
                    response.setFecha(fecha);
                    response.setHoraInicio(franja.inicio());
                    response.setHoraFin(franja.fin());

                    resultado.add(response);
                }
            }
        }

        resultado.sort(
                Comparator.comparing(DisponibilidadResponse::getFecha)
                        .thenComparing(
                                DisponibilidadResponse::getHoraInicio)
        );

        return resultado;
    }

    private List<FranjaHoraria> descontarBloqueo(
            List<FranjaHoraria> franjas,
            LocalTime inicioBloqueo,
            LocalTime finBloqueo) {

        List<FranjaHoraria> resultado = new ArrayList<>();

        for (FranjaHoraria franja : franjas) {
            LocalTime inicio = franja.inicio();
            LocalTime fin = franja.fin();

            // El bloqueo no se cruza con esta franja.
            if (!inicioBloqueo.isBefore(fin)
                    || !finBloqueo.isAfter(inicio)) {
                resultado.add(franja);
                continue;
            }

            // Conservar la parte anterior al bloqueo.
            if (inicio.isBefore(inicioBloqueo)) {
                resultado.add(
                        new FranjaHoraria(inicio, inicioBloqueo)
                );
            }

            // Conservar la parte posterior al bloqueo.
            if (finBloqueo.isBefore(fin)) {
                resultado.add(
                        new FranjaHoraria(finBloqueo, fin)
                );
            }
        }

        return resultado;
    }

    private String convertirDiaSemana(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> "LUNES";
            case TUESDAY -> "MARTES";
            case WEDNESDAY -> "MIERCOLES";
            case THURSDAY -> "JUEVES";
            case FRIDAY -> "VIERNES";
            case SATURDAY -> "SABADO";
            case SUNDAY -> "DOMINGO";
        };
    }

    private record FranjaHoraria(
            LocalTime inicio,
            LocalTime fin) {
    }
}

