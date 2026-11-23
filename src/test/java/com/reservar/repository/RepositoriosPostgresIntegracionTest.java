package com.reservar.repository;

import com.reservar.entity.Calificacion;
import com.reservar.entity.CategoriaServicio;
import com.reservar.entity.HistorialReserva;
import com.reservar.entity.OfertaServicio;
import com.reservar.entity.Proveedor;
import com.reservar.entity.Reserva;
import com.reservar.entity.Sesion;
import com.reservar.entity.TipoServicio;
import com.reservar.entity.Usuario;
import com.reservar.entity.enums.EstadoReserva;
import com.reservar.entity.enums.EstadoUsuario;
import com.reservar.entity.enums.Rol;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integración de las consultas propias de los repositorios contra PostgreSQL real (hallazgo S1-H3).
 * Las pruebas unitarias mockean los repositorios, así que solo aquí se ejecutan de verdad el JPQL, ESCAPE,
 * citext, tsrange y la restricción de exclusión. Si no hay Docker disponible, la clase se omite; en CI siempre corre.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class RepositoriosPostgresIntegracionTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withInitScript("db/extensiones.sql");

    @DynamicPropertySource
    static void configurarBaseDeDatos(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        registro.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    private static final LocalDateTime MANANA_9 = LocalDateTime.of(2030, 1, 15, 9, 0);

    @Autowired
    private EntityManager em;
    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private OfertaServicioRepository ofertaRepository;
    @Autowired
    private CalificacionRepository calificacionRepository;
    @Autowired
    private HistorialReservaRepository historialRepository;
    @Autowired
    private SesionRepository sesionRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario cliente;
    private Usuario otroCliente;
    private Usuario usuarioProveedor;
    private CategoriaServicio salud;
    private TipoServicio odontologia;
    private OfertaServicio limpieza;
    private OfertaServicio blanqueamiento;

    @BeforeEach
    void poblar() {
        cliente = usuario("cliente@correo.com", Rol.CLIENTE);
        otroCliente = usuario("otro@correo.com", Rol.CLIENTE);
        usuarioProveedor = usuario("proveedor@correo.com", Rol.PROVEEDOR);
        Proveedor proveedor = new Proveedor();
        proveedor.setUsuario(usuarioProveedor);
        em.persist(proveedor);

        salud = categoria("SALUD", "ACTIVO");
        odontologia = tipo("Odontologia", salud, "ACTIVO");
        limpieza = oferta(proveedor, odontologia, "Limpieza dental", "Remoción de sarro", "ACTIVO");
        blanqueamiento = oferta(proveedor, odontologia, "Blanqueamiento", "Descuento del 50% en sesión", "ACTIVO");
        em.flush();
    }

    // ---------- Datos de prueba ----------

    private Usuario usuario(String correo, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto("Usuario " + correo);
        usuario.setCorreo(correo);
        usuario.setHashContrasena("hash");
        usuario.setRol(rol);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        em.persist(usuario);
        return usuario;
    }

    private CategoriaServicio categoria(String nombre, String estado) {
        CategoriaServicio categoria = new CategoriaServicio();
        categoria.setNombre(nombre);
        categoria.setEstado(estado);
        em.persist(categoria);
        return categoria;
    }

    private TipoServicio tipo(String nombre, CategoriaServicio categoria, String estado) {
        TipoServicio tipo = new TipoServicio();
        tipo.setNombre(nombre);
        tipo.setCategoria(categoria);
        tipo.setEstado(estado);
        em.persist(tipo);
        return tipo;
    }

    private OfertaServicio oferta(Proveedor proveedor, TipoServicio tipo, String nombre, String descripcion, String estado) {
        OfertaServicio oferta = new OfertaServicio();
        oferta.setProveedor(proveedor);
        oferta.setTipoServicio(tipo);
        oferta.setNombre(nombre);
        oferta.setDescripcion(descripcion);
        oferta.setDuracionMinutos(45);
        oferta.setModalidad("REMOTO");
        oferta.setEstado(estado);
        em.persist(oferta);
        return oferta;
    }

    private Reserva reserva(Usuario titular, OfertaServicio oferta, LocalDateTime inicio, int minutos, EstadoReserva estado) {
        Reserva reserva = new Reserva();
        reserva.setCliente(titular);
        reserva.setServicio(oferta);
        reserva.setFechaInicio(inicio);
        reserva.setFechaFin(inicio.plusMinutes(minutos));
        reserva.setEstado(estado);
        em.persist(reserva);
        em.flush();
        return reserva;
    }

    private static final List<EstadoReserva> OCUPAN = List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

    @Nested
    class Reservas {

        @Test
        void deberiaDetectarReservasSuperpuestasSoloConEstadosQueOcupanElHorario() {
            // Arrange: 09:00-09:45 PENDIENTE y 11:00-11:45 CANCELADA.
            reserva(cliente, limpieza, MANANA_9, 45, EstadoReserva.PENDIENTE);
            reserva(cliente, limpieza, MANANA_9.plusHours(2), 45, EstadoReserva.CANCELADA);
            Long id = limpieza.getIdServicio();

            // Act + Assert
            assertAll(
                    () -> assertTrue(reservaRepository.existeReservaSuperpuesta(
                            id, MANANA_9.plusMinutes(30), MANANA_9.plusMinutes(75), OCUPAN), "solapa parcialmente"),
                    () -> assertFalse(reservaRepository.existeReservaSuperpuesta(
                            id, MANANA_9.plusMinutes(45), MANANA_9.plusMinutes(90), OCUPAN), "contigua: [) no solapa"),
                    () -> assertFalse(reservaRepository.existeReservaSuperpuesta(
                            id, MANANA_9.minusMinutes(45), MANANA_9, OCUPAN), "termina justo cuando empieza la otra"),
                    () -> assertFalse(reservaRepository.existeReservaSuperpuesta(
                            id, MANANA_9.plusHours(2), MANANA_9.plusHours(3), OCUPAN), "la cancelada no ocupa"),
                    () -> assertFalse(reservaRepository.existeReservaSuperpuesta(
                            blanqueamiento.getIdServicio(), MANANA_9, MANANA_9.plusHours(1), OCUPAN), "otra oferta")
            );
        }

        @Test
        void deberiaListarLasReservasActivasQueTocanElRango() {
            // Arrange
            Reserva dentro = reserva(cliente, limpieza, MANANA_9, 45, EstadoReserva.CONFIRMADA);
            reserva(cliente, limpieza, MANANA_9.plusDays(10), 45, EstadoReserva.PENDIENTE);
            reserva(cliente, limpieza, MANANA_9.plusHours(3), 45, EstadoReserva.RECHAZADA);

            // Act
            List<Reserva> activas = reservaRepository.findReservasActivasEnRango(limpieza.getIdServicio(), OCUPAN,
                    MANANA_9.toLocalDate().atStartOfDay(), MANANA_9.toLocalDate().plusDays(7).atStartOfDay());

            // Assert
            assertEquals(List.of(dentro.getIdReserva()), activas.stream().map(Reserva::getIdReserva).toList());
        }

        @Test
        void deberiaCompletarSoloLasConfirmadasQueYaTerminaron() {
            // Arrange
            Reserva terminada = reserva(cliente, limpieza, MANANA_9, 45, EstadoReserva.CONFIRMADA);
            Reserva terminaJustoAhora = reserva(cliente, limpieza, MANANA_9.plusHours(1), 60, EstadoReserva.CONFIRMADA);
            Reserva futura = reserva(cliente, limpieza, MANANA_9.plusHours(5), 45, EstadoReserva.CONFIRMADA);
            Reserva pendientePasada = reserva(otroCliente, blanqueamiento, MANANA_9, 45, EstadoReserva.PENDIENTE);
            LocalDateTime ahora = MANANA_9.plusHours(2);

            // Act
            int actualizadas = reservaRepository.completarFinalizadas(
                    EstadoReserva.CONFIRMADA, EstadoReserva.COMPLETADA, ahora);
            em.clear();

            // Assert
            assertAll(
                    () -> assertEquals(2, actualizadas),
                    () -> assertEquals(EstadoReserva.COMPLETADA, estado(terminada)),
                    () -> assertEquals(EstadoReserva.COMPLETADA, estado(terminaJustoAhora)),
                    () -> assertEquals(EstadoReserva.CONFIRMADA, estado(futura)),
                    () -> assertEquals(EstadoReserva.PENDIENTE, estado(pendientePasada)),
                    () -> assertEquals(ahora, reservaRepository.findById(terminada.getIdReserva())
                            .orElseThrow().getFechaActualizacion())
            );
        }

        private EstadoReserva estado(Reserva reserva) {
            return reservaRepository.findById(reserva.getIdReserva()).orElseThrow().getEstado();
        }

        @Test
        void deberiaMostrarElHistorialDelClienteYDelProveedorConFiltroYPaginacion() {
            // Arrange
            reserva(cliente, limpieza, MANANA_9, 45, EstadoReserva.CONFIRMADA);
            reserva(cliente, blanqueamiento, MANANA_9.plusDays(1), 45, EstadoReserva.CANCELADA);
            reserva(otroCliente, limpieza, MANANA_9.plusDays(2), 45, EstadoReserva.PENDIENTE);
            PageRequest pagina = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "fechaInicio"));

            // Act
            Page<Reserva> delCliente = reservaRepository.findHistorialPropio(cliente.getIdUsuario(), null, pagina);
            Page<Reserva> delProveedor = reservaRepository.findHistorialPropio(usuarioProveedor.getIdUsuario(), null, pagina);
            Page<Reserva> canceladasDelCliente = reservaRepository.findHistorialPropio(
                    cliente.getIdUsuario(), EstadoReserva.CANCELADA, pagina);
            Page<Reserva> deUnExtrano = reservaRepository.findHistorialPropio(UUID.randomUUID(), null, pagina);

            // Assert
            assertAll(
                    () -> assertEquals(2, delCliente.getTotalElements()),
                    () -> assertEquals(MANANA_9.plusDays(1), delCliente.getContent().get(0).getFechaInicio(), "orden DESC"),
                    () -> assertEquals(3, delProveedor.getTotalElements(), "el proveedor ve las reservas de sus ofertas"),
                    () -> assertEquals(1, canceladasDelCliente.getTotalElements()),
                    () -> assertEquals(0, deUnExtrano.getTotalElements())
            );
        }

        @Test
        void deberiaBloquearLaReservaParaActualizarla() {
            // Arrange
            Reserva reserva = reserva(cliente, limpieza, MANANA_9, 45, EstadoReserva.PENDIENTE);

            // Act + Assert
            assertEquals(reserva.getIdReserva(),
                    reservaRepository.findByIdParaActualizar(reserva.getIdReserva()).orElseThrow().getIdReserva());
            assertTrue(reservaRepository.findByIdParaActualizar(-1L).isEmpty());
        }

        @Test
        void laRestriccionDeExclusionDeberiaRechazarReservasSolapadasConElCodigo23P01() {
            // Arrange: es la garantía ante concurrencia de HU-11 (ReservaService traduce 23P01 a HORARIO_NO_DISPONIBLE).
            reserva(cliente, limpieza, MANANA_9, 45, EstadoReserva.PENDIENTE);
            reserva(otroCliente, limpieza, MANANA_9.plusMinutes(45), 45, EstadoReserva.CONFIRMADA); // contigua: válida
            reserva(otroCliente, limpieza, MANANA_9, 45, EstadoReserva.CANCELADA); // cancelada: no ocupa
            Reserva solapada = new Reserva();
            solapada.setCliente(otroCliente);
            solapada.setServicio(limpieza);
            solapada.setFechaInicio(MANANA_9.plusMinutes(30));
            solapada.setFechaFin(MANANA_9.plusMinutes(75));
            solapada.setEstado(EstadoReserva.PENDIENTE);

            // Act
            DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,
                    () -> reservaRepository.saveAndFlush(solapada));

            // Assert
            assertEquals("23P01", sqlState(ex));
        }

        private String sqlState(Throwable ex) {
            for (Throwable actual = ex; actual != null; actual = actual.getCause()) {
                if (actual instanceof SQLException sql) {
                    return sql.getSQLState();
                }
            }
            return null;
        }
    }

    @Nested
    class BusquedaDeOfertas {

        private final PageRequest pagina = PageRequest.of(0, 10);

        private List<String> nombres(String texto, Long idCategoria) {
            return ofertaRepository.buscarOfertasActivas(texto, idCategoria, pagina)
                    .map(OfertaServicio::getNombre).getContent();
        }

        @Test
        void deberiaBuscarPorNombreODescripcionSinDistinguirMayusculas() {
            // Act + Assert (el servicio envía el texto ya en minúsculas)
            assertAll(
                    () -> assertEquals(List.of("Limpieza dental"), nombres("limpieza", null)),
                    () -> assertEquals(List.of("Limpieza dental"), nombres("sarro", null)),
                    () -> assertEquals(2, nombres("", null).size())
            );
        }

        @Test
        void deberiaTratarElPorcentajeYElGuionBajoEscapadosComoTextoLiteral() {
            // Act + Assert: regresión del bug #15. "\%" solo coincide con un "%" real.
            assertAll(
                    () -> assertEquals(List.of("Blanqueamiento"), nombres("50\\%", null)),
                    () -> assertEquals(List.of(), nombres("\\%\\%", null)),
                    () -> assertEquals(List.of(), nombres("\\_", null))
            );
        }

        @Test
        void deberiaExcluirOfertasInactivasYDeCatalogoInactivoYFiltrarPorCategoria() {
            // Arrange
            CategoriaServicio deporte = categoria("DEPORTE", "ACTIVO");
            TipoServicio yoga = tipo("Yoga", deporte, "ACTIVO");
            Proveedor proveedor = limpieza.getProveedor();
            oferta(proveedor, yoga, "Yoga matutino", "Clase", "ACTIVO");
            oferta(proveedor, yoga, "Yoga inactivo", "Clase", "INACTIVO");
            TipoServicio pilates = tipo("Pilates", deporte, "INACTIVO");
            oferta(proveedor, pilates, "Pilates", "Clase", "ACTIVO");
            em.flush();

            // Act + Assert
            assertAll(
                    () -> assertEquals(List.of("Yoga matutino"), nombres("", deporte.getIdCategoria())),
                    () -> assertEquals(2, nombres("", salud.getIdCategoria()).size()),
                    () -> assertEquals(3, nombres("", null).size())
            );
        }
    }

    @Nested
    class OtrasConsultas {

        @Test
        void deberiaResumirPromedioYCantidadDeCalificacionesPorOferta() {
            // Arrange
            calificar(reserva(cliente, limpieza, MANANA_9, 45, EstadoReserva.COMPLETADA), 5);
            calificar(reserva(otroCliente, limpieza, MANANA_9.plusHours(1), 45, EstadoReserva.COMPLETADA), 4);

            // Act
            CalificacionRepository.Resumen conCalificaciones = calificacionRepository.resumirPorServicio(limpieza.getIdServicio());
            CalificacionRepository.Resumen sinCalificaciones = calificacionRepository.resumirPorServicio(blanqueamiento.getIdServicio());

            // Assert
            assertAll(
                    () -> assertEquals(4.5, conCalificaciones.getPromedio()),
                    () -> assertEquals(2L, conCalificaciones.getCantidad()),
                    () -> assertNull(sinCalificaciones.getPromedio()),
                    () -> assertEquals(0L, sinCalificaciones.getCantidad()),
                    () -> assertTrue(calificacionRepository.findByReservaIdReserva(-1L).isEmpty())
            );
        }

        private void calificar(Reserva reserva, int estrellas) {
            Calificacion calificacion = new Calificacion();
            calificacion.setReserva(reserva);
            calificacion.setEstrellas((short) estrellas);
            em.persist(calificacion);
            em.flush();
        }

        @Test
        void deberiaDevolverLaAuditoriaEnOrdenCronologico() {
            // Arrange: se insertan en desorden.
            Reserva reserva = reserva(cliente, limpieza, MANANA_9, 45, EstadoReserva.CONFIRMADA);
            evento(reserva, EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA, MANANA_9.minusHours(1));
            evento(reserva, null, EstadoReserva.PENDIENTE, MANANA_9.minusDays(1));

            // Act
            List<HistorialReserva> eventos = historialRepository
                    .findByReservaIdReservaOrderByFechaCambioAscIdHistorialAsc(reserva.getIdReserva());

            // Assert
            assertEquals(List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA),
                    eventos.stream().map(HistorialReserva::getEstadoNuevo).toList());
        }

        private void evento(Reserva reserva, EstadoReserva anterior, EstadoReserva nuevo, LocalDateTime fecha) {
            HistorialReserva historial = new HistorialReserva();
            historial.setReserva(reserva);
            historial.setActor(cliente);
            historial.setEstadoAnterior(anterior);
            historial.setEstadoNuevo(nuevo);
            historial.setFechaCambio(fecha);
            em.persist(historial);
            em.flush();
        }

        @Test
        void deberiaAceptarSoloSesionesVigentesYNoRevocadas() {
            // Arrange
            LocalDateTime ahora = LocalDateTime.of(2030, 1, 1, 12, 0);
            sesion("vigente", ahora.plusHours(1), null);
            sesion("expirada", ahora.minusMinutes(1), null);
            sesion("revocada", ahora.plusHours(1), ahora.minusMinutes(5));

            // Act + Assert
            assertAll(
                    () -> assertEquals(cliente.getIdUsuario(), sesionRepository
                            .findByHashTokenAndFechaRevocacionIsNullAndFechaExpiracionAfter("vigente", ahora)
                            .orElseThrow().getUsuario().getIdUsuario()),
                    () -> assertTrue(sesionRepository
                            .findByHashTokenAndFechaRevocacionIsNullAndFechaExpiracionAfter("expirada", ahora).isEmpty()),
                    () -> assertTrue(sesionRepository
                            .findByHashTokenAndFechaRevocacionIsNullAndFechaExpiracionAfter("revocada", ahora).isEmpty())
            );
        }

        private void sesion(String hash, LocalDateTime expiracion, LocalDateTime revocacion) {
            Sesion sesion = new Sesion();
            sesion.setUsuario(cliente);
            sesion.setHashToken(hash);
            sesion.setFechaExpiracion(expiracion);
            sesion.setFechaRevocacion(revocacion);
            sesion.setIpOrigen("192.168.0.10");
            em.persist(sesion);
            em.flush();
        }

        @Test
        void deberiaEncontrarElCorreoSinDistinguirMayusculasGraciasACitext() {
            // Act + Assert: HU-02 inicio de sesión y HU-01 correo único.
            assertAll(
                    () -> assertEquals(cliente.getIdUsuario(),
                            usuarioRepository.findByCorreo("CLIENTE@Correo.COM").orElseThrow().getIdUsuario()),
                    () -> assertTrue(usuarioRepository.existsByCorreo("Proveedor@correo.com")),
                    () -> assertFalse(usuarioRepository.existsByCorreo("nadie@correo.com"))
            );
        }
    }
}
