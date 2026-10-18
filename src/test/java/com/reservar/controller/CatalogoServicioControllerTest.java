package com.reservar.controller;

import com.reservar.dto.servicio.CategoriaCatalogoResponse;
import com.reservar.dto.servicio.TipoServicioCatalogoResponse;
import com.reservar.service.CatalogoServicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CatalogoServicioControllerTest {

    @Mock
    private CatalogoServicioService catalogoServicio;

    @InjectMocks
    private CatalogoServicioController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void deberiaResponder200ConLasCategoriasDelCatalogo() throws Exception {
        // Arrange
        when(catalogoServicio.listarCategorias())
                .thenReturn(List.of(new CategoriaCatalogoResponse(1L, "SALUD", "Servicios de salud")));

        // Act + Assert
        mockMvc.perform(get("/api/catalogo/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idCategoria").value(1))
                .andExpect(jsonPath("$[0].nombre").value("SALUD"));
    }

    @Test
    void deberiaResponder200ConLosTiposDeServicio() throws Exception {
        // Arrange
        when(catalogoServicio.listarTipos()).thenReturn(List.of(
                new TipoServicioCatalogoResponse(2L, 1L, "SALUD", "Odontologia", "Cuidado dental")));

        // Act + Assert
        mockMvc.perform(get("/api/catalogo/tipos-servicio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idTipoServicio").value(2))
                .andExpect(jsonPath("$[0].categoria").value("SALUD"));
    }

    @Test
    void deberiaResponder200ConLosTiposDeUnaCategoria() throws Exception {
        // Arrange
        when(catalogoServicio.listarTiposPorCategoria(1L)).thenReturn(List.of(
                new TipoServicioCatalogoResponse(2L, 1L, "SALUD", "Odontologia", null)));

        // Act + Assert
        mockMvc.perform(get("/api/catalogo/categorias/1/tipos-servicio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Odontologia"));
    }

    @Test
    void deberiaResponder200ConListaVaciaCuandoLaCategoriaNoTieneTipos() throws Exception {
        // Arrange
        when(catalogoServicio.listarTiposPorCategoria(99L)).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get("/api/catalogo/categorias/99/tipos-servicio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
