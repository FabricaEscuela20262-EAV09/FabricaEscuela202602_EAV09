package com.reservar.service;

import com.reservar.entity.*;
import com.reservar.entity.enums.*;
import com.reservar.exception.BusinessException;
import com.reservar.repository.HistorialReservaRepository;
import com.reservar.repository.ReservaRepository;
import com.reservar.support.ServicioFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestionReservaServiceTest {
    @Mock ReservaRepository reservas;
    @Mock HistorialReservaRepository historial;
    GestionReservaService service;
    Usuario proveedor;
    Reserva reserva;
    final Clock clock = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"), ZoneId.of("America/Bogota"));

    @BeforeEach
    void preparar() {
        service = new GestionReservaService(reservas, historial, clock);
        proveedor = ServicioFixtures.proveedorActivo();
        reserva = new Reserva();
        reserva.setIdReserva(1L);
        reserva.setServicio(ServicioFixtures.oferta(ServicioFixtures.proveedor(3L, proveedor),
                ServicioFixtures.tipoActivo(), "ACTIVO"));
        reserva.setCliente(ServicioFixtures.usuario(Rol.CLIENTE, EstadoUsuario.ACTIVO));
        reserva.setFechaInicio(LocalDateTime.now(clock).plusDays(1));
        reserva.setFechaFin(reserva.getFechaInicio().plusMinutes(45));
        reserva.setEstado(EstadoReserva.PENDIENTE);
    }

    @Test void confirmaYAudita() {
        when(reservas.findByIdParaActualizar(1L)).thenReturn(Optional.of(reserva));
        assertEquals(EstadoReserva.CONFIRMADA, service.confirmar(proveedor, 1L).getEstado());
        ArgumentCaptor<HistorialReserva> captor = ArgumentCaptor.forClass(HistorialReserva.class);
        verify(historial).save(captor.capture());
        assertAll(() -> assertEquals(proveedor, captor.getValue().getActor()),
                () -> assertEquals(EstadoReserva.PENDIENTE, captor.getValue().getEstadoAnterior()),
                () -> assertEquals(EstadoReserva.CONFIRMADA, captor.getValue().getEstadoNuevo()),
                () -> assertEquals(LocalDateTime.now(clock), captor.getValue().getFechaCambio()));
        verify(reservas).save(reserva);
    }

    @Test void rechazaConMotivo() {
        when(reservas.findByIdParaActualizar(1L)).thenReturn(Optional.of(reserva));
        assertEquals(EstadoReserva.RECHAZADA, service.rechazar(proveedor, 1L, "  Ausencia  ").getEstado());
        ArgumentCaptor<HistorialReserva> captor = ArgumentCaptor.forClass(HistorialReserva.class);
        verify(historial).save(captor.capture());
        assertEquals("Ausencia", captor.getValue().getMotivo());
    }

    @Test void rechazaSinMotivo() {
        when(reservas.findByIdParaActualizar(1L)).thenReturn(Optional.of(reserva));
        assertEquals(EstadoReserva.RECHAZADA, service.rechazar(proveedor, 1L, null).getEstado());
    }

    @ParameterizedTest @EnumSource(value = EstadoReserva.class, names = "PENDIENTE", mode = EnumSource.Mode.EXCLUDE)
    void rechazaTransicionesDesdeOtrosEstados(EstadoReserva estado) {
        reserva.setEstado(estado);
        when(reservas.findByIdParaActualizar(1L)).thenReturn(Optional.of(reserva));
        assertEquals("ESTADO_RESERVA_INVALIDO", assertThrows(BusinessException.class,
                () -> service.confirmar(proveedor, 1L)).getCodigo());
        assertTrue(assertThrows(BusinessException.class,
                () -> service.rechazar(proveedor, 1L, null)).getMessage().contains(estado.name()));
        verifyNoInteractions(historial);
        verify(reservas, never()).save(any());
    }

    @Test void impideProveedorAjeno() {
        when(reservas.findByIdParaActualizar(1L)).thenReturn(Optional.of(reserva));
        assertEquals("RESERVA_NO_PROPIA", assertThrows(BusinessException.class,
                () -> service.confirmar(ServicioFixtures.proveedorActivo(), 1L)).getCodigo());
        verifyNoInteractions(historial);
    }

    @Test void exigeProveedorActivoYSesion() {
        assertEquals("USUARIO_NO_AUTENTICADO", assertThrows(BusinessException.class,
                () -> service.confirmar(null, 1L)).getCodigo());
        assertEquals("ROL_NO_AUTORIZADO", assertThrows(BusinessException.class,
                () -> service.confirmar(reserva.getCliente(), 1L)).getCodigo());
        proveedor.setEstado(EstadoUsuario.PENDIENTE);
        assertEquals("CUENTA_NO_VERIFICADA", assertThrows(BusinessException.class,
                () -> service.confirmar(proveedor, 1L)).getCodigo());
        verifyNoInteractions(reservas, historial);
    }

    @Test void reservaInexistente() {
        when(reservas.findByIdParaActualizar(1L)).thenReturn(Optional.empty());
        assertEquals("RESERVA_NO_ENCONTRADA", assertThrows(BusinessException.class,
                () -> service.confirmar(proveedor, 1L)).getCodigo());
    }

    @ParameterizedTest @EnumSource(value = EstadoReserva.class, names = {"PENDIENTE", "CONFIRMADA"})
    void clienteCancelaYAudita(EstadoReserva estado) {
        reserva.setEstado(estado);
        when(reservas.findByIdParaActualizar(1L)).thenReturn(Optional.of(reserva));
        assertEquals(EstadoReserva.CANCELADA, service.cancelar(reserva.getCliente(), 1L, null).getEstado());
        ArgumentCaptor<HistorialReserva> captor = ArgumentCaptor.forClass(HistorialReserva.class);
        verify(historial).save(captor.capture());
        assertEquals(reserva.getCliente(), captor.getValue().getActor());
        assertEquals(estado, captor.getValue().getEstadoAnterior());
        assertEquals(EstadoReserva.CANCELADA, captor.getValue().getEstadoNuevo());
        assertEquals(LocalDateTime.now(clock), captor.getValue().getFechaCambio());
        assertSame(reserva, captor.getValue().getReserva());
        verify(reservas).save(reserva);
    }

    @Test void proveedorCancelaConfirmada() {
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        when(reservas.findByIdParaActualizar(1L)).thenReturn(Optional.of(reserva));
        assertEquals(EstadoReserva.CANCELADA, service.cancelar(proveedor, 1L, "Imprevisto").getEstado());
        ArgumentCaptor<HistorialReserva> captor = ArgumentCaptor.forClass(HistorialReserva.class);
        verify(historial).save(captor.capture());
        assertAll(() -> assertSame(proveedor, captor.getValue().getActor()),
                () -> assertEquals(EstadoReserva.CONFIRMADA, captor.getValue().getEstadoAnterior()),
                () -> assertEquals(EstadoReserva.CANCELADA, captor.getValue().getEstadoNuevo()),
                () -> assertEquals(LocalDateTime.now(clock), captor.getValue().getFechaCambio()),
                () -> assertEquals("Imprevisto", captor.getValue().getMotivo()),
                () -> assertSame(reserva, captor.getValue().getReserva()));
        verify(reservas).save(reserva);
    }

    @Test void proveedorNoCancelaPendiente() {
        when(reservas.findByIdParaActualizar(1L)).thenReturn(Optional.of(reserva));
        assertThrows(BusinessException.class, () -> service.cancelar(proveedor, 1L, null));
        verifyNoInteractions(historial);
    }

    @ParameterizedTest @EnumSource(value = EstadoReserva.class, names = {"COMPLETADA", "CANCELADA", "RECHAZADA"})
    void noCancelaTerminales(EstadoReserva estado) {
        reserva.setEstado(estado);
        when(reservas.findByIdParaActualizar(1L)).thenReturn(Optional.of(reserva));
        assertThrows(BusinessException.class, () -> service.cancelar(reserva.getCliente(), 1L, null));
        verifyNoInteractions(historial);
    }

    @Test void noCancelaReservaAjena() {
        when(reservas.findByIdParaActualizar(1L)).thenReturn(Optional.of(reserva));
        assertEquals("RESERVA_NO_PROPIA", assertThrows(BusinessException.class,
                () -> service.cancelar(ServicioFixtures.usuario(Rol.CLIENTE, EstadoUsuario.ACTIVO), 1L, null)).getCodigo());
        verifyNoInteractions(historial);
    }

    @Test void noCancelaConfirmadaFinalizada() {
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reserva.setFechaFin(LocalDateTime.now(clock));
        when(reservas.findByIdParaActualizar(1L)).thenReturn(Optional.of(reserva));
        assertThrows(BusinessException.class, () -> service.cancelar(reserva.getCliente(), 1L, null));
        verifyNoInteractions(historial);
    }

    @Test void historialSiempreFiltraPorUsuarioAutenticado() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(reservas.findHistorialPropio(proveedor.getIdUsuario(), EstadoReserva.CONFIRMADA, pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(reserva)));
        assertEquals(1, service.historial(proveedor, EstadoReserva.CONFIRMADA, pageable).getTotalElements());
        verify(reservas).completarFinalizadas(EstadoReserva.CONFIRMADA, EstadoReserva.COMPLETADA, LocalDateTime.now(clock));
    }

    @Test void historialClienteUsaIdentidadDeSesionSinFiltro() {
        var cliente = reserva.getCliente();
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(reservas.findHistorialPropio(cliente.getIdUsuario(), null, pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(reserva)));
        assertEquals(1L, service.historial(cliente, null, pageable).getContent().get(0).getIdReserva());
        verify(reservas).findHistorialPropio(cliente.getIdUsuario(), null, pageable);
    }

    @Test void clienteConsultaDetallePropio() {
        when(reservas.findByIdParaActualizar(1L)).thenReturn(Optional.of(reserva));
        assertEquals(1L, service.detalle(reserva.getCliente(), 1L).getIdReserva());
    }

    @Test void impideDetalleAjeno() {
        when(reservas.findByIdParaActualizar(1L)).thenReturn(Optional.of(reserva));
        assertEquals("RESERVA_NO_PROPIA", assertThrows(BusinessException.class,
                () -> service.detalle(ServicioFixtures.proveedorActivo(), 1L)).getCodigo());
    }

    @Test void auditoriaSoloParticipante() {
        when(reservas.findById(1L)).thenReturn(Optional.of(reserva));
        HistorialReserva evento = new HistorialReserva();
        evento.setIdHistorial(2L);
        evento.setActor(proveedor);
        evento.setEstadoNuevo(EstadoReserva.CONFIRMADA);
        evento.setFechaCambio(LocalDateTime.now(clock));
        when(historial.findByReservaIdReservaOrderByFechaCambioAscIdHistorialAsc(1L))
                .thenReturn(java.util.List.of(evento));
        assertEquals(proveedor.getIdUsuario(), service.auditoria(reserva.getCliente(), 1L).get(0).idActor());
        assertEquals("RESERVA_NO_PROPIA", assertThrows(BusinessException.class,
                () -> service.auditoria(ServicioFixtures.proveedorActivo(), 1L)).getCodigo());
    }

    @Test void finalizadorUsaRelojColombia() {
        new FinalizacionReservaService(reservas, clock).completarFinalizadas();
        verify(reservas).completarFinalizadas(EstadoReserva.CONFIRMADA, EstadoReserva.COMPLETADA, LocalDateTime.now(clock));
    }

    @Test void limitaMotivoSinModificarReserva() {
        when(reservas.findByIdParaActualizar(1L)).thenReturn(Optional.of(reserva));
        assertEquals("MOTIVO_INVALIDO", assertThrows(BusinessException.class,
                () -> service.rechazar(proveedor, 1L, "x".repeat(256))).getCodigo());
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
        verifyNoInteractions(historial);
    }
}
