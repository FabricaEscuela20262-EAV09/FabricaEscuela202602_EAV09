package com.reservar.service;

import com.reservar.dto.servicio.ActualizarOfertaRequest;
import com.reservar.dto.servicio.BusquedaOfertasResponse;
import com.reservar.dto.servicio.CrearOfertaRequest;
import com.reservar.dto.servicio.OfertaServicioResponse;
import com.reservar.entity.CategoriaServicio;
import com.reservar.entity.OfertaServicio;
import com.reservar.entity.Proveedor;
import com.reservar.entity.TipoServicio;
import com.reservar.entity.Usuario;
import com.reservar.entity.enums.EstadoUsuario;
import com.reservar.entity.enums.Rol;
import com.reservar.exception.BusinessException;
import com.reservar.repository.BloqueoDisponibilidadRepository;
import com.reservar.repository.CategoriaServicioRepository;
import com.reservar.repository.DisponibilidadServicioRepository;
import com.reservar.repository.OfertaServicioRepository;
import com.reservar.repository.ProveedorRepository;
import com.reservar.repository.TipoServicioRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.reservar.support.ServicioFixtures.ID_CATEGORIA;
import static com.reservar.support.ServicioFixtures.ID_SERVICIO;
import static com.reservar.support.ServicioFixtures.ID_TIPO;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HU-06 (publicación), HU-07 (edición/desactivación) y HU-09 (búsqueda) a nivel de servicio.
 */
@ExtendWith(MockitoExtension.class)
class OfertaServicioGestionTest {

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

    private Usuario usuario;
    private Proveedor proveedor;
    private TipoServicio tipo;

