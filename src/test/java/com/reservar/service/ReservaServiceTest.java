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
import com.reservar.support.ServicioFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.reservar.support.ServicioFixtures.ID_SERVICIO;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HU-11: creación de reservas. Reloj fijo en America/Bogota: martes 10-nov-2026 10:00.
 * La reserva base es el miércoles 11-nov-2026 de 09:00 a 09:45 (oferta de 45 minutos).
 */
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-10T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDate MIERCOLES = LocalDate.of(2026, 11, 11);
    private static final LocalDateTime INICIO = MIERCOLES.atTime(9, 0);
    private static final LocalDateTime FIN = INICIO.plusMinutes(45);

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private HistorialReservaRepository historialRepository;
    @Mock
    private OfertaServicioRepository ofertaRepository;
    @Mock
    private DisponibilidadServicioRepository disponibilidadRepository;
    @Mock
    private BloqueoDisponibilidadRepository bloqueoRepository;

    private ReservaService service;
    private Usuario cliente;
    private Usuario usuarioProveedor;
    private OfertaServicio oferta;

    @BeforeEach
    void preparar() {
        service = new ReservaService(reservaRepository, historialRepository, ofertaRepository,
                disponibilidadRepository, bloqueoRepository, CLOCK);
        cliente = ServicioFixtures.usuario(Rol.CLIENTE, EstadoUsuario.ACTIVO);
        usuarioProveedor = ServicioFixtures.proveedorActivo();
        oferta = ServicioFixtures.oferta(
                ServicioFixtures.proveedor(ServicioFixtures.ID_PROVEEDOR, usuarioProveedor),
                ServicioFixtures.tipoActivo(), "ACTIVO");

        lenient().when(ofertaRepository.findByIdServicioAndEstado(ID_SERVICIO, "ACTIVO"))
                .thenReturn(Optional.of(oferta));
        lenient().when(disponibilidadRepository.findByServicioIdServicioAndDiaSemana(ID_SERVICIO, "MIERCOLES"))
                .thenReturn(List.of(horario("MIERCOLES", LocalTime.of(8, 0), LocalTime.of(12, 0))));
        lenient().when(bloqueoRepository.findByServicioIdServicioAndFecha(ID_SERVICIO, MIERCOLES))
                .thenReturn(List.of());
        lenient().when(reservaRepository.existeReservaSuperpuesta(eq(ID_SERVICIO), any(), any(), anyList()))
                .thenReturn(false);
        lenient().when(reservaRepository.saveAndFlush(any(Reserva.class))).thenAnswer(invocacion -> {
            Reserva reserva = invocacion.getArgument(0);
            reserva.setIdReserva(99L);
            reserva.setFechaCreacion(LocalDateTime.now(CLOCK));
            return reserva;
        });
    }

    private static CrearReservaRequest solicitud(LocalDateTime fechaInicio) {
        CrearReservaRequest request = new CrearReservaRequest();
        request.setIdServicio(ID_SERVICIO);
        request.setFechaInicio(fechaInicio);
        return request;
    }

    private static DisponibilidadServicio horario(String dia, LocalTime inicio, LocalTime fin) {
        DisponibilidadServicio horario = new DisponibilidadServicio();
        horario.setDiaSemana(dia);
        horario.setHoraInicio(inicio);
        horario.setHoraFin(fin);
        return horario;
    }

    private static BloqueoDisponibilidad bloqueo(LocalTime inicio, LocalTime fin) {
        BloqueoDisponibilidad bloqueo = new BloqueoDisponibilidad();
        bloqueo.setFecha(MIERCOLES);
        bloqueo.setHoraInicio(inicio);
        bloqueo.setHoraFin(fin);
        return bloqueo;
    }

    private String codigo(Usuario usuario, CrearReservaRequest request) {
        return assertThrows(BusinessException.class, () -> service.crearReserva(usuario, request)).getCodigo();
    }

    @Nested
    class CreacionExitosa {

        @Test
        void deberiaCrearLaReservaPendienteYCalcularLaFechaFin() {
            // Act
            ReservaResponse respuesta = service.crearReserva(cliente, solicitud(INICIO));

            // Assert
            assertAll(
                    () -> assertEquals(99L, respuesta.getIdReserva()),
                    () -> assertEquals(ID_SERVICIO, respuesta.getIdServicio()),
                    () -> assertEquals("Limpieza dental", respuesta.getNombreServicio()),
                    () -> assertEquals(INICIO, respuesta.getFechaInicio()),
                    () -> assertEquals(FIN, respuesta.getFechaFin()),
                    () -> assertEquals(EstadoReserva.PENDIENTE, respuesta.getEstado()),
                    () -> assertEquals(LocalDateTime.now(CLOCK), respuesta.getFechaCreacion())
            );
            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).saveAndFlush(captor.capture());
            assertAll(
                    () -> assertSame(cliente, captor.getValue().getCliente()),
                    () -> assertSame(oferta, captor.getValue().getServicio())
            );
        }

        @Test
        void deberiaRegistrarElEventoInicialEnElHistorialUsandoElRelojDeBogota() {
            // Act
            service.crearReserva(cliente, solicitud(INICIO));

            // Assert
            ArgumentCaptor<HistorialReserva> captor = ArgumentCaptor.forClass(HistorialReserva.class);
            verify(historialRepository).save(captor.capture());
            HistorialReserva historial = captor.getValue();
            assertAll(
                    () -> assertSame(cliente, historial.getActor()),
                    () -> assertEquals(99L, historial.getReserva().getIdReserva()),
                    () -> assertNull(historial.getEstadoAnterior()),
                    () -> assertEquals(EstadoReserva.PENDIENTE, historial.getEstadoNuevo()),
                    () -> assertEquals(LocalDateTime.of(2026, 11, 10, 10, 0), historial.getFechaCambio()),
                    () -> assertEquals("Creación de reserva", historial.getMotivo())
            );
        }

        @Test
        void deberiaConsultarSoloEstadosQueOcupanElHorario() {
            // Act
            service.crearReserva(cliente, solicitud(INICIO));

            // Assert
            verify(reservaRepository).existeReservaSuperpuesta(ID_SERVICIO, INICIO, FIN,
                    List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA));
        }

        @Test
        void deberiaAceptarUnaReservaQueOcupaExactamenteLosBordesDelHorario() {
            // Arrange: la franja termina justo cuando termina el servicio.
            when(disponibilidadRepository.findByServicioIdServicioAndDiaSemana(ID_SERVICIO, "MIERCOLES"))
                    .thenReturn(List.of(horario("MIERCOLES", LocalTime.of(9, 0), LocalTime.of(9, 45))));

            // Act
            ReservaResponse respuesta = service.crearReserva(cliente, solicitud(INICIO));

            // Assert
            assertEquals(FIN, respuesta.getFechaFin());
        }

        @Test
        void deberiaPermitirReservarJustoDespuesDeUnBloqueoContiguo() {
            // Arrange: el bloqueo termina a las 09:00 y la reserva inicia a las 09:00.
            when(bloqueoRepository.findByServicioIdServicioAndFecha(ID_SERVICIO, MIERCOLES))
                    .thenReturn(List.of(bloqueo(LocalTime.of(8, 0), LocalTime.of(9, 0))));

            // Act + Assert
            assertEquals(EstadoReserva.PENDIENTE, service.crearReserva(cliente, solicitud(INICIO)).getEstado());
        }

        @Test
        void deberiaPermitirReservarJustoAntesDeUnBloqueoContiguo() {
            // Arrange: la reserva termina a las 09:45 y el bloqueo inicia a las 09:45.
            when(bloqueoRepository.findByServicioIdServicioAndFecha(ID_SERVICIO, MIERCOLES))
                    .thenReturn(List.of(bloqueo(LocalTime.of(9, 45), LocalTime.of(11, 0))));

            // Act + Assert
            assertEquals(EstadoReserva.PENDIENTE, service.crearReserva(cliente, solicitud(INICIO)).getEstado());
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "2026-11-16T09:00,LUNES",
                "2026-11-17T09:00,MARTES",
                "2026-11-11T09:00,MIERCOLES",
                "2026-11-12T09:00,JUEVES",
                "2026-11-13T09:00,VIERNES",
                "2026-11-14T09:00,SABADO",
                "2026-11-15T09:00,DOMINGO"
        })
        void deberiaBuscarLaDisponibilidadDelDiaDeLaSemanaEnEspanol(LocalDateTime inicio, String dia) {
            // Arrange
            when(disponibilidadRepository.findByServicioIdServicioAndDiaSemana(ID_SERVICIO, dia))
                    .thenReturn(List.of(horario(dia, LocalTime.of(8, 0), LocalTime.of(12, 0))));

            // Act
            service.crearReserva(cliente, solicitud(inicio));

            // Assert
            verify(disponibilidadRepository).findByServicioIdServicioAndDiaSemana(ID_SERVICIO, dia);
        }
    }

    @Nested
    class ValidacionDelCliente {

        @Test
        void deberiaRechazarAUnProveedor() {
            // Act + Assert
            assertEquals("ROL_NO_AUTORIZADO", codigo(usuarioProveedor, solicitud(INICIO)));
            verifyNoInteractions(ofertaRepository, reservaRepository, historialRepository);
        }

        @Test
        void deberiaRechazarAUnClienteSinVerificar() {
            // Arrange
            cliente.setEstado(EstadoUsuario.PENDIENTE);

            // Act + Assert
            assertEquals("CUENTA_NO_VERIFICADA", codigo(cliente, solicitud(INICIO)));
            verifyNoInteractions(reservaRepository);
        }

        @Test
        void deberiaRechazarAUnClienteInactivo() {
            // Arrange
            cliente.setEstado(EstadoUsuario.INACTIVO);

            // Act + Assert
            assertEquals("CUENTA_INACTIVA", codigo(cliente, solicitud(INICIO)));
            verifyNoInteractions(reservaRepository);
        }
    }

    @Nested
    class ValidacionDeLaOferta {

        @Test
        void deberiaRechazarUnaOfertaInexistenteOInactiva() {
            // Arrange
            when(ofertaRepository.findByIdServicioAndEstado(ID_SERVICIO, "ACTIVO")).thenReturn(Optional.empty());

            // Act + Assert
            assertEquals("OFERTA_NO_DISPONIBLE", codigo(cliente, solicitud(INICIO)));
            verifyNoInteractions(reservaRepository);
        }

        @Test
        void deberiaRechazarUnaOfertaConTipoDeServicioInactivo() {
            // Arrange
            oferta.getTipoServicio().setEstado("INACTIVO");

            // Act + Assert
            assertEquals("OFERTA_NO_DISPONIBLE", codigo(cliente, solicitud(INICIO)));
        }

        @Test
        void deberiaRechazarUnaOfertaConCategoriaInactiva() {
            // Arrange
            oferta.getTipoServicio().getCategoria().setEstado("INACTIVO");

            // Act + Assert
            assertEquals("OFERTA_NO_DISPONIBLE", codigo(cliente, solicitud(INICIO)));
        }

        @Test
        void deberiaImpedirQueElClienteReserveSuPropiaOferta() {
            // Arrange: el mismo usuario figura como dueño de la oferta.
            oferta.getProveedor().setUsuario(cliente);

            // Act + Assert
            assertEquals("AUTORRESERVA_NO_PERMITIDA", codigo(cliente, solicitud(INICIO)));
            verify(reservaRepository, never()).saveAndFlush(any());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -15})
        void deberiaRechazarDuracionesNoPositivas(int duracion) {
            // Arrange
            oferta.setDuracionMinutos(duracion);

            // Act + Assert
            assertEquals("DURACION_SERVICIO_INVALIDA", codigo(cliente, solicitud(INICIO)));
        }

        @Test
        void deberiaRechazarUnaDuracionSinConfigurar() {
            // Arrange
            oferta.setDuracionMinutos(null);

            // Act + Assert
            assertEquals("DURACION_SERVICIO_INVALIDA", codigo(cliente, solicitud(INICIO)));
        }
    }

    @Nested
    class ValidacionDeLaFecha {

        @Test
        void deberiaRechazarUnaFechaNula() {
            // Act + Assert
            assertEquals("FECHA_RESERVA_INVALIDA", codigo(cliente, solicitud(null)));
        }

        @Test
        void deberiaRechazarUnaFechaPasada() {
            // Act + Assert
            assertEquals("FECHA_RESERVA_INVALIDA",
                    codigo(cliente, solicitud(LocalDateTime.of(2026, 11, 10, 9, 59))));
        }

        @Test
        void deberiaRechazarUnaReservaQueIniciaExactamenteAhora() {
            // Act + Assert
            assertEquals("FECHA_RESERVA_INVALIDA",
                    codigo(cliente, solicitud(LocalDateTime.of(2026, 11, 10, 10, 0))));
        }

        @Test
        void deberiaEvaluarLaFechaFuturaConElRelojDeBogotaYNoConElDelServidor() {
            // Arrange: "ahora" son las 10:00 en Bogotá (15:00 UTC). Con el reloj de Bogotá, 10:30 es futuro
            // y 09:30 es pasado, sin importar la zona horaria de la JVM (regresión del bug #16 en HU-11).
            when(disponibilidadRepository.findByServicioIdServicioAndDiaSemana(ID_SERVICIO, "MARTES"))
                    .thenReturn(List.of(horario("MARTES", LocalTime.of(8, 0), LocalTime.of(12, 0))));
            lenient().when(bloqueoRepository.findByServicioIdServicioAndFecha(eq(ID_SERVICIO), any()))
                    .thenReturn(List.of());

            // Act + Assert
            assertEquals(EstadoReserva.PENDIENTE,
                    service.crearReserva(cliente, solicitud(LocalDateTime.of(2026, 11, 10, 10, 30))).getEstado());
            assertEquals("FECHA_RESERVA_INVALIDA",
                    codigo(cliente, solicitud(LocalDateTime.of(2026, 11, 10, 9, 30))));
        }

        @Test
        void deberiaRechazarUnaReservaQueTerminaAlDiaSiguiente() {
            // Act + Assert: 23:30 + 45 min = 00:15 del jueves.
            assertEquals("RESERVA_FUERA_DE_HORARIO",
                    codigo(cliente, solicitud(MIERCOLES.atTime(23, 30))));
            verifyNoInteractions(disponibilidadRepository);
        }
    }

    @Nested
    class ValidacionDeDisponibilidad {

        @Test
        void deberiaRechazarCuandoElDiaNoTieneHorarios() {
            // Arrange
            when(disponibilidadRepository.findByServicioIdServicioAndDiaSemana(ID_SERVICIO, "MIERCOLES"))
                    .thenReturn(List.of());

            // Act + Assert
            assertEquals("FUERA_DE_DISPONIBILIDAD", codigo(cliente, solicitud(INICIO)));
        }

        @Test
        void deberiaRechazarCuandoLaReservaIniciaAntesDelHorario() {
            // Act + Assert
            assertEquals("FUERA_DE_DISPONIBILIDAD", codigo(cliente, solicitud(MIERCOLES.atTime(7, 30))));
        }

        @Test
        void deberiaRechazarCuandoElServicioNoAlcanzaATerminarDentroDelHorario() {
            // Act + Assert: 11:30 + 45 min = 12:15 > 12:00.
            assertEquals("FUERA_DE_DISPONIBILIDAD", codigo(cliente, solicitud(MIERCOLES.atTime(11, 30))));
            verifyNoInteractions(bloqueoRepository);
        }

        @Test
        void deberiaRechazarCuandoCoincideConUnBloqueo() {
            // Arrange
            when(bloqueoRepository.findByServicioIdServicioAndFecha(ID_SERVICIO, MIERCOLES))
                    .thenReturn(List.of(bloqueo(LocalTime.of(9, 30), LocalTime.of(10, 0))));

            // Act + Assert
            assertEquals("HORARIO_BLOQUEADO", codigo(cliente, solicitud(INICIO)));
            verify(reservaRepository, never()).existeReservaSuperpuesta(anyLong(), any(), any(), anyList());
        }

        @Test
        void deberiaRechazarCuandoYaExisteUnaReservaSuperpuesta() {
            // Arrange
            when(reservaRepository.existeReservaSuperpuesta(eq(ID_SERVICIO), any(), any(), anyList()))
                    .thenReturn(true);

            // Act + Assert
            assertEquals("HORARIO_NO_DISPONIBLE", codigo(cliente, solicitud(INICIO)));
            verify(reservaRepository, never()).saveAndFlush(any());
            verifyNoInteractions(historialRepository);
        }
    }

    @Nested
    class Concurrencia {

        @Test
        void deberiaTraducirLaRestriccionDeExclusionDePostgresAHorarioNoDisponible() {
            // Arrange: 23P01 = exclusion_violation (dos clientes reservan el mismo intervalo a la vez).
            DataIntegrityViolationException conflicto = new DataIntegrityViolationException("conflicto",
                    new RuntimeException("envoltorio", new SQLException("exclusion", "23P01")));
            when(reservaRepository.saveAndFlush(any(Reserva.class))).thenThrow(conflicto);

            // Act + Assert
            assertEquals("HORARIO_NO_DISPONIBLE", codigo(cliente, solicitud(INICIO)));
            verifyNoInteractions(historialRepository);
        }

        @Test
        void deberiaPropagarOtrasViolacionesDeIntegridad() {
            // Arrange
            DataIntegrityViolationException otra = new DataIntegrityViolationException("fk",
                    new SQLException("foreign key", "23503"));
            when(reservaRepository.saveAndFlush(any(Reserva.class))).thenThrow(otra);

            // Act + Assert
            assertSame(otra, assertThrows(DataIntegrityViolationException.class,
                    () -> service.crearReserva(cliente, solicitud(INICIO))));
        }

        @Test
        void deberiaPropagarViolacionesSinCausaSql() {
            // Arrange
            DataIntegrityViolationException sinCausa = new DataIntegrityViolationException("sin causa");
            when(reservaRepository.saveAndFlush(any(Reserva.class))).thenThrow(sinCausa);

            // Act + Assert
            assertSame(sinCausa, assertThrows(DataIntegrityViolationException.class,
                    () -> service.crearReserva(cliente, solicitud(INICIO))));
        }
    }
}
