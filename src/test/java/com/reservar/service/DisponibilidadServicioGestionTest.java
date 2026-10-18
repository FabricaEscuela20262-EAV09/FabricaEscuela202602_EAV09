package com.reservar.service;

import com.reservar.dto.servicio.BloqueoDisponibilidadRequest;
import com.reservar.dto.servicio.DisponibilidadRequest;
import com.reservar.dto.servicio.DisponibilidadResponse;
import com.reservar.entity.BloqueoDisponibilidad;
import com.reservar.entity.DisponibilidadServicio;
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
import com.reservar.support.ServicioFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.reservar.support.ServicioFixtures.ID_SERVICIO;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HU-08: franjas semanales, validación de intervalos/traslapes, bloqueos de fechas y autorización del dueño.
 */
@ExtendWith(MockitoExtension.class)
class DisponibilidadServicioGestionTest {

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
    private Proveedor proveedor;
    private OfertaServicio oferta;

    @BeforeEach
    void setUp() {
        usuario = ServicioFixtures.proveedorActivo();
        proveedor = ServicioFixtures.proveedor(ServicioFixtures.ID_PROVEEDOR, usuario);
        oferta = ServicioFixtures.oferta(proveedor, ServicioFixtures.tipoActivo(), "ACTIVO");

        lenient().when(proveedorRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()))
                .thenReturn(Optional.of(proveedor));
        lenient().when(ofertaRepository.findByIdServicioAndEstado(ID_SERVICIO, "ACTIVO"))
                .thenReturn(Optional.of(oferta));
        lenient().when(disponibilidadRepository.save(any(DisponibilidadServicio.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        lenient().when(bloqueoRepository.save(any(BloqueoDisponibilidad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private static String codigo(Runnable accion) {
        return assertThrows(BusinessException.class, accion::run).getCodigo();
    }

    private static DisponibilidadRequest franja(String dia, LocalTime inicio, LocalTime fin) {
        DisponibilidadRequest request = new DisponibilidadRequest();
        request.setDiaSemana(dia);
        request.setHoraInicio(inicio);
        request.setHoraFin(fin);
        return request;
    }

    private static DisponibilidadServicio franjaExistente(Long id, OfertaServicio servicio, String dia,
                                                          LocalTime inicio, LocalTime fin) {
        DisponibilidadServicio existente = new DisponibilidadServicio();
        existente.setIdDisponibilidad(id);
        existente.setServicio(servicio);
        existente.setDiaSemana(dia);
        existente.setHoraInicio(inicio);
        existente.setHoraFin(fin);
        return existente;
    }

    private static BloqueoDisponibilidadRequest bloqueoRequest(LocalDate fecha, LocalTime inicio, LocalTime fin,
                                                               String motivo) {
        BloqueoDisponibilidadRequest request = new BloqueoDisponibilidadRequest();
        request.setFecha(fecha);
        request.setHoraInicio(inicio);
        request.setHoraFin(fin);
        request.setMotivo(motivo);
        return request;
    }

    private static BloqueoDisponibilidad bloqueoExistente(Long id, OfertaServicio servicio, LocalDate fecha,
                                                          LocalTime inicio, LocalTime fin) {
        BloqueoDisponibilidad existente = new BloqueoDisponibilidad();
        existente.setIdBloqueo(id);
        existente.setServicio(servicio);
        existente.setFecha(fecha);
        existente.setHoraInicio(inicio);
        existente.setHoraFin(fin);
        existente.setMotivo("OTRO");
        return existente;
    }

    private OfertaServicio ofertaDeOtroProveedor() {
        Proveedor otro = ServicioFixtures.proveedor(99L, ServicioFixtures.proveedorActivo());
        return ServicioFixtures.oferta(otro, ServicioFixtures.tipoActivo(), "ACTIVO");
    }

    @Nested
    class AgregarDisponibilidad {

        @Test
        void deberiaRegistrarLaFranjaSemanalNormalizandoElDia() {
            // Arrange
            when(disponibilidadRepository.findByServicioIdServicioAndDiaSemana(ID_SERVICIO, "LUNES"))
                    .thenReturn(List.of());

            // Act
            DisponibilidadResponse respuesta = service.agregarDisponibilidad(
                    usuario, ID_SERVICIO, franja("  lunes ", LocalTime.of(9, 0), LocalTime.of(12, 0)));

            // Assert
            ArgumentCaptor<DisponibilidadServicio> captor = ArgumentCaptor.forClass(DisponibilidadServicio.class);
            verify(disponibilidadRepository).save(captor.capture());
            assertAll(
                    () -> assertEquals("LUNES", captor.getValue().getDiaSemana()),
                    () -> assertEquals(oferta, captor.getValue().getServicio()),
                    () -> assertEquals("SEMANAL", respuesta.getTipo()),
                    () -> assertEquals("LUNES", respuesta.getDiaSemana()),
                    () -> assertEquals(LocalTime.of(9, 0), respuesta.getHoraInicio()),
                    () -> assertEquals(LocalTime.of(12, 0), respuesta.getHoraFin()),
                    () -> assertEquals(ID_SERVICIO, respuesta.getIdServicio())
            );
        }

        @Test
        void deberiaPermitirFranjasContiguasEnElMismoDia() {
            // Arrange
            DisponibilidadServicio existente = franjaExistente(
                    1L, oferta, "MARTES", LocalTime.of(9, 0), LocalTime.of(10, 0));
            when(disponibilidadRepository.findByServicioIdServicioAndDiaSemana(ID_SERVICIO, "MARTES"))
                    .thenReturn(List.of(existente));

            // Act
            DisponibilidadResponse despues = service.agregarDisponibilidad(
                    usuario, ID_SERVICIO, franja("MARTES", LocalTime.of(10, 0), LocalTime.of(11, 0)));
            DisponibilidadResponse antes = service.agregarDisponibilidad(
                    usuario, ID_SERVICIO, franja("MARTES", LocalTime.of(8, 0), LocalTime.of(9, 0)));

            // Assert
            assertAll(
                    () -> assertEquals(LocalTime.of(10, 0), despues.getHoraInicio()),
                    () -> assertEquals(LocalTime.of(8, 0), antes.getHoraInicio())
            );
        }

        @ParameterizedTest(name = "[{index}] {0}-{1} se traslapa con 09:00-11:00")
        @ValueSource(strings = {"08:00,10:00", "10:00,12:00", "09:30,10:30", "08:00,12:00", "09:00,11:00"})
        void deberiaRechazarFranjasQueSeTraslapanConUnaExistente(String rango) {
            // Arrange
            String[] horas = rango.split(",");
            DisponibilidadServicio existente = franjaExistente(
                    1L, oferta, "MIERCOLES", LocalTime.of(9, 0), LocalTime.of(11, 0));
            when(disponibilidadRepository.findByServicioIdServicioAndDiaSemana(ID_SERVICIO, "MIERCOLES"))
                    .thenReturn(List.of(existente));
            DisponibilidadRequest request = franja("MIERCOLES", LocalTime.parse(horas[0]), LocalTime.parse(horas[1]));

            // Act + Assert
            assertEquals("DISPONIBILIDAD_TRASLAPADA", codigo(() -> service.agregarDisponibilidad(usuario, ID_SERVICIO, request)));
            verify(disponibilidadRepository, never()).save(any());
        }

        @Test
        void deberiaRechazarHoraDeFinIgualALaDeInicio() {
            // Act + Assert
            assertEquals("INTERVALO_HORARIO_INVALIDO", codigo(() -> service.agregarDisponibilidad(
                    usuario, ID_SERVICIO, franja("LUNES", LocalTime.of(9, 0), LocalTime.of(9, 0)))));
        }

        @Test
        void deberiaRechazarHoraDeFinAnteriorALaDeInicio() {
            // Act + Assert
            assertEquals("INTERVALO_HORARIO_INVALIDO", codigo(() -> service.agregarDisponibilidad(
                    usuario, ID_SERVICIO, franja("LUNES", LocalTime.of(12, 0), LocalTime.of(9, 0)))));
            verify(disponibilidadRepository, never()).save(any());
        }

        @Test
        void deberiaRechazarHorasNulas() {
            // Act + Assert
            assertEquals("INTERVALO_HORARIO_INVALIDO", codigo(() -> service.agregarDisponibilidad(
                    usuario, ID_SERVICIO, franja("LUNES", null, LocalTime.of(9, 0)))));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"FERIADO", "MIÉRCOLES", "monday"})
        void deberiaRechazarDiasQueNoSonDeLaSemana(String dia) {
            assertEquals("DIA_SEMANA_INVALIDO", codigo(() -> service.agregarDisponibilidad(
                    usuario, ID_SERVICIO, franja(dia, LocalTime.of(9, 0), LocalTime.of(10, 0)))));
        }

        @ParameterizedTest
        @ValueSource(strings = {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"})
        void deberiaAceptarLosSieteDiasDeLaSemana(String dia) {
            when(disponibilidadRepository.findByServicioIdServicioAndDiaSemana(ID_SERVICIO, dia))
                    .thenReturn(List.of());

            DisponibilidadResponse respuesta = service.agregarDisponibilidad(
                    usuario, ID_SERVICIO, franja(dia, LocalTime.of(9, 0), LocalTime.of(10, 0)));

            assertEquals(dia, respuesta.getDiaSemana());
        }

        @Test
        void deberiaRechazarConForbiddenSemanticoCuandoElServicioEsDeOtroProveedor() {
            // Arrange
            when(ofertaRepository.findByIdServicioAndEstado(ID_SERVICIO, "ACTIVO"))
                    .thenReturn(Optional.of(ofertaDeOtroProveedor()));

            // Act + Assert
            assertEquals("OFERTA_NO_PROPIA", codigo(() -> service.agregarDisponibilidad(
                    usuario, ID_SERVICIO, franja("LUNES", LocalTime.of(9, 0), LocalTime.of(10, 0)))));
            verify(disponibilidadRepository, never()).save(any());
        }

        @Test
        void deberiaRechazarCuandoElUsuarioEsCliente() {
            // Arrange
            Usuario cliente = ServicioFixtures.usuario(Rol.CLIENTE, EstadoUsuario.ACTIVO);

            // Act + Assert
            assertEquals("ROL_NO_AUTORIZADO", codigo(() -> service.agregarDisponibilidad(
                    cliente, ID_SERVICIO, franja("LUNES", LocalTime.of(9, 0), LocalTime.of(10, 0)))));
        }

        @Test
        void deberiaRechazarCuandoNoHayUsuarioORolNulo() {
            // Arrange
            Usuario sinRol = ServicioFixtures.usuario(null, EstadoUsuario.ACTIVO);
            DisponibilidadRequest request = franja("LUNES", LocalTime.of(9, 0), LocalTime.of(10, 0));

            // Act + Assert
            assertAll(
                    () -> assertEquals("ROL_NO_AUTORIZADO",
                            codigo(() -> service.agregarDisponibilidad(null, ID_SERVICIO, request))),
                    () -> assertEquals("ROL_NO_AUTORIZADO",
                            codigo(() -> service.agregarDisponibilidad(sinRol, ID_SERVICIO, request)))
            );
        }

        @Test
        void deberiaRechazarProveedorConCuentaInactivaOSinEstado() {
            // Arrange
            Usuario inactivo = ServicioFixtures.usuario(Rol.PROVEEDOR, EstadoUsuario.INACTIVO);
            Usuario sinEstado = ServicioFixtures.usuario(Rol.PROVEEDOR, null);
            DisponibilidadRequest request = franja("LUNES", LocalTime.of(9, 0), LocalTime.of(10, 0));

            // Act + Assert
            assertAll(
                    () -> assertEquals("PROVEEDOR_NO_ACTIVO",
                            codigo(() -> service.agregarDisponibilidad(inactivo, ID_SERVICIO, request))),
                    () -> assertEquals("PROVEEDOR_NO_ACTIVO",
                            codigo(() -> service.agregarDisponibilidad(sinEstado, ID_SERVICIO, request)))
            );
        }

        @Test
        void deberiaRechazarCuandoElProveedorNoTienePerfil() {
            // Arrange
            when(proveedorRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())).thenReturn(Optional.empty());

            // Act + Assert
            assertEquals("PROVEEDOR_NO_ENCONTRADO", codigo(() -> service.agregarDisponibilidad(
                    usuario, ID_SERVICIO, franja("LUNES", LocalTime.of(9, 0), LocalTime.of(10, 0)))));
        }

        @Test
        void deberiaRechazarCuandoLaOfertaNoExisteOEstaInactiva() {
            // Arrange
            when(ofertaRepository.findByIdServicioAndEstado(77L, "ACTIVO")).thenReturn(Optional.empty());

            // Act + Assert
            assertEquals("OFERTA_NO_DISPONIBLE", codigo(() -> service.agregarDisponibilidad(
                    usuario, 77L, franja("LUNES", LocalTime.of(9, 0), LocalTime.of(10, 0)))));
        }

        @Test
        void deberiaRechazarCuandoElTipoOLaCategoriaDeLaOfertaEstanInactivos() {
            // Arrange
            TipoServicio tipoInactivo = ServicioFixtures.tipo("INACTIVO", ServicioFixtures.categoria("ACTIVO"));
            oferta.setTipoServicio(tipoInactivo);
            DisponibilidadRequest request = franja("LUNES", LocalTime.of(9, 0), LocalTime.of(10, 0));

            // Act + Assert
            assertEquals("OFERTA_NO_DISPONIBLE", codigo(() -> service.agregarDisponibilidad(usuario, ID_SERVICIO, request)));

            oferta.setTipoServicio(ServicioFixtures.tipo("ACTIVO", ServicioFixtures.categoria("INACTIVO")));
            assertEquals("OFERTA_NO_DISPONIBLE", codigo(() -> service.agregarDisponibilidad(usuario, ID_SERVICIO, request)));
        }
    }

    @Nested
    class ListarYEliminarDisponibilidad {

        @Test
        void deberiaListarLasFranjasSemanalesDeUnaOfertaActiva() {
            // Arrange
            when(disponibilidadRepository.findByServicioIdServicio(ID_SERVICIO)).thenReturn(List.of(
                    franjaExistente(1L, oferta, "LUNES", LocalTime.of(9, 0), LocalTime.of(10, 0)),
                    franjaExistente(2L, oferta, "VIERNES", LocalTime.of(14, 0), LocalTime.of(16, 0))));

            // Act
            List<DisponibilidadResponse> franjas = service.listarDisponibilidad(ID_SERVICIO);

            // Assert
            assertAll(
                    () -> assertEquals(2, franjas.size()),
                    () -> assertEquals(1L, franjas.get(0).getId()),
                    () -> assertEquals("VIERNES", franjas.get(1).getDiaSemana()),
                    () -> assertEquals("SEMANAL", franjas.get(1).getTipo())
            );
        }

        @Test
        void deberiaRechazarElListadoDeUnaOfertaInactivaOInexistente() {
            // Arrange
            when(ofertaRepository.findByIdServicioAndEstado(55L, "ACTIVO")).thenReturn(Optional.empty());

            // Act + Assert
            assertEquals("OFERTA_NO_DISPONIBLE", codigo(() -> service.listarDisponibilidad(55L)));
        }

        @Test
        void deberiaEliminarUnaFranjaPropia() {
            // Arrange
            DisponibilidadServicio existente = franjaExistente(
                    5L, oferta, "LUNES", LocalTime.of(9, 0), LocalTime.of(10, 0));
            when(disponibilidadRepository.findById(5L)).thenReturn(Optional.of(existente));

            // Act
            service.eliminarDisponibilidad(usuario, ID_SERVICIO, 5L);

            // Assert
            verify(disponibilidadRepository).delete(existente);
        }

        @Test
        void deberiaRechazarEliminarUnaFranjaInexistente() {
            // Arrange
            when(disponibilidadRepository.findById(5L)).thenReturn(Optional.empty());

            // Act + Assert
            assertEquals("DISPONIBILIDAD_NO_ENCONTRADA",
                    codigo(() -> service.eliminarDisponibilidad(usuario, ID_SERVICIO, 5L)));
        }

        @Test
        void deberiaRechazarEliminarUnaFranjaQuePerteneceAOtraOferta() {
            // Arrange
            OfertaServicio otraOferta = ServicioFixtures.oferta(proveedor, ServicioFixtures.tipoActivo(), "ACTIVO");
            otraOferta.setIdServicio(11L);
            DisponibilidadServicio existente = franjaExistente(
                    5L, otraOferta, "LUNES", LocalTime.of(9, 0), LocalTime.of(10, 0));
            when(disponibilidadRepository.findById(5L)).thenReturn(Optional.of(existente));

            // Act + Assert
            assertEquals("DISPONIBILIDAD_NO_PERTENECE",
                    codigo(() -> service.eliminarDisponibilidad(usuario, ID_SERVICIO, 5L)));
            verify(disponibilidadRepository, never()).delete(any(DisponibilidadServicio.class));
        }

        @Test
        void deberiaRechazarEliminarFranjasDeUnServicioAjeno() {
            // Arrange
            when(ofertaRepository.findByIdServicioAndEstado(ID_SERVICIO, "ACTIVO"))
                    .thenReturn(Optional.of(ofertaDeOtroProveedor()));

            // Act + Assert
            assertEquals("OFERTA_NO_PROPIA", codigo(() -> service.eliminarDisponibilidad(usuario, ID_SERVICIO, 5L)));
            verify(disponibilidadRepository, never()).findById(any());
        }
    }

    @Nested
    class CrearBloqueo {

        private final LocalDate futura = LocalDate.now().plusDays(5);

        @Test
        void deberiaPermitirBloquearHoy() {
            // Arrange
            when(bloqueoRepository.findByServicioIdServicioAndFecha(eq(ID_SERVICIO), any())).thenReturn(List.of());

            // Act
            DisponibilidadResponse respuesta = service.crearBloqueo(
                    usuario, ID_SERVICIO, bloqueoRequest(LocalDate.now(), null, null, null));

            // Assert
            assertEquals(LocalDate.now(), respuesta.getFecha());
        }

        @Test
        void deberiaRegistrarMotivoReservadoSinImportarMayusculas() {
            // Arrange
            when(bloqueoRepository.findByServicioIdServicioAndFecha(ID_SERVICIO, futura)).thenReturn(List.of());

            // Act
            DisponibilidadResponse respuesta = service.crearBloqueo(
                    usuario, ID_SERVICIO, bloqueoRequest(futura, LocalTime.of(9, 0), LocalTime.of(10, 0), " reservado "));

            // Assert
            assertAll(
                    () -> assertEquals("RESERVADO", respuesta.getMotivo()),
                    () -> assertEquals("BLOQUEO", respuesta.getTipo()),
                    () -> assertEquals(futura, respuesta.getFecha()),
                    () -> assertFalse(respuesta.getDiaCompleto())
            );
        }

        @Test
        void deberiaRechazarFechasPasadas() {
            // Act + Assert
            assertEquals("FECHA_BLOQUEO_INVALIDA", codigo(() -> service.crearBloqueo(
                    usuario, ID_SERVICIO, bloqueoRequest(LocalDate.now().minusDays(1), null, null, null))));
            verify(bloqueoRepository, never()).save(any());
        }

        @Test
        void deberiaRechazarMotivoDistintoDeReservadoUOtro() {
            // Act + Assert
            assertEquals("MOTIVO_BLOQUEO_INVALIDO", codigo(() -> service.crearBloqueo(
                    usuario, ID_SERVICIO, bloqueoRequest(futura, null, null, "VACACIONES"))));
        }

        @Test
        void deberiaRechazarSoloLaHoraDeFin() {
            // Act + Assert
            assertEquals("INTERVALO_HORARIO_INVALIDO", codigo(() -> service.crearBloqueo(
                    usuario, ID_SERVICIO, bloqueoRequest(futura, null, LocalTime.of(10, 0), null))));
        }

        @Test
        void deberiaRechazarBloqueoConHoraDeFinAnteriorALaDeInicio() {
            // Act + Assert
            assertEquals("INTERVALO_HORARIO_INVALIDO", codigo(() -> service.crearBloqueo(
                    usuario, ID_SERVICIO, bloqueoRequest(futura, LocalTime.of(11, 0), LocalTime.of(10, 0), null))));
        }

        @Test
        void deberiaRechazarBloqueoParcialQueSeTraslapaConOtroBloqueo() {
            // Arrange
            BloqueoDisponibilidad existente = bloqueoExistente(
                    1L, oferta, futura, LocalTime.of(9, 0), LocalTime.of(11, 0));
            when(bloqueoRepository.findByServicioIdServicioAndFecha(ID_SERVICIO, futura)).thenReturn(List.of(existente));

            // Act + Assert
            assertEquals("BLOQUEO_TRASLAPADO", codigo(() -> service.crearBloqueo(
                    usuario, ID_SERVICIO, bloqueoRequest(futura, LocalTime.of(10, 0), LocalTime.of(12, 0), null))));
            verify(bloqueoRepository, never()).save(any());
        }

        @Test
        void deberiaPermitirBloqueosParcialesContiguosEnLaMismaFecha() {
            // Arrange
            BloqueoDisponibilidad existente = bloqueoExistente(
                    1L, oferta, futura, LocalTime.of(9, 0), LocalTime.of(11, 0));
            when(bloqueoRepository.findByServicioIdServicioAndFecha(ID_SERVICIO, futura)).thenReturn(List.of(existente));

            // Act
            DisponibilidadResponse respuesta = service.crearBloqueo(
                    usuario, ID_SERVICIO, bloqueoRequest(futura, LocalTime.of(11, 0), LocalTime.of(12, 0), null));

            // Assert
            assertEquals(LocalTime.of(11, 0), respuesta.getHoraInicio());
        }

        @Test
        void noDeberiaMarcarComoDiaCompletoUnBloqueoParcial() {
            // Arrange
            when(bloqueoRepository.findByServicioIdServicioAndFecha(ID_SERVICIO, futura)).thenReturn(List.of());

            // Act
            DisponibilidadResponse parcial = service.crearBloqueo(
                    usuario, ID_SERVICIO, bloqueoRequest(futura, LocalTime.MIN, LocalTime.of(12, 0), null));

            // Assert
            assertFalse(parcial.getDiaCompleto());
        }

        @Test
        void deberiaRechazarBloqueoDeUnServicioAjeno() {
            // Arrange
            when(ofertaRepository.findByIdServicioAndEstado(ID_SERVICIO, "ACTIVO"))
                    .thenReturn(Optional.of(ofertaDeOtroProveedor()));

            // Act + Assert
            assertEquals("OFERTA_NO_PROPIA", codigo(() -> service.crearBloqueo(
                    usuario, ID_SERVICIO, bloqueoRequest(futura, null, null, null))));
        }

        @Test
        void deberiaRechazarBloqueoDeUnCliente() {
            // Arrange
            Usuario cliente = ServicioFixtures.usuario(Rol.CLIENTE, EstadoUsuario.ACTIVO);

            // Act + Assert
            assertEquals("ROL_NO_AUTORIZADO", codigo(() -> service.crearBloqueo(
                    cliente, ID_SERVICIO, bloqueoRequest(futura, null, null, null))));
        }
    }

    @Nested
    class ListarBloqueos {

        private final LocalDate desde = LocalDate.of(2030, 1, 1);
        private final LocalDate hasta = LocalDate.of(2030, 1, 31);

        private BloqueoDisponibilidad unBloqueo() {
            return bloqueoExistente(1L, oferta, desde, LocalTime.MIN, LocalTime.of(23, 59, 59));
        }

        @Test
        void deberiaListarTodosLosBloqueosCuandoNoHayRango() {
            // Arrange
            when(bloqueoRepository.findByServicioIdServicio(ID_SERVICIO)).thenReturn(List.of(unBloqueo()));

            // Act
            List<DisponibilidadResponse> bloqueos = service.listarBloqueos(ID_SERVICIO, null, null);

            // Assert
            assertAll(
                    () -> assertEquals(1, bloqueos.size()),
                    () -> assertTrue(bloqueos.get(0).getDiaCompleto())
            );
        }

        @Test
        void deberiaFiltrarPorElRangoCompleto() {
            // Arrange
            when(bloqueoRepository.findByServicioIdServicioAndFechaBetween(ID_SERVICIO, desde, hasta))
                    .thenReturn(List.of(unBloqueo()));

            // Act + Assert
            assertEquals(1, service.listarBloqueos(ID_SERVICIO, desde, hasta).size());
        }

        @Test
        void deberiaFiltrarPorUnaSolaFechaCuandoSoloSeIndicaElInicio() {
            // Arrange
            when(bloqueoRepository.findByServicioIdServicioAndFechaBetween(ID_SERVICIO, desde, desde))
                    .thenReturn(List.of(unBloqueo()));

            // Act + Assert
            assertEquals(1, service.listarBloqueos(ID_SERVICIO, desde, null).size());
        }

        @Test
        void deberiaFiltrarPorUnaSolaFechaCuandoSoloSeIndicaElFin() {
            // Arrange
            when(bloqueoRepository.findByServicioIdServicioAndFechaBetween(ID_SERVICIO, hasta, hasta))
                    .thenReturn(List.of());

            // Act + Assert
            assertTrue(service.listarBloqueos(ID_SERVICIO, null, hasta).isEmpty());
        }

        @Test
        void deberiaRechazarUnRangoDeFechasInvertido() {
            // Act + Assert
            assertEquals("RANGO_FECHAS_INVALIDO", codigo(() -> service.listarBloqueos(ID_SERVICIO, hasta, desde)));
        }

        @Test
        void deberiaRechazarElListadoDeUnaOfertaNoDisponible() {
            // Arrange
            when(ofertaRepository.findByIdServicioAndEstado(55L, "ACTIVO")).thenReturn(Optional.empty());

            // Act + Assert
            assertEquals("OFERTA_NO_DISPONIBLE", codigo(() -> service.listarBloqueos(55L, null, null)));
        }
    }

    @Nested
    class EliminarBloqueo {

        @Test
        void deberiaEliminarUnBloqueoPropio() {
            // Arrange
            BloqueoDisponibilidad existente = bloqueoExistente(
                    8L, oferta, LocalDate.now().plusDays(2), LocalTime.MIN, LocalTime.of(23, 59, 59));
            when(bloqueoRepository.findById(8L)).thenReturn(Optional.of(existente));

            // Act
            service.eliminarBloqueo(usuario, ID_SERVICIO, 8L);

            // Assert
            verify(bloqueoRepository).delete(existente);
        }

        @Test
        void deberiaRechazarUnBloqueoInexistente() {
            // Arrange
            when(bloqueoRepository.findById(8L)).thenReturn(Optional.empty());

            // Act + Assert
            assertEquals("BLOQUEO_NO_ENCONTRADO", codigo(() -> service.eliminarBloqueo(usuario, ID_SERVICIO, 8L)));
        }

        @Test
        void deberiaRechazarUnBloqueoQuePerteneceAOtraOferta() {
            // Arrange
            OfertaServicio otraOferta = ServicioFixtures.oferta(proveedor, ServicioFixtures.tipoActivo(), "ACTIVO");
            otraOferta.setIdServicio(11L);
            BloqueoDisponibilidad existente = bloqueoExistente(
                    8L, otraOferta, LocalDate.now(), LocalTime.MIN, LocalTime.of(23, 59, 59));
            when(bloqueoRepository.findById(8L)).thenReturn(Optional.of(existente));

            // Act + Assert
            assertEquals("BLOQUEO_NO_PERTENECE", codigo(() -> service.eliminarBloqueo(usuario, ID_SERVICIO, 8L)));
            verify(bloqueoRepository, never()).delete(any(BloqueoDisponibilidad.class));
        }

        @Test
        void deberiaRechazarEliminarBloqueosDeUnServicioAjeno() {
            // Arrange
            when(ofertaRepository.findByIdServicioAndEstado(ID_SERVICIO, "ACTIVO"))
                    .thenReturn(Optional.of(ofertaDeOtroProveedor()));

            // Act + Assert
            assertEquals("OFERTA_NO_PROPIA", codigo(() -> service.eliminarBloqueo(usuario, ID_SERVICIO, 8L)));
        }
    }
}