    @BeforeEach
    void setUp() {
        usuario = ServicioFixtures.proveedorActivo();
        proveedor = ServicioFixtures.proveedor(ServicioFixtures.ID_PROVEEDOR, usuario);
        tipo = ServicioFixtures.tipoActivo();

        lenient().when(proveedorRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()))
                .thenReturn(Optional.of(proveedor));
        lenient().when(tipoServicioRepository.findById(ID_TIPO)).thenReturn(Optional.of(tipo));
        lenient().when(ofertaRepository.save(any(OfertaServicio.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private CrearOfertaRequest crearRequest() {
        CrearOfertaRequest request = new CrearOfertaRequest();
        request.setIdTipoServicio(ID_TIPO);
        request.setNombre("  Limpieza dental  ");
        request.setDescripcion("  Limpieza completa  ");
        request.setDuracionMinutos(45);
        request.setModalidad("presencial");
        request.setUbicacion("  Villavicencio  ");
        return request;
    }

    private ActualizarOfertaRequest actualizarRequest() {
        ActualizarOfertaRequest request = new ActualizarOfertaRequest();
        request.setIdTipoServicio(ID_TIPO);
        request.setNombre("Blanqueamiento");
        request.setDescripcion("Blanqueamiento dental");
        request.setDuracionMinutos(60);
        request.setModalidad("REMOTO");
        request.setUbicacion(null);
        return request;
    }

    private OfertaServicio ofertaPropia(String estado) {
        return ServicioFixtures.oferta(proveedor, tipo, estado);
    }

    private static String codigo(Runnable accion) {
        return assertThrows(BusinessException.class, accion::run).getCodigo();
    }

    @Nested
    class CrearOferta {

        @Test
        void deberiaPublicarOfertaActivaNormalizandoLosDatosCuandoElProveedorEstaActivo() {
            // Act
            OfertaServicioResponse respuesta = service.crearOferta(usuario, crearRequest());

            // Assert
            ArgumentCaptor<OfertaServicio> captor = ArgumentCaptor.forClass(OfertaServicio.class);
            verify(ofertaRepository).save(captor.capture());
            OfertaServicio guardada = captor.getValue();
            assertAll(
                    () -> assertEquals("ACTIVO", guardada.getEstado()),
                    () -> assertSame(proveedor, guardada.getProveedor()),
                    () -> assertSame(tipo, guardada.getTipoServicio()),
                    () -> assertEquals("Limpieza dental", guardada.getNombre()),
                    () -> assertEquals("Limpieza completa", guardada.getDescripcion()),
                    () -> assertEquals("PRESENCIAL", guardada.getModalidad()),
                    () -> assertEquals("Villavicencio", guardada.getUbicacion()),
                    () -> assertEquals("ACTIVO", respuesta.getEstado()),
                    () -> assertEquals(ID_CATEGORIA, respuesta.getIdCategoria()),
                    () -> assertEquals("SALUD", respuesta.getNombreCategoria()),
                    () -> assertEquals("Odontologia", respuesta.getNombreTipoServicio()),
                    () -> assertEquals("Ana Proveedora", respuesta.getNombreProveedor()),
                    () -> assertEquals(45, respuesta.getDuracionMinutos())
            );
        }

        @Test
        void deberiaPermitirOfertaRemotaSinUbicacion() {
            // Arrange
            CrearOfertaRequest request = crearRequest();
            request.setModalidad("remoto");
            request.setUbicacion("   ");

            // Act
            OfertaServicioResponse respuesta = service.crearOferta(usuario, request);

            // Assert
            assertAll(
                    () -> assertEquals("REMOTO", respuesta.getModalidad()),
                    () -> assertNull(respuesta.getUbicacion())
            );
        }

        @Test
        void deberiaRechazarCuandoNoHayUsuarioAutenticado() {
            // Act + Assert
            assertEquals("USUARIO_NO_AUTENTICADO", codigo(() -> service.crearOferta(null, crearRequest())));
            verify(ofertaRepository, never()).save(any());
        }

        @Test
        void deberiaRechazarConRolNoAutorizadoCuandoElUsuarioEsCliente() {
            // Arrange
            Usuario cliente = ServicioFixtures.usuario(Rol.CLIENTE, EstadoUsuario.ACTIVO);

            // Act + Assert
            assertEquals("ROL_NO_AUTORIZADO", codigo(() -> service.crearOferta(cliente, crearRequest())));
            verify(ofertaRepository, never()).save(any());
        }

        @Test
        void deberiaRechazarConRolNoAutorizadoCuandoElUsuarioNoTieneRol() {
            // Arrange
            Usuario sinRol = ServicioFixtures.usuario(null, EstadoUsuario.ACTIVO);

            // Act + Assert
            assertEquals("ROL_NO_AUTORIZADO", codigo(() -> service.crearOferta(sinRol, crearRequest())));
        }

        @ParameterizedTest
        @ValueSource(strings = {"PENDIENTE", "INACTIVO"})
        void deberiaRechazarProveedorConCuentaNoVerificadaOInactiva(String estado) {
            Usuario noActivo = ServicioFixtures.usuario(Rol.PROVEEDOR, EstadoUsuario.valueOf(estado));

            assertEquals("PROVEEDOR_NO_ACTIVO", codigo(() -> service.crearOferta(noActivo, crearRequest())));
            verify(ofertaRepository, never()).save(any());
        }

        @Test
        void deberiaRechazarProveedorSinEstado() {
            // Arrange
            Usuario sinEstado = ServicioFixtures.usuario(Rol.PROVEEDOR, null);

            // Act + Assert
            assertEquals("PROVEEDOR_NO_ACTIVO", codigo(() -> service.crearOferta(sinEstado, crearRequest())));
        }

        @Test
        void deberiaRechazarCuandoNoExistePerfilDeProveedor() {
            // Arrange
            when(proveedorRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())).thenReturn(Optional.empty());

            // Act + Assert
            assertEquals("PROVEEDOR_NO_ENCONTRADO", codigo(() -> service.crearOferta(usuario, crearRequest())));
        }

        @Test
        void deberiaRechazarCuandoLaCategoriaNoPerteneceALaListaPredefinida() {
            // Arrange
            when(tipoServicioRepository.findById(ID_TIPO)).thenReturn(Optional.empty());

            // Act + Assert
            assertEquals("TIPO_SERVICIO_NO_ENCONTRADO", codigo(() -> service.crearOferta(usuario, crearRequest())));
            verify(ofertaRepository, never()).save(any());
        }

        @Test
        void deberiaRechazarCuandoElTipoDeServicioEstaInactivo() {
            // Arrange
            tipo.setEstado("INACTIVO");

            // Act + Assert
            assertEquals("TIPO_SERVICIO_INACTIVO", codigo(() -> service.crearOferta(usuario, crearRequest())));
        }

        @Test
        void deberiaRechazarCuandoLaCategoriaDelTipoEstaInactiva() {
            // Arrange
            tipo.getCategoria().setEstado("INACTIVO");

            // Act + Assert
            assertEquals("CATEGORIA_INACTIVA", codigo(() -> service.crearOferta(usuario, crearRequest())));
        }

        @Test
        void deberiaRechazarCuandoElTipoNoTieneCategoria() {
            // Arrange
            tipo.setCategoria(null);

            // Act + Assert
            assertEquals("CATEGORIA_INACTIVA", codigo(() -> service.crearOferta(usuario, crearRequest())));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"HIBRIDA", "virtual"})
        void deberiaRechazarModalidadFueraDePresencialORemoto(String modalidad) {
            CrearOfertaRequest request = crearRequest();
            request.setModalidad(modalidad);

            assertEquals("MODALIDAD_INVALIDA", codigo(() -> service.crearOferta(usuario, request)));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void deberiaRechazarOfertaPresencialSinUbicacion(String ubicacion) {
            CrearOfertaRequest request = crearRequest();
            request.setUbicacion(ubicacion);

            assertEquals("UBICACION_REQUERIDA", codigo(() -> service.crearOferta(usuario, request)));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void deberiaRechazarNombreVacio(String nombre) {
            CrearOfertaRequest request = crearRequest();
            request.setNombre(nombre);

            assertEquals("NOMBRE_REQUERIDO", codigo(() -> service.crearOferta(usuario, request)));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void deberiaRechazarDescripcionVacia(String descripcion) {
            CrearOfertaRequest request = crearRequest();
            request.setDescripcion(descripcion);

            assertEquals("DESCRIPCION_REQUERIDA", codigo(() -> service.crearOferta(usuario, request)));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -5})
        void deberiaRechazarDuracionNoPositiva(int duracion) {
            CrearOfertaRequest request = crearRequest();
            request.setDuracionMinutos(duracion);

            assertEquals("DURACION_INVALIDA", codigo(() -> service.crearOferta(usuario, request)));
        }

        @Test
        void deberiaRechazarDuracionNula() {
            // Arrange
            CrearOfertaRequest request = crearRequest();
            request.setDuracionMinutos(null);

            // Act + Assert
            assertEquals("DURACION_INVALIDA", codigo(() -> service.crearOferta(usuario, request)));
        }
    }

    @Nested
    class ActualizarOferta {

        @Test
        void deberiaActualizarLosDatosDeUnaOfertaPropia() {
            // Arrange
            OfertaServicio existente = ofertaPropia("ACTIVO");
            when(ofertaRepository.findById(ID_SERVICIO)).thenReturn(Optional.of(existente));

            // Act
            OfertaServicioResponse respuesta = service.actualizarOferta(usuario, ID_SERVICIO, actualizarRequest());

            // Assert
            assertAll(
                    () -> assertEquals("Blanqueamiento", existente.getNombre()),
                    () -> assertEquals("Blanqueamiento dental", existente.getDescripcion()),
                    () -> assertEquals(60, existente.getDuracionMinutos()),
                    () -> assertEquals("REMOTO", existente.getModalidad()),
                    () -> assertNull(existente.getUbicacion()),
                    () -> assertEquals("Blanqueamiento", respuesta.getNombre()),
                    () -> assertEquals(60, respuesta.getDuracionMinutos())
            );
            verify(ofertaRepository).save(existente);
        }

        @Test
        void deberiaCambiarLaCategoriaCuandoSeIndicaOtroTipoDeServicio() {
            // Arrange
            CategoriaServicio otraCategoria = ServicioFixtures.categoria("ACTIVO");
            otraCategoria.setIdCategoria(7L);
            TipoServicio otroTipo = ServicioFixtures.tipo("ACTIVO", otraCategoria);
            otroTipo.setIdTipoServicio(8L);
            ActualizarOfertaRequest request = actualizarRequest();
            request.setIdTipoServicio(8L);
            OfertaServicio existente = ofertaPropia("ACTIVO");
            when(ofertaRepository.findById(ID_SERVICIO)).thenReturn(Optional.of(existente));
            when(tipoServicioRepository.findById(8L)).thenReturn(Optional.of(otroTipo));

            // Act
            OfertaServicioResponse respuesta = service.actualizarOferta(usuario, ID_SERVICIO, request);

            // Assert
            assertAll(
                    () -> assertSame(otroTipo, existente.getTipoServicio()),
                    () -> assertEquals(7L, respuesta.getIdCategoria())
            );
        }

        @Test
        void deberiaResponderOfertaNoEncontradaCuandoNoExiste() {
            // Arrange
            when(ofertaRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertEquals("OFERTA_NO_ENCONTRADA",
                    codigo(() -> service.actualizarOferta(usuario, 99L, actualizarRequest())));
        }

        @Test
        void deberiaRechazarConOfertaNoPropiaCuandoElServicioPerteneceAOtroProveedor() {
            // Arrange
            Proveedor otro = ServicioFixtures.proveedor(99L, ServicioFixtures.proveedorActivo());
            OfertaServicio ajena = ServicioFixtures.oferta(otro, tipo, "ACTIVO");
            when(ofertaRepository.findById(ID_SERVICIO)).thenReturn(Optional.of(ajena));

            // Act + Assert
            assertEquals("OFERTA_NO_PROPIA",
                    codigo(() -> service.actualizarOferta(usuario, ID_SERVICIO, actualizarRequest())));
            verify(ofertaRepository, never()).save(any());
        }

        @Test
        void deberiaRechazarEdicionDeUnClienteSinConsultarElRepositorio() {
            // Arrange
            Usuario cliente = ServicioFixtures.usuario(Rol.CLIENTE, EstadoUsuario.ACTIVO);

            // Act + Assert
            assertEquals("ROL_NO_AUTORIZADO",
                    codigo(() -> service.actualizarOferta(cliente, ID_SERVICIO, actualizarRequest())));
            verify(ofertaRepository, never()).findById(any());
        }

        @Test
        void deberiaRechazarEdicionConTipoDeServicioInactivo() {
            // Arrange
            when(ofertaRepository.findById(ID_SERVICIO)).thenReturn(Optional.of(ofertaPropia("ACTIVO")));
            tipo.setEstado("INACTIVO");

            // Act + Assert
            assertEquals("TIPO_SERVICIO_INACTIVO",
                    codigo(() -> service.actualizarOferta(usuario, ID_SERVICIO, actualizarRequest())));
            verify(ofertaRepository, never()).save(any());
        }

        @Test
        void deberiaRechazarEdicionConDatosInvalidos() {
            // Arrange
            when(ofertaRepository.findById(ID_SERVICIO)).thenReturn(Optional.of(ofertaPropia("ACTIVO")));
            ActualizarOfertaRequest request = actualizarRequest();
            request.setNombre(" ");

            // Act + Assert
            assertEquals("NOMBRE_REQUERIDO", codigo(() -> service.actualizarOferta(usuario, ID_SERVICIO, request)));
            verify(ofertaRepository, never()).save(any());
        }
    }

    @Nested
    class DesactivarOferta {

        @Test
        void deberiaMarcarLaOfertaPropiaComoInactivaSinEliminarla() {
            // Arrange
            OfertaServicio existente = ofertaPropia("ACTIVO");
            when(ofertaRepository.findById(ID_SERVICIO)).thenReturn(Optional.of(existente));

            // Act
            service.desactivarOferta(usuario, ID_SERVICIO);

            // Assert
            assertEquals("INACTIVO", existente.getEstado());
            verify(ofertaRepository).save(existente);
            verify(ofertaRepository, never()).delete(any(OfertaServicio.class));
            verify(ofertaRepository, never()).deleteById(any());
        }

        @Test
        void deberiaRechazarDesactivacionDeOfertaAjena() {
            // Arrange
            Proveedor otro = ServicioFixtures.proveedor(99L, ServicioFixtures.proveedorActivo());
            when(ofertaRepository.findById(ID_SERVICIO))
                    .thenReturn(Optional.of(ServicioFixtures.oferta(otro, tipo, "ACTIVO")));

            // Act + Assert
            assertEquals("OFERTA_NO_PROPIA", codigo(() -> service.desactivarOferta(usuario, ID_SERVICIO)));
            verify(ofertaRepository, never()).save(any());
        }

        @Test
        void deberiaRechazarDesactivacionDeOfertaInexistente() {
            // Arrange
            when(ofertaRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertEquals("OFERTA_NO_ENCONTRADA", codigo(() -> service.desactivarOferta(usuario, 99L)));
        }

        @Test
        void deberiaRechazarDesactivacionSinUsuario() {
            // Act + Assert
            assertEquals("USUARIO_NO_AUTENTICADO", codigo(() -> service.desactivarOferta(null, ID_SERVICIO)));
        }
    }

    @Nested
    class BuscarOfertas {

        private final Pageable pagina = PageRequest.of(0, 10);

        @Test
        void deberiaNormalizarElTextoYFiltrarPorCategoriaActiva() {
            // Arrange
            CategoriaServicio categoria = ServicioFixtures.categoria("ACTIVO");
            when(categoriaRepository.findById(ID_CATEGORIA)).thenReturn(Optional.of(categoria));
            when(ofertaRepository.buscarOfertasActivas("limpieza", ID_CATEGORIA, pagina))
                    .thenReturn(new PageImpl<>(List.of(ofertaPropia("ACTIVO")), pagina, 1));

            // Act
            BusquedaOfertasResponse respuesta = service.buscarOfertas("  LiMpieZA ", ID_CATEGORIA, pagina);

            // Assert
            assertAll(
                    () -> assertEquals(1, respuesta.getTotalElements()),
                    () -> assertEquals("Limpieza dental", respuesta.getContent().get(0).getNombre()),
                    () -> assertNull(respuesta.getMensaje())
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void deberiaBuscarSinTextoCuandoLaPalabraClaveEstaVacia(String texto) {
            // Arrange
            when(ofertaRepository.buscarOfertasActivas(eq(""), eq(null), eq(pagina)))
                    .thenReturn(new PageImpl<>(List.of(), pagina, 0));

            // Act
            BusquedaOfertasResponse respuesta = service.buscarOfertas(texto, null, pagina);

            // Assert
            assertEquals(0, respuesta.getTotalElements());
            verify(categoriaRepository, never()).findById(any());
        }

        @Test
        void deberiaRechazarCategoriaInexistente() {
            // Arrange
            when(categoriaRepository.findById(404L)).thenReturn(Optional.empty());

            // Act + Assert
            assertEquals("CATEGORIA_NO_ENCONTRADA", codigo(() -> service.buscarOfertas("x", 404L, pagina)));
            verify(ofertaRepository, never()).buscarOfertasActivas(any(), any(), any());
        }

        @Test
        void deberiaRechazarCategoriaInactiva() {
            // Arrange
            when(categoriaRepository.findById(ID_CATEGORIA))
                    .thenReturn(Optional.of(ServicioFixtures.categoria("INACTIVO")));

            // Act + Assert
            assertEquals("CATEGORIA_INACTIVA", codigo(() -> service.buscarOfertas("x", ID_CATEGORIA, pagina)));
        }

        @Test
        void deberiaConservarLaInformacionDePaginacionDelResultado() {
            // Arrange
            Pageable segunda = PageRequest.of(1, 2);
            List<OfertaServicio> contenido = List.of(ofertaPropia("ACTIVO"), ofertaPropia("ACTIVO"));
            when(ofertaRepository.buscarOfertasActivas("", null, segunda))
                    .thenReturn(new PageImpl<>(contenido, segunda, 7));

            // Act
            BusquedaOfertasResponse respuesta = service.buscarOfertas(null, null, segunda);

            // Assert
            assertAll(
                    () -> assertEquals(2, respuesta.getContent().size()),
                    () -> assertEquals(7, respuesta.getTotalElements()),
                    () -> assertEquals(4, respuesta.getTotalPages()),
                    () -> assertEquals(1, respuesta.getNumber())
            );
        }
    }

    @Nested
    class ListadosDelProveedor {

        private final Pageable pagina = PageRequest.of(0, 10);

        @Test
        void deberiaListarSoloLasOfertasActivasDelProveedor() {
            // Arrange
            when(ofertaRepository.findByProveedorIdProveedorAndEstado(ServicioFixtures.ID_PROVEEDOR, "ACTIVO", pagina))
                    .thenReturn(new PageImpl<>(List.of(ofertaPropia("ACTIVO")), pagina, 1));

            // Act
            Page<OfertaServicioResponse> resultado = service.listarMisOfertas(usuario, pagina);

            // Assert
            assertAll(
                    () -> assertEquals(1, resultado.getTotalElements()),
                    () -> assertEquals("ACTIVO", resultado.getContent().get(0).getEstado())
            );
        }

        @Test
        void deberiaListarElHistorialConOfertasActivasEInactivas() {
            // Arrange
            when(ofertaRepository.findByProveedorIdProveedor(ServicioFixtures.ID_PROVEEDOR, pagina))
                    .thenReturn(new PageImpl<>(
                            List.of(ofertaPropia("ACTIVO"), ofertaPropia("INACTIVO")), pagina, 2));

            // Act
            Page<OfertaServicioResponse> resultado = service.listarHistorialOfertas(usuario, pagina);

            // Assert
            assertAll(
                    () -> assertEquals(2, resultado.getTotalElements()),
                    () -> assertEquals("INACTIVO", resultado.getContent().get(1).getEstado())
            );
        }

        @Test
        void deberiaRechazarListadoDeUnClienteEnAmbosEndpoints() {
            // Arrange
            Usuario cliente = ServicioFixtures.usuario(Rol.CLIENTE, EstadoUsuario.ACTIVO);

            // Act + Assert
            assertAll(
                    () -> assertEquals("ROL_NO_AUTORIZADO", codigo(() -> service.listarMisOfertas(cliente, pagina))),
                    () -> assertEquals("ROL_NO_AUTORIZADO",
                            codigo(() -> service.listarHistorialOfertas(cliente, pagina)))
            );
        }
    }
}
