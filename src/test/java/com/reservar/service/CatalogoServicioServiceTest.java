package com.reservar.service;

import com.reservar.dto.servicio.CategoriaCatalogoResponse;
import com.reservar.dto.servicio.TipoServicioCatalogoResponse;
import com.reservar.entity.CategoriaServicio;
import com.reservar.entity.TipoServicio;
import com.reservar.repository.CategoriaServicioRepository;
import com.reservar.repository.TipoServicioRepository;
import com.reservar.support.ServicioFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/** Lista predefinida (catálogo) que usa HU-06 para validar la categoría y el tipo de servicio. */
@ExtendWith(MockitoExtension.class)
class CatalogoServicioServiceTest {

    @Mock
    private CategoriaServicioRepository categoriaRepository;
    @Mock
    private TipoServicioRepository tipoRepository;

    @InjectMocks
    private CatalogoServicioService service;

    @Test
    void deberiaListarSoloLasCategoriasActivas() {
        // Arrange
        CategoriaServicio categoria = ServicioFixtures.categoria("ACTIVO");
        when(categoriaRepository.findByEstado("ACTIVO")).thenReturn(List.of(categoria));

        // Act
        List<CategoriaCatalogoResponse> respuesta = service.listarCategorias();

        // Assert
        assertAll(
                () -> assertEquals(1, respuesta.size()),
                () -> assertEquals(ServicioFixtures.ID_CATEGORIA, respuesta.get(0).idCategoria()),
                () -> assertEquals("SALUD", respuesta.get(0).nombre()),
                () -> assertEquals("Servicios de salud", respuesta.get(0).descripcion())
        );
    }

    @Test
    void deberiaRetornarListaVaciaCuandoNoHayCategoriasActivas() {
        when(categoriaRepository.findByEstado("ACTIVO")).thenReturn(List.of());

        assertTrue(service.listarCategorias().isEmpty());
    }

    @Test
    void deberiaListarLosTiposDeServicioActivosConSuCategoria() {
        // Arrange
        TipoServicio tipo = ServicioFixtures.tipoActivo();
        when(tipoRepository.findByEstado("ACTIVO")).thenReturn(List.of(tipo));

        // Act
        List<TipoServicioCatalogoResponse> respuesta = service.listarTipos();

        // Assert
        TipoServicioCatalogoResponse primero = respuesta.get(0);
        assertAll(
                () -> assertEquals(1, respuesta.size()),
                () -> assertEquals(ServicioFixtures.ID_TIPO, primero.idTipoServicio()),
                () -> assertEquals(ServicioFixtures.ID_CATEGORIA, primero.idCategoria()),
                () -> assertEquals("SALUD", primero.categoria()),
                () -> assertEquals("Odontologia", primero.nombre()),
                () -> assertEquals("Cuidado dental", primero.descripcion())
        );
    }

    @Test
    void deberiaListarLosTiposActivosDeUnaCategoria() {
        // Arrange
        when(tipoRepository.findByCategoriaIdCategoriaAndEstado(ServicioFixtures.ID_CATEGORIA, "ACTIVO"))
                .thenReturn(List.of(ServicioFixtures.tipoActivo()));

        // Act
        List<TipoServicioCatalogoResponse> respuesta = service.listarTiposPorCategoria(ServicioFixtures.ID_CATEGORIA);

        // Assert
        assertEquals("Odontologia", respuesta.get(0).nombre());
    }

    @Test
    void deberiaRetornarListaVaciaCuandoLaCategoriaNoTieneTipos() {
        when(tipoRepository.findByCategoriaIdCategoriaAndEstado(99L, "ACTIVO")).thenReturn(List.of());

        assertTrue(service.listarTiposPorCategoria(99L).isEmpty());
    }
}
