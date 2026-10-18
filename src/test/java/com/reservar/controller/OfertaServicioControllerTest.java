package com.reservar.controller;

import com.reservar.dto.servicio.ActualizarOfertaRequest;
import com.reservar.dto.servicio.BusquedaOfertasResponse;
import com.reservar.dto.servicio.CrearOfertaRequest;
import com.reservar.dto.servicio.OfertaServicioResponse;
import com.reservar.entity.Usuario;
import com.reservar.exception.BusinessException;
import com.reservar.exception.GlobalExceptionHandler;
import com.reservar.service.OfertaServicioService;
import com.reservar.support.ServicioFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-06, HU-07, HU-09 y HU-10 a nivel HTTP (códigos de estado, validaciones y paginación). */
@ExtendWith(MockitoExtension.class)
class OfertaServicioControllerTest {

    private static final String VALIDO = """
            {"idTipoServicio":2,"nombre":"Limpieza dental","descripcion":"Limpieza completa",
             "duracionMinutos":45,"modalidad":"PRESENCIAL","ubicacion":"Villavicencio"}
            """;

    @Mock
    private OfertaServicioService ofertaServicio;

    @InjectMocks
    private OfertaServicioController controller;

    private MockMvc mockMvc;
    private Usuario proveedor;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        proveedor = ServicioFixtures.proveedorActivo();
        autenticarComo(proveedor);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private static void autenticarComo(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private static OfertaServicioResponse respuesta(String nombre) {
        OfertaServicioResponse response = new OfertaServicioResponse();
        response.setIdServicio(10L);
        response.setNombre(nombre);
        response.setEstado("ACTIVO");
        response.setNombreProveedor("Ana Proveedora");
        response.setNombreCategoria("SALUD");
        response.setDuracionMinutos(45);
        response.setModalidad("PRESENCIAL");
        response.setUbicacion("Villavicencio");
        return response;
    }

    @Test
    void deberiaResponder201CuandoSePublicaUnaOferta() throws Exception {
        // Arrange
        when(ofertaServicio.crearOferta(eq(proveedor), any(CrearOfertaRequest.class)))
                .thenReturn(respuesta("Limpieza dental"));

        // Act + Assert
        mockMvc.perform(post("/api/ofertas").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.nombre").value("Limpieza dental"));
    }

    @Test
    void deberiaEntregarAlServicioLosDatosDeLaSolicitud() throws Exception {
        // Arrange
        when(ofertaServicio.crearOferta(any(), any())).thenReturn(respuesta("x"));

        // Act
        mockMvc.perform(post("/api/ofertas").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated());

        // Assert
        ArgumentCaptor<CrearOfertaRequest> captor = ArgumentCaptor.forClass(CrearOfertaRequest.class);
        verify(ofertaServicio).crearOferta(eq(proveedor), captor.capture());
        assertEquals(2L, captor.getValue().getIdTipoServicio());
        assertEquals(45, captor.getValue().getDuracionMinutos());
        assertEquals("Villavicencio", captor.getValue().getUbicacion());
    }

    @Test
    void deberiaResponder400IndicandoElCampoQueFallaCuandoElNombreEstaVacio() throws Exception {
        // Arrange
        String body = VALIDO.replace("Limpieza dental", "");

        // Act + Assert
        mockMvc.perform(post("/api/ofertas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.errores[0]").value("nombre: Debe ingresar nombre de la oferta"));
        verifyNoInteractions(ofertaServicio);
    }

    @Test
    void deberiaResponder400ConTodosLosCamposObligatoriosFaltantes() throws Exception {
        mockMvc.perform(post("/api/ofertas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.length()").value(5));
        verifyNoInteractions(ofertaServicio);
    }

    @Test
    void deberiaResponder403CuandoUnClienteIntentaPublicar() throws Exception {
        // Arrange
        when(ofertaServicio.crearOferta(any(), any())).thenThrow(
                new BusinessException("ROL_NO_AUTORIZADO", "Solo los proveedores pueden gestionar ofertas."));

        // Act + Assert
        mockMvc.perform(post("/api/ofertas").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ROL_NO_AUTORIZADO"));
    }

    @Test
    void deberiaResponder400CuandoLaCuentaDelProveedorNoEstaActiva() throws Exception {
        when(ofertaServicio.crearOferta(any(), any())).thenThrow(
                new BusinessException("PROVEEDOR_NO_ACTIVO", "Debes tener la cuenta activa para gestionar ofertas."));

        mockMvc.perform(post("/api/ofertas").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PROVEEDOR_NO_ACTIVO"));
    }

    @Test
    void deberiaResponder401CuandoNoHayUsuarioAutenticado() throws Exception {
        // Arrange
        SecurityContextHolder.clearContext();

        // Act + Assert
        mockMvc.perform(post("/api/ofertas").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("USUARIO_NO_AUTENTICADO"));
        verifyNoInteractions(ofertaServicio);
    }

    @Test
    void deberiaResponder401CuandoLaAutenticacionNoEstaConfirmada() throws Exception {
        // Arrange: constructor de dos argumentos => authenticated = false
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(proveedor, null));

        // Act + Assert
        mockMvc.perform(post("/api/ofertas").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deberiaResponder401CuandoElPrincipalNoEsUnUsuarioDelSistema() throws Exception {
        autenticarComo("anonymousUser");

        mockMvc.perform(put("/api/ofertas/10").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(ofertaServicio);
    }

    @Test
    void deberiaResponder200CuandoSeEditaUnaOfertaPropia() throws Exception {
        // Arrange
        when(ofertaServicio.actualizarOferta(eq(proveedor), eq(10L), any(ActualizarOfertaRequest.class)))
                .thenReturn(respuesta("Nuevo nombre"));

        // Act + Assert
        mockMvc.perform(put("/api/ofertas/10").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo nombre"));
    }

    @Test
    void deberiaResponder403CuandoSeEditaUnaOfertaAjena() throws Exception {
        when(ofertaServicio.actualizarOferta(any(), eq(10L), any())).thenThrow(
                new BusinessException("OFERTA_NO_PROPIA", "No tienes permiso para modificar esta oferta."));

        mockMvc.perform(put("/api/ofertas/10").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("OFERTA_NO_PROPIA"));
    }

    @Test
    void deberiaResponder404CuandoSeEditaUnaOfertaInexistente() throws Exception {
        when(ofertaServicio.actualizarOferta(any(), eq(999L), any())).thenThrow(
                new BusinessException("OFERTA_NO_ENCONTRADA", "La oferta indicada no existe."));

        mockMvc.perform(put("/api/ofertas/999").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    void deberiaResponder400CuandoLaEdicionTieneDuracionNoPositiva() throws Exception {
        String body = VALIDO.replace("\"duracionMinutos\":45", "\"duracionMinutos\":0");

        mockMvc.perform(put("/api/ofertas/10").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]")
                        .value("duracionMinutos: La duración de la oferta debe ser mayor que cero"));
    }

    @Test
    void deberiaResponder204CuandoSeDesactivaUnaOfertaPropia() throws Exception {
        mockMvc.perform(patch("/api/ofertas/10/desactivar"))
                .andExpect(status().isNoContent());

        verify(ofertaServicio).desactivarOferta(proveedor, 10L);
    }

    @Test
    void deberiaResponder403CuandoSeDesactivaUnaOfertaAjena() throws Exception {
        doThrow(new BusinessException("OFERTA_NO_PROPIA", "No tienes permiso para modificar esta oferta."))
                .when(ofertaServicio).desactivarOferta(any(), eq(10L));

        mockMvc.perform(patch("/api/ofertas/10/desactivar"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deberiaResponder401CuandoSeDesactivaSinSesion() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(patch("/api/ofertas/10/desactivar"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(ofertaServicio);
    }

    @Test
    void deberiaResponder200ConElDetalleDeUnaOfertaActiva() throws Exception {
        // Arrange
        when(ofertaServicio.obtenerDetalle(10L)).thenReturn(respuesta("Limpieza dental"));

        // Act + Assert (no requiere sesión)
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/ofertas/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreProveedor").value("Ana Proveedora"))
                .andExpect(jsonPath("$.nombreCategoria").value("SALUD"))
                .andExpect(jsonPath("$.duracionMinutos").value(45))
                .andExpect(jsonPath("$.ubicacion").value("Villavicencio"));
    }

    @Test
    void deberiaResponder404ConMensajeClaroCuandoElServicioNoEstaDisponible() throws Exception {
        when(ofertaServicio.obtenerDetalle(5L)).thenThrow(new BusinessException(
                "OFERTA_NO_DISPONIBLE", "Servicio no disponible. La oferta no existe o no está activa."));

        mockMvc.perform(get("/api/ofertas/5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("OFERTA_NO_DISPONIBLE"))
                .andExpect(jsonPath("$.mensaje").value("Servicio no disponible. La oferta no existe o no está activa."));
    }

    @Test
    void deberiaBuscarConPaginacionPorDefectoDeDiezOrdenadaPorFechaDeCreacion() throws Exception {
        // Arrange
        when(ofertaServicio.buscarOfertas(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new BusquedaOfertasResponse(new PageImpl<>(List.of(respuesta("A")), PageRequest.of(0, 10), 1), null));

        // Act
        mockMvc.perform(get("/api/ofertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("A"))
                .andExpect(jsonPath("$.mensaje").doesNotExist());

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(ofertaServicio).buscarOfertas(isNull(), isNull(), captor.capture());
        assertEquals(10, captor.getValue().getPageSize());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("fechaCreacion").getDirection());
    }

    @Test
    void deberiaPasarTextoCategoriaYPaginaSolicitados() throws Exception {
        // Arrange
        when(ofertaServicio.buscarOfertas(eq("dental"), eq(1L), any(Pageable.class)))
                .thenReturn(new BusquedaOfertasResponse(new PageImpl<>(List.of(), PageRequest.of(2, 5), 0), "Sin resultados"));

        // Act + Assert: lista vacía con mensaje, no un error (HU-09 escenario 2)
        mockMvc.perform(get("/api/ofertas?texto=dental&idCategoria=1&page=2&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.mensaje").value("Sin resultados"));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(ofertaServicio).buscarOfertas(eq("dental"), eq(1L), captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
    }

    @Test
    void deberiaResponder400CuandoLaCategoriaDelFiltroNoExiste() throws Exception {
        when(ofertaServicio.buscarOfertas(any(), eq(404L), any())).thenThrow(
                new BusinessException("CATEGORIA_NO_ENCONTRADA", "La categoría indicada no existe."));

        mockMvc.perform(get("/api/ofertas?idCategoria=404"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CATEGORIA_NO_ENCONTRADA"));
    }

    @Test
    void deberiaListarLasOfertasActivasDelProveedor() throws Exception {
        // Arrange
        when(ofertaServicio.listarMisOfertas(eq(proveedor), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(respuesta("Mía")), PageRequest.of(0, 10), 1));

        // Act + Assert
        mockMvc.perform(get("/api/ofertas/mis-ofertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Mía"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void deberiaListarElHistorialDeOfertasDelProveedor() throws Exception {
        // Arrange
        when(ofertaServicio.listarHistorialOfertas(eq(proveedor), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(respuesta("Antigua")), PageRequest.of(0, 10), 1));

        // Act + Assert
        mockMvc.perform(get("/api/ofertas/mis-ofertas/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Antigua"));
    }

    @Test
    void deberiaResponder401AlListarOfertasPropiasSinSesion() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/ofertas/mis-ofertas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/ofertas/mis-ofertas/historial")).andExpect(status().isUnauthorized());
        verifyNoInteractions(ofertaServicio);
    }
}
