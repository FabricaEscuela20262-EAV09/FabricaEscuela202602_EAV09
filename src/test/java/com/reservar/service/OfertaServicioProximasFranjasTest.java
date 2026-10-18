package com.reservar.service;

import com.reservar.dto.servicio.DisponibilidadResponse;
import com.reservar.dto.servicio.OfertaServicioResponse;
import com.reservar.entity.BloqueoDisponibilidad;
import com.reservar.entity.DisponibilidadServicio;
import com.reservar.entity.OfertaServicio;
import com.reservar.entity.TipoServicio;
import com.reservar.repository.BloqueoDisponibilidadRepository;
import com.reservar.repository.CategoriaServicioRepository;
import com.reservar.repository.DisponibilidadServicioRepository;
import com.reservar.repository.OfertaServicioRepository;
import com.reservar.repository.ProveedorRepository;
import com.reservar.repository.TipoServicioRepository;
import com.reservar.support.ServicioFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.reservar.support.ServicioFixtures.ID_SERVICIO;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * HU-10: las próximas franjas del detalle se calculan a partir de la disponibilidad semanal (HU-08)
 * descontando las fechas bloqueadas. La ventana es de 7 días contando hoy.
 */
@ExtendWith(MockitoExtension.class)
class OfertaServicioProximasFranjasTest {

    private static final String[] DIAS = {
            "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"};

    @Mock
    private OfertaServicioRepository ofertaRepository;
    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private TipoServicioRepository tipoServicioRepository;
    @Mock
    private CategoriaServicioRepository categoriaRepository;
    @Mock
    private DisponibilidadServicioRepository disponibilidadRepository;
    @Mock
    private BloqueoDisponibilidadRepository bloqueoRepository;

    @InjectMocks
    private OfertaServicioService service;

    private LocalDate hoy;

    @BeforeEach
    void setUp() {
        hoy = LocalDate.now();
        TipoServicio tipo = ServicioFixtures.tipoActivo();
        OfertaServicio oferta = ServicioFixtures.oferta(
                ServicioFixtures.proveedor(ServicioFixtures.ID_PROVEEDOR, ServicioFixtures.proveedorActivo()),
                tipo, "ACTIVO");
        when(ofertaRepository.findByIdServicioAndEstado(ID_SERVICIO, "ACTIVO")).thenReturn(Optional.of(oferta));
    }

    private static String nombreDia(LocalDate fecha) {
        return DIAS[fecha.getDayOfWeek().getValue() - 1];
    }

    private static DisponibilidadServicio horario(String dia, LocalTime inicio, LocalTime fin) {
        DisponibilidadServicio horario = new DisponibilidadServicio();
        horario.setDiaSemana(dia);
        horario.setHoraInicio(inicio);
        horario.setHoraFin(fin);
        return horario;
    }

    private static BloqueoDisponibilidad bloqueo(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        BloqueoDisponibilidad bloqueo = new BloqueoDisponibilidad();
        bloqueo.setFecha(fecha);
        bloqueo.setHoraInicio(inicio);
        bloqueo.setHoraFin(fin);
        bloqueo.setMotivo("OTRO");
        return bloqueo;
    }

    private List<DisponibilidadServicio> mismoHorarioTodosLosDias(LocalTime inicio, LocalTime fin) {
        List<DisponibilidadServicio> horarios = new ArrayList<>();
        for (String dia : DIAS) {
            horarios.add(horario(dia, inicio, fin));
        }
        return horarios;
    }

