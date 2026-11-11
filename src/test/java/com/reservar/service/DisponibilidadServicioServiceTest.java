
package com.reservar.service;

import com.reservar.dto.servicio.BloqueoDisponibilidadRequest;
import com.reservar.dto.servicio.DisponibilidadResponse;
import com.reservar.entity.BloqueoDisponibilidad;
import com.reservar.entity.CategoriaServicio;
import com.reservar.entity.OfertaServicio;
import com.reservar.entity.Proveedor;
import com.reservar.entity.TipoServicio;
import com.reservar.entity.Usuario;
import com.reservar.entity.enums.EstadoUsuario;
import com.reservar.entity.enums.Rol;
import com.reservar.exception.BusinessException;
import com.reservar.repository.BloqueoDisponibilidadRepository;
import com.reservar.repository.DisponibilidadServicioRepository;
import com.reservar.repository.OfertaServicioRepository;
import com.reservar.repository.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServicioServiceTest {

    private static final Long ID_SERVICIO = 10L;

    private static final ZoneId ZONA_COLOMBIA =
            ZoneId.of("America/Bogota");

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-06T01:00:00Z"),
            ZONA_COLOMBIA
    );

    @Mock
    private DisponibilidadServicioRepository disponibilidadRepository;

    @Mock
    private BloqueoDisponibilidadRepository bloqueoRepository;

    @Mock
    private OfertaServicioRepository ofertaRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private DisponibilidadServicioService service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(CLOCK.instant());
        lenient().when(clock.getZone()).thenReturn(CLOCK.getZone());

        usuario = new Usuario();
        usuario.setIdUsuario(UUID.randomUUID());
        usuario.setRol(Rol.PROVEEDOR);
        usuario.setEstado(EstadoUsuario.ACTIVO);

        Proveedor proveedor = new Proveedor();
        proveedor.setIdProveedor(3L);
        proveedor.setUsuario(usuario);

        CategoriaServicio categoria = new CategoriaServicio();
        categoria.setEstado("ACTIVO");

        TipoServicio tipo = new TipoServicio();
        tipo.setEstado("ACTIVO");
        tipo.setCategoria(categoria);

        OfertaServicio oferta = new OfertaServicio();
        oferta.setIdServicio(ID_SERVICIO);
        oferta.setProveedor(proveedor);
        oferta.setTipoServicio(tipo);
        oferta.setEstado("ACTIVO");

        when(proveedorRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()))
                .thenReturn(Optional.of(proveedor));

        when(ofertaRepository.findByIdServicioAndEstado(ID_SERVICIO, "ACTIVO"))
                .thenReturn(Optional.of(oferta));
    }

    private BloqueoDisponibilidadRequest solicitud(
            LocalTime inicio,
            LocalTime fin,
            String motivo) {

        return solicitud(
                LocalDate.now(CLOCK).plusDays(3),
                inicio,
                fin,
                motivo
        );
    }

    private BloqueoDisponibilidadRequest solicitud(
            LocalDate fecha,
            LocalTime inicio,
            LocalTime fin,
            String motivo) {

        BloqueoDisponibilidadRequest request =
                new BloqueoDisponibilidadRequest();

        request.setFecha(fecha);
        request.setHoraInicio(inicio);
        request.setHoraFin(fin);
        request.setMotivo(motivo);

        return request;
    }

    @Test
    void deberiaBloquearElDiaCompletoCuandoNoSeIndicanHoras() {
        when(bloqueoRepository.findByServicioIdServicioAndFecha(any(), any()))
                .thenReturn(List.of());

        when(bloqueoRepository.save(any(BloqueoDisponibilidad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        DisponibilidadResponse respuesta = service.crearBloqueo(
                usuario, ID_SERVICIO, solicitud(null, null, null)
        );

        ArgumentCaptor<BloqueoDisponibilidad> captor =
                ArgumentCaptor.forClass(BloqueoDisponibilidad.class);

        verify(bloqueoRepository).save(captor.capture());

        BloqueoDisponibilidad guardado = captor.getValue();

        assertAll(
                () -> assertEquals(LocalTime.MIN, guardado.getHoraInicio()),
                () -> assertEquals(LocalTime.of(23, 59, 59), guardado.getHoraFin()),
                () -> assertEquals("OTRO", guardado.getMotivo()),
                () -> assertTrue(respuesta.getDiaCompleto())
        );
    }

    @Test
    void deberiaConservarElRangoCuandoSeIndicanLasHoras() {
        when(bloqueoRepository.findByServicioIdServicioAndFecha(any(), any()))
                .thenReturn(List.of());

        when(bloqueoRepository.save(any(BloqueoDisponibilidad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        DisponibilidadResponse respuesta = service.crearBloqueo(
                usuario,
                ID_SERVICIO,
                solicitud(LocalTime.of(9, 0), LocalTime.of(11, 0), "otro")
        );

        assertAll(
                () -> assertEquals(LocalTime.of(9, 0), respuesta.getHoraInicio()),
                () -> assertEquals(LocalTime.of(11, 0), respuesta.getHoraFin()),
                () -> assertEquals("OTRO", respuesta.getMotivo()),
                () -> assertEquals(Boolean.FALSE, respuesta.getDiaCompleto())
        );
    }

    @Test
    void deberiaRechazarBloqueoConSoloUnaDeLasHoras() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.crearBloqueo(
                        usuario,
                        ID_SERVICIO,
                        solicitud(LocalTime.of(9, 0), null, null)
                )
        );

        assertEquals("INTERVALO_HORARIO_INVALIDO", ex.getCodigo());
        verify(bloqueoRepository, never()).save(any());
    }

    @Test
    void deberiaRechazarBloqueoDeDiaCompletoSiYaExisteUnBloqueoParcialEnLaFecha() {
        BloqueoDisponibilidad existente = new BloqueoDisponibilidad();
        existente.setHoraInicio(LocalTime.of(9, 0));
        existente.setHoraFin(LocalTime.of(10, 0));

        when(bloqueoRepository.findByServicioIdServicioAndFecha(any(), any()))
                .thenReturn(List.of(existente));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.crearBloqueo(
                        usuario,
                        ID_SERVICIO,
                        solicitud(null, null, null)
                )
        );

        assertEquals("BLOQUEO_TRASLAPADO", ex.getCodigo());
        verify(bloqueoRepository, never()).save(any());
    }

    @Test
    void deberiaPermitirBloqueoParaHoyAunqueUtcEsteEnElDiaSiguiente() {
        LocalDate hoyColombia = LocalDate.now(CLOCK);

        when(bloqueoRepository.findByServicioIdServicioAndFecha(any(), any()))
                .thenReturn(List.of());

        when(bloqueoRepository.save(any(BloqueoDisponibilidad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        DisponibilidadResponse respuesta = service.crearBloqueo(
                usuario,
                ID_SERVICIO,
                solicitud(hoyColombia, null, null, null)
        );

        ArgumentCaptor<BloqueoDisponibilidad> captor =
                ArgumentCaptor.forClass(BloqueoDisponibilidad.class);

        verify(bloqueoRepository).save(captor.capture());

        assertAll(
                () -> assertEquals(hoyColombia, captor.getValue().getFecha()),
                () -> assertEquals(LocalTime.MIN, respuesta.getHoraInicio()),
                () -> assertTrue(respuesta.getDiaCompleto())
        );
    }

    @Test
    void deberiaRechazarBloqueoParaUnaFechaPasadaEnColombia() {
        LocalDate ayerColombia = LocalDate.now(CLOCK).minusDays(1);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.crearBloqueo(
                        usuario,
                        ID_SERVICIO,
                        solicitud(ayerColombia, null, null, null)
                )
        );

        assertEquals("FECHA_BLOQUEO_INVALIDA", ex.getCodigo());
        verify(bloqueoRepository, never()).save(any());
    }
}
