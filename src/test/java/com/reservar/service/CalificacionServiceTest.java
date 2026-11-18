package com.reservar.service;

import com.reservar.dto.reserva.CalificacionRequest;
import com.reservar.entity.*;
import com.reservar.entity.enums.*;
import com.reservar.exception.BusinessException;
import com.reservar.repository.*;
import com.reservar.support.ServicioFixtures;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalificacionServiceTest {
    @Mock CalificacionRepository calificaciones;
    @Mock ReservaRepository reservas;
    CalificacionService service;
    Usuario cliente;
    Reserva reserva;
    CalificacionRequest request;
    Clock clock = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"), ZoneId.of("America/Bogota"));

    @BeforeEach void preparar() {
        service = new CalificacionService(calificaciones, reservas, clock);
        cliente = ServicioFixtures.usuario(Rol.CLIENTE, EstadoUsuario.ACTIVO);
        reserva = new Reserva();
        reserva.setIdReserva(1L);
        reserva.setCliente(cliente);
        reserva.setEstado(EstadoReserva.COMPLETADA);
        reserva.setFechaFin(LocalDateTime.now(clock).minusMinutes(1));
        request = new CalificacionRequest();
        request.setEstrellas((short) 5);
        request.setComentario(" Excelente ");
    }
    void encontrada() { when(reservas.findByIdParaActualizar(1L)).thenReturn(Optional.of(reserva)); }

    @Test void creaCalificacionPropiaCompletada() {
        encontrada();
        when(calificaciones.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        var response = service.crear(cliente, 1L, request);
        assertEquals((short) 5, response.estrellas());
        assertEquals("Excelente", response.comentario());
        assertEquals(1L, response.idReserva());
    }
    @Test void noDuplica() {
        encontrada();
        when(calificaciones.findByReservaIdReserva(1L)).thenReturn(Optional.of(new Calificacion()));
        assertEquals("CALIFICACION_EXISTENTE", assertThrows(BusinessException.class,
                () -> service.crear(cliente, 1L, request)).getCodigo());
        verify(calificaciones, never()).saveAndFlush(any());
    }
    @Test void editaMismoRegistro() {
        encontrada();
        Calificacion existente = new Calificacion();
        existente.setIdCalificacion(8L);
        existente.setReserva(reserva);
        when(calificaciones.findByReservaIdReserva(1L)).thenReturn(Optional.of(existente));
        when(calificaciones.saveAndFlush(existente)).thenReturn(existente);
        assertEquals(8L, service.editar(cliente, 1L, request).idCalificacion());
    }
    @Test void editarInexistente() {
        encontrada();
        assertEquals("CALIFICACION_NO_ENCONTRADA", assertThrows(BusinessException.class,
                () -> service.editar(cliente, 1L, request)).getCodigo());
    }
    @ParameterizedTest @EnumSource(value = EstadoReserva.class, names = "COMPLETADA", mode = EnumSource.Mode.EXCLUDE)
    void soloCompletadas(EstadoReserva estado) {
        encontrada();
        reserva.setEstado(estado);
        reserva.setFechaFin(LocalDateTime.now(clock).plusHours(1));
        assertEquals("ESTADO_RESERVA_INVALIDO", assertThrows(BusinessException.class,
                () -> service.crear(cliente, 1L, request)).getCodigo());
        verifyNoInteractions(calificaciones);
    }
    @Test void confirmaFinalizadaSeCompletaAntesDeCalificar() {
        encontrada();
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        when(calificaciones.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        service.crear(cliente, 1L, request);
        assertEquals(EstadoReserva.COMPLETADA, reserva.getEstado());
        verify(reservas).save(reserva);
    }
    @Test void noCalificaAjena() {
        encontrada();
        assertEquals("RESERVA_NO_PROPIA", assertThrows(BusinessException.class,
                () -> service.crear(ServicioFixtures.usuario(Rol.CLIENTE, EstadoUsuario.ACTIVO), 1L, request)).getCodigo());
        verifyNoInteractions(calificaciones);
    }
    @Test void exigeClienteActivo() {
        assertThrows(BusinessException.class, () -> service.crear(null, 1L, request));
        assertThrows(BusinessException.class, () -> service.crear(ServicioFixtures.proveedorActivo(), 1L, request));
        cliente.setEstado(EstadoUsuario.PENDIENTE);
        assertThrows(BusinessException.class, () -> service.crear(cliente, 1L, request));
        verifyNoInteractions(reservas, calificaciones);
    }
    @ParameterizedTest @ValueSource(shorts = {0, 6})
    void validaEstrellas(short estrellas) {
        encontrada();
        request.setEstrellas(estrellas);
        assertEquals("ESTRELLAS_INVALIDAS", assertThrows(BusinessException.class,
                () -> service.crear(cliente, 1L, request)).getCodigo());
    }
    @Test void comentarioMaximo() {
        encontrada();
        request.setComentario("x".repeat(501));
        assertEquals("COMENTARIO_INVALIDO", assertThrows(BusinessException.class,
                () -> service.crear(cliente, 1L, request)).getCodigo());
    }
    @Test void reservaInexistente() {
        assertEquals("RESERVA_NO_ENCONTRADA", assertThrows(BusinessException.class,
                () -> service.crear(cliente, 1L, request)).getCodigo());
    }

    // --- Pruebas agregadas por Calidad (Épica 3) ---

    @Test void usuarioSinIdentificadorNoEstaAutenticado() {
        // Arrange
        cliente.setIdUsuario(null);

        // Act + Assert
        assertEquals("USUARIO_NO_AUTENTICADO", assertThrows(BusinessException.class,
                () -> service.editar(cliente, 1L, request)).getCodigo());
        verifyNoInteractions(reservas, calificaciones);
    }

    @Test void solicitudSinEstrellasEsInvalida() {
        // Arrange
        encontrada();
        request.setEstrellas(null);

        // Act + Assert
        assertEquals("ESTRELLAS_INVALIDAS", assertThrows(BusinessException.class,
                () -> service.crear(cliente, 1L, request)).getCodigo());
        assertEquals("ESTRELLAS_INVALIDAS", assertThrows(BusinessException.class,
                () -> service.crear(cliente, 1L, null)).getCodigo());
        verify(calificaciones, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @ValueSource(strings = {"   "})
    void comentarioVacioSeGuardaComoNulo(String comentario) {
        // Arrange
        encontrada();
        request.setComentario(comentario);
        when(calificaciones.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        var respuesta = service.crear(cliente, 1L, request);

        // Assert
        assertNull(respuesta.comentario());
        assertEquals((short) 5, respuesta.estrellas());
    }

    @Test void comentarioDe500CaracteresEsElLimiteValido() {
        // Arrange
        encontrada();
        request.setComentario("x".repeat(500));
        when(calificaciones.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        var respuesta = service.crear(cliente, 1L, request);

        // Assert
        assertEquals(500, respuesta.comentario().length());
    }

    @Test void confirmadaQueNoHaTerminadoNoSeCalifica() {
        // Arrange
        encontrada();
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reserva.setFechaFin(LocalDateTime.now(clock).plusMinutes(30));

        // Act + Assert
        assertEquals("ESTADO_RESERVA_INVALIDO", assertThrows(BusinessException.class,
                () -> service.crear(cliente, 1L, request)).getCodigo());
        verify(reservas, never()).save(any());
        verifyNoInteractions(calificaciones);
    }
}