    private void configurar(List<DisponibilidadServicio> horarios, List<BloqueoDisponibilidad> bloqueos) {
        when(disponibilidadRepository.findByServicioIdServicio(ID_SERVICIO)).thenReturn(horarios);
        when(bloqueoRepository.findByServicioIdServicioAndFechaBetween(eq(ID_SERVICIO), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(bloqueos);
    }

    private List<DisponibilidadResponse> franjasDeFecha(OfertaServicioResponse detalle, LocalDate fecha) {
        return detalle.getProximasDisponibilidades().stream()
                .filter(f -> f.getFecha().equals(fecha))
                .toList();
    }

    private List<DisponibilidadResponse> franjasFuturas(OfertaServicioResponse detalle) {
        return detalle.getProximasDisponibilidades().stream()
                .filter(f -> f.getFecha().isAfter(hoy))
                .toList();
    }

    /** Evita resultados inestables cuando la prueba corre justo alrededor de la medianoche. */
    private static void assumeLejosDeMedianoche() {
        LocalTime ahora = LocalTime.now();
        assumeTrue(ahora.isAfter(LocalTime.of(0, 5)) && ahora.isBefore(LocalTime.of(23, 50)));
    }

    @Test
    void deberiaOfrecerLaFranjaSemanalEnCadaUnoDeLosSeisDiasSiguientes() {
        // Arrange
        configurar(mismoHorarioTodosLosDias(LocalTime.of(9, 0), LocalTime.of(12, 0)), List.of());

        // Act
        OfertaServicioResponse detalle = service.obtenerDetalle(ID_SERVICIO);

        // Assert
        List<DisponibilidadResponse> futuras = franjasFuturas(detalle);
        assertEquals(6, futuras.size());
        for (int i = 0; i < futuras.size(); i++) {
            LocalDate esperada = hoy.plusDays(i + 1L);
            DisponibilidadResponse franja = futuras.get(i);
            assertAll(
                    () -> assertEquals(esperada, franja.getFecha()),
                    () -> assertEquals(nombreDia(esperada), franja.getDiaSemana()),
                    () -> assertEquals("DISPONIBLE", franja.getTipo()),
                    () -> assertEquals(ID_SERVICIO, franja.getIdServicio()),
                    () -> assertEquals(LocalTime.of(9, 0), franja.getHoraInicio()),
                    () -> assertEquals(LocalTime.of(12, 0), franja.getHoraFin())
            );
        }
    }

    @Test
    void deberiaMapearCorrectamenteLosSieteDiasDeLaSemana() {
        // Arrange: un horario distinto (hora de inicio) para cada día de la semana
        List<DisponibilidadServicio> horarios = new ArrayList<>();
        for (int i = 0; i < DIAS.length; i++) {
            horarios.add(horario(DIAS[i], LocalTime.of(8 + i, 0), LocalTime.of(8 + i, 30)));
        }
        configurar(horarios, List.of());

        // Act
        OfertaServicioResponse detalle = service.obtenerDetalle(ID_SERVICIO);

        // Assert
        for (DisponibilidadResponse franja : franjasFuturas(detalle)) {
            DayOfWeek dia = franja.getFecha().getDayOfWeek();
            assertAll(
                    () -> assertEquals(DIAS[dia.getValue() - 1], franja.getDiaSemana()),
                    () -> assertEquals(LocalTime.of(8 + dia.getValue() - 1, 0), franja.getHoraInicio())
            );
        }
    }

    @Test
    void deberiaOfrecerSoloLosDiasConHorarioConfigurado() {
        // Arrange: solo mañana tiene horario
        LocalDate manana = hoy.plusDays(1);
        configurar(List.of(horario(nombreDia(manana), LocalTime.of(10, 0), LocalTime.of(11, 0))), List.of());

        // Act
        OfertaServicioResponse detalle = service.obtenerDetalle(ID_SERVICIO);

        // Assert
        List<DisponibilidadResponse> futuras = franjasFuturas(detalle);
        assertAll(
                () -> assertEquals(1, futuras.size()),
                () -> assertEquals(manana, futuras.get(0).getFecha())
        );
    }

    @Test
    void deberiaRetornarListaVaciaCuandoElServicioNoTieneDisponibilidadConfigurada() {
        // Arrange
        configurar(List.of(), List.of());

        // Act
        OfertaServicioResponse detalle = service.obtenerDetalle(ID_SERVICIO);

        // Assert
        assertTrue(detalle.getProximasDisponibilidades().isEmpty());
    }

    @Test
    void deberiaExcluirLaFechaBloqueadaTodoElDia() {
        // Arrange
        LocalDate bloqueada = hoy.plusDays(2);
        configurar(mismoHorarioTodosLosDias(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                List.of(bloqueo(bloqueada, LocalTime.MIN, LocalTime.of(23, 59, 59))));

        // Act
        OfertaServicioResponse detalle = service.obtenerDetalle(ID_SERVICIO);

        // Assert
        assertAll(
                () -> assertTrue(franjasDeFecha(detalle, bloqueada).isEmpty()),
                () -> assertEquals(5, franjasFuturas(detalle).size())
        );
    }

    @Test
    void deberiaPartirLaFranjaCuandoElBloqueoEstaEnElMedio() {
        // Arrange
        LocalDate fecha = hoy.plusDays(1);
        configurar(mismoHorarioTodosLosDias(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                List.of(bloqueo(fecha, LocalTime.of(10, 0), LocalTime.of(11, 0))));

        // Act
        List<DisponibilidadResponse> franjas = franjasDeFecha(service.obtenerDetalle(ID_SERVICIO), fecha);

        // Assert
        assertAll(
                () -> assertEquals(2, franjas.size()),
                () -> assertEquals(LocalTime.of(9, 0), franjas.get(0).getHoraInicio()),
                () -> assertEquals(LocalTime.of(10, 0), franjas.get(0).getHoraFin()),
                () -> assertEquals(LocalTime.of(11, 0), franjas.get(1).getHoraInicio()),
                () -> assertEquals(LocalTime.of(12, 0), franjas.get(1).getHoraFin())
        );
    }

    @Test
    void deberiaConservarSoloLaParteFinalCuandoElBloqueoCubreElInicioDeLaFranja() {
        // Arrange
        LocalDate fecha = hoy.plusDays(1);
        configurar(mismoHorarioTodosLosDias(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                List.of(bloqueo(fecha, LocalTime.of(8, 0), LocalTime.of(10, 0))));

        // Act
        List<DisponibilidadResponse> franjas = franjasDeFecha(service.obtenerDetalle(ID_SERVICIO), fecha);

        // Assert
        assertAll(
                () -> assertEquals(1, franjas.size()),
                () -> assertEquals(LocalTime.of(10, 0), franjas.get(0).getHoraInicio()),
                () -> assertEquals(LocalTime.of(12, 0), franjas.get(0).getHoraFin())
        );
    }

    @Test
    void deberiaConservarSoloLaParteInicialCuandoElBloqueoCubreElFinalDeLaFranja() {
        // Arrange
        LocalDate fecha = hoy.plusDays(1);
        configurar(mismoHorarioTodosLosDias(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                List.of(bloqueo(fecha, LocalTime.of(11, 0), LocalTime.of(13, 0))));

        // Act
        List<DisponibilidadResponse> franjas = franjasDeFecha(service.obtenerDetalle(ID_SERVICIO), fecha);

        // Assert
        assertAll(
                () -> assertEquals(1, franjas.size()),
                () -> assertEquals(LocalTime.of(9, 0), franjas.get(0).getHoraInicio()),
                () -> assertEquals(LocalTime.of(11, 0), franjas.get(0).getHoraFin())
        );
    }

    @Test
    void deberiaIgnorarUnBloqueoQueNoSeCruzaConLaFranja() {
        // Arrange: bloqueo posterior y bloqueo anterior (contiguos) a la franja 09:00-12:00
        LocalDate fecha = hoy.plusDays(1);
        configurar(mismoHorarioTodosLosDias(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                List.of(bloqueo(fecha, LocalTime.of(12, 0), LocalTime.of(14, 0)),
                        bloqueo(fecha, LocalTime.of(7, 0), LocalTime.of(9, 0))));

        // Act
        List<DisponibilidadResponse> franjas = franjasDeFecha(service.obtenerDetalle(ID_SERVICIO), fecha);

        // Assert
        assertAll(
                () -> assertEquals(1, franjas.size()),
                () -> assertEquals(LocalTime.of(9, 0), franjas.get(0).getHoraInicio()),
                () -> assertEquals(LocalTime.of(12, 0), franjas.get(0).getHoraFin())
        );
    }

    @Test
    void deberiaDescontarVariosBloqueosDeLaMismaFecha() {
        // Arrange
        LocalDate fecha = hoy.plusDays(1);
        configurar(mismoHorarioTodosLosDias(LocalTime.of(9, 0), LocalTime.of(18, 0)),
                List.of(bloqueo(fecha, LocalTime.of(10, 0), LocalTime.of(11, 0)),
                        bloqueo(fecha, LocalTime.of(14, 0), LocalTime.of(15, 0))));

        // Act
        List<DisponibilidadResponse> franjas = franjasDeFecha(service.obtenerDetalle(ID_SERVICIO), fecha);

        // Assert
        assertEquals(3, franjas.size());
        assertAll(
                () -> assertEquals(LocalTime.of(9, 0), franjas.get(0).getHoraInicio()),
                () -> assertEquals(LocalTime.of(10, 0), franjas.get(0).getHoraFin()),
                () -> assertEquals(LocalTime.of(11, 0), franjas.get(1).getHoraInicio()),
                () -> assertEquals(LocalTime.of(14, 0), franjas.get(1).getHoraFin()),
                () -> assertEquals(LocalTime.of(15, 0), franjas.get(2).getHoraInicio()),
                () -> assertEquals(LocalTime.of(18, 0), franjas.get(2).getHoraFin())
        );
    }

    @Test
    void deberiaOrdenarLasFranjasPorFechaYHoraDeInicio() {
        // Arrange: dos franjas por día registradas en orden inverso
        List<DisponibilidadServicio> horarios = new ArrayList<>();
        for (String dia : DIAS) {
            horarios.add(horario(dia, LocalTime.of(15, 0), LocalTime.of(17, 0)));
            horarios.add(horario(dia, LocalTime.of(8, 0), LocalTime.of(10, 0)));
        }
        configurar(horarios, List.of());

        // Act
        List<DisponibilidadResponse> futuras = franjasFuturas(service.obtenerDetalle(ID_SERVICIO));

        // Assert
        assertEquals(12, futuras.size());
        for (int i = 0; i < futuras.size() - 1; i++) {
            DisponibilidadResponse actual = futuras.get(i);
            DisponibilidadResponse siguiente = futuras.get(i + 1);
            boolean ordenada = actual.getFecha().isBefore(siguiente.getFecha())
                    || (actual.getFecha().equals(siguiente.getFecha())
                    && actual.getHoraInicio().isBefore(siguiente.getHoraInicio()));
            assertTrue(ordenada, "Las franjas deben estar ordenadas por fecha y hora");
        }
    }

    @Test
    void noDeberiaOfrecerHorasQueYaPasaronEnElDiaActual() {
        // Arrange
        assumeLejosDeMedianoche();
        configurar(List.of(horario(nombreDia(hoy), LocalTime.MIN, LocalTime.of(23, 59))), List.of());

        // Act
        List<DisponibilidadResponse> deHoy = franjasDeFecha(service.obtenerDetalle(ID_SERVICIO), hoy);

        // Assert
        assertEquals(1, deHoy.size());
        assertTrue(deHoy.get(0).getHoraInicio().isAfter(LocalTime.MIN));
        assertEquals(LocalTime.of(23, 59), deHoy.get(0).getHoraFin());
    }

    @Test
    void deberiaConservarLaHoraDeInicioCuandoLaFranjaDeHoyAunNoComienza() {
        // Arrange
        assumeLejosDeMedianoche();
        configurar(List.of(horario(nombreDia(hoy), LocalTime.of(23, 55), LocalTime.of(23, 59))), List.of());

        // Act
        List<DisponibilidadResponse> deHoy = franjasDeFecha(service.obtenerDetalle(ID_SERVICIO), hoy);

        // Assert
        assertEquals(1, deHoy.size());
        assertEquals(LocalTime.of(23, 55), deHoy.get(0).getHoraInicio());
    }

    @Test
    void noDeberiaOfrecerLaFranjaDeHoyCuandoYaTermino() {
        // Arrange
        assumeLejosDeMedianoche();
        configurar(List.of(horario(nombreDia(hoy), LocalTime.MIN, LocalTime.of(0, 1))), List.of());

        // Act
        OfertaServicioResponse detalle = service.obtenerDetalle(ID_SERVICIO);

        // Assert
        assertTrue(franjasDeFecha(detalle, hoy).isEmpty());
    }

    @Test
    void deberiaIgnorarBloqueosDeOtrasFechasAlCalcularUnDia() {
        // Arrange: el bloqueo es del día 3, no debe afectar al día 1
        LocalDate fechaDia1 = hoy.plusDays(1);
        configurar(mismoHorarioTodosLosDias(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                List.of(bloqueo(hoy.plusDays(3), LocalTime.of(9, 0), LocalTime.of(10, 0))));

        // Act
        List<DisponibilidadResponse> franjas = franjasDeFecha(service.obtenerDetalle(ID_SERVICIO), fechaDia1);

        // Assert
        assertEquals(1, franjas.size());
    }
}
