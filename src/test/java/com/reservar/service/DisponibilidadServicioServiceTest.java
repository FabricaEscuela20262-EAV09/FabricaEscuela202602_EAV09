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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServicioServiceTest {

    private static final Long ID_SERVICIO = 10L;

    @Mock
    private DisponibilidadServicioRepository disponibilidadRepository;
    @Mock
    private BloqueoDisponibilidadRepository bloqueoRepository;
    @Mock
    private OfertaServicioRepository ofertaRepository;
    @Mock
    private ProveedorRepository proveedorRepository;

    @InjectMocks
    private DisponibilidadServicioService service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
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

    private BloqueoDisponibilidadRequest solicitud(LocalTime inicio, LocalTime fin, String motivo) {
        BloqueoDisponibilidadRequest request = new BloqueoDisponibilidadRequest();
        request.setFecha(LocalDate.now().plusDays(3));
        request.setHoraInicio(inicio);
        request.setHoraFin(fin);
        request.setMotivo(motivo);
        return request;
    }

    @Test
    void deberiaBloquearElDiaCompletoCuandoNoSeIndicanHoras() {
        // Arrange
        when(bloqueoRepository.findByServicioIdServicioAndFecha(any(), any())).thenReturn(List.of());
        when(bloqueoRepository.save(any(BloqueoDisponibilidad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act
        DisponibilidadResponse respuesta = service.crearBloqueo(usuario, ID_SERVICIO, solicitud(null, null, null));

        // Assert
        ArgumentCaptor<BloqueoDisponibilidad> captor = ArgumentCaptor.forClass(BloqueoDisponibilidad.class);
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
        // Arrange
        when(bloqueoRepository.findByServicioIdServicioAndFecha(any(), any())).thenReturn(List.of());
        when(bloqueoRepository.save(any(BloqueoDisponibilidad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act
        DisponibilidadResponse respuesta = service.crearBloqueo(
                usuario, ID_SERVICIO, solicitud(LocalTime.of(9, 0), LocalTime.of(11, 0), "otro"));

        // Assert
        assertAll(
                () -> assertEquals(LocalTime.of(9, 0), respuesta.getHoraInicio()),
                () -> assertEquals(LocalTime.of(11, 0), respuesta.getHoraFin()),
                () -> assertEquals("OTRO", respuesta.getMotivo()),
                () -> assertEquals(Boolean.FALSE, respuesta.getDiaCompleto())
        );
    }

    @Test
    void deberiaRechazarBloqueoConSoloUnaDeLasHoras() {
        // Act
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.crearBloqueo(usuario, ID_SERVICIO, solicitud(LocalTime.of(9, 0), null, null)));

        // Assert
        assertEquals("INTERVALO_HORARIO_INVALIDO", ex.getCodigo());
        verify(bloqueoRepository, never()).save(any());
    }

    @Test
    void deberiaRechazarBloqueoDeDiaCompletoSiYaExisteUnBloqueoParcialEnLaFecha() {
        // Arrange
        BloqueoDisponibilidad existente = new BloqueoDisponibilidad();
        existente.setHoraInicio(LocalTime.of(9, 0));
        existente.setHoraFin(LocalTime.of(10, 0));
        when(bloqueoRepository.findByServicioIdServicioAndFecha(any(), any())).thenReturn(List.of(existente));

        // Act
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.crearBloqueo(usuario, ID_SERVICIO, solicitud(null, null, null)));

        // Assert
        assertEquals("BLOQUEO_TRASLAPADO", ex.getCodigo());
        verify(bloqueoRepository, never()).save(any());
    }
}
