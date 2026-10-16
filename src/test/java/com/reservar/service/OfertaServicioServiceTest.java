package com.reservar.service;

import com.reservar.dto.servicio.BusquedaOfertasResponse;
import com.reservar.dto.servicio.OfertaServicioResponse;
import com.reservar.entity.CategoriaServicio;
import com.reservar.entity.OfertaServicio;
import com.reservar.entity.Proveedor;
import com.reservar.entity.TipoServicio;
import com.reservar.entity.Usuario;
import com.reservar.exception.BusinessException;
import com.reservar.repository.BloqueoDisponibilidadRepository;
import com.reservar.repository.CategoriaServicioRepository;
import com.reservar.repository.DisponibilidadServicioRepository;
import com.reservar.repository.OfertaServicioRepository;
import com.reservar.repository.ProveedorRepository;
import com.reservar.repository.TipoServicioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfertaServicioServiceTest {

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

    private OfertaServicio crearOferta(String estadoTipo, String estadoCategoria) {
        CategoriaServicio categoria = new CategoriaServicio();
        categoria.setIdCategoria(1L);
        categoria.setNombre("SALUD");
        categoria.setEstado(estadoCategoria);

        TipoServicio tipo = new TipoServicio();
        tipo.setIdTipoServicio(2L);
        tipo.setNombre("Odontologia");
        tipo.setEstado(estadoTipo);
        tipo.setCategoria(categoria);

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto("Ana Proveedora");

        Proveedor proveedor = new Proveedor();
        proveedor.setIdProveedor(3L);
        proveedor.setUsuario(usuario);

        OfertaServicio oferta = new OfertaServicio();
        oferta.setIdServicio(10L);
        oferta.setProveedor(proveedor);
        oferta.setTipoServicio(tipo);
        oferta.setNombre("Limpieza dental");
        oferta.setDescripcion("Limpieza completa");
        oferta.setDuracionMinutos(45);
        oferta.setModalidad("PRESENCIAL");
        oferta.setUbicacion("Villavicencio");
        oferta.setEstado("ACTIVO");
        return oferta;
    }

    @Test
    void deberiaRetornarDetalleDeOfertaActivaConDatosDelProveedor() {
        // Arrange
        when(ofertaRepository.findByIdServicioAndEstado(10L, "ACTIVO"))
                .thenReturn(Optional.of(crearOferta("ACTIVO", "ACTIVO")));
        when(disponibilidadRepository.findByServicioIdServicio(10L)).thenReturn(List.of());
        when(bloqueoRepository.findByServicioIdServicioAndFechaBetween(eq(10L), any(), any()))
                .thenReturn(List.of());

        // Act
        OfertaServicioResponse respuesta = service.obtenerDetalle(10L);

        // Assert
        assertAll(
                () -> assertEquals("Limpieza dental", respuesta.getNombre()),
                () -> assertEquals("Ana Proveedora", respuesta.getNombreProveedor()),
                () -> assertEquals("SALUD", respuesta.getNombreCategoria()),
                () -> assertEquals(45, respuesta.getDuracionMinutos()),
                () -> assertEquals("Villavicencio", respuesta.getUbicacion())
        );
    }

    @Test
    void deberiaLanzarOfertaNoDisponibleCuandoLaOfertaNoExisteOEstaInactiva() {
        // Arrange
        when(ofertaRepository.findByIdServicioAndEstado(99L, "ACTIVO")).thenReturn(Optional.empty());

        // Act
        BusinessException ex = assertThrows(BusinessException.class, () -> service.obtenerDetalle(99L));

        // Assert
        assertAll(
                () -> assertEquals("OFERTA_NO_DISPONIBLE", ex.getCodigo()),
                () -> assertTrue(ex.getMessage().startsWith("Servicio no disponible"))
        );
    }

    @Test
    void deberiaLanzarOfertaNoDisponibleCuandoElCatalogoDeLaOfertaEstaInactivo() {
        // Arrange
        when(ofertaRepository.findByIdServicioAndEstado(10L, "ACTIVO"))
                .thenReturn(Optional.of(crearOferta("ACTIVO", "INACTIVO")));

        // Act
        BusinessException ex = assertThrows(BusinessException.class, () -> service.obtenerDetalle(10L));

        // Assert
        assertEquals("OFERTA_NO_DISPONIBLE", ex.getCodigo());
    }

    @Test
    void deberiaRetornarListaVaciaConMensajeCuandoLaBusquedaNoTieneResultados() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(ofertaRepository.buscarOfertasActivas(eq("zzzz"), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // Act
        BusquedaOfertasResponse respuesta = service.buscarOfertas("zzzz", null, pageable);

        // Assert
        assertAll(
                () -> assertTrue(respuesta.getContent().isEmpty()),
                () -> assertEquals(0, respuesta.getTotalElements()),
                () -> assertEquals("No se encontraron servicios que coincidan con la búsqueda.", respuesta.getMensaje())
        );
    }

    @Test
    void deberiaRetornarResultadosSinMensajeCuandoLaBusquedaEncuentraOfertas() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        OfertaServicio oferta = crearOferta("ACTIVO", "ACTIVO");
        when(ofertaRepository.buscarOfertasActivas(eq("limpieza"), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(oferta), pageable, 1));

        // Act
        BusquedaOfertasResponse respuesta = service.buscarOfertas("Limpieza", null, pageable);

        // Assert
        assertAll(
                () -> assertEquals(1, respuesta.getContent().size()),
                () -> assertEquals(1, respuesta.getTotalElements()),
                () -> assertNull(respuesta.getMensaje())
        );
    }

    @Test
    void deberiaRetornarPaginaVaciaSinMensajeCuandoLaPaginaSolicitadaExcedeLosResultados() {
        // Arrange
        Pageable pageable = PageRequest.of(5, 10);
        when(ofertaRepository.buscarOfertasActivas(eq(""), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 3));

        // Act
        BusquedaOfertasResponse respuesta = service.buscarOfertas(null, null, pageable);

        // Assert
        assertAll(
                () -> assertTrue(respuesta.getContent().isEmpty()),
                () -> assertEquals(3, respuesta.getTotalElements()),
                () -> assertNull(respuesta.getMensaje())
        );
    }
}
