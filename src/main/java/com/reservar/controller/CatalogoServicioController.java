package com.reservar.controller;

import com.reservar.dto.servicio.CategoriaCatalogoResponse;
import com.reservar.dto.servicio.TipoServicioCatalogoResponse;
import com.reservar.service.CatalogoServicioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/catalogo")
public class CatalogoServicioController {

    private final CatalogoServicioService catalogoServicio;

    public CatalogoServicioController(CatalogoServicioService catalogoServicio) {
        this.catalogoServicio = catalogoServicio;
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaCatalogoResponse>>
    listarCategorias() {
        return ResponseEntity.ok(
                catalogoServicio.listarCategorias());
    }

    @GetMapping("/tipos-servicio")
    public ResponseEntity<List<TipoServicioCatalogoResponse>>
    listarTipos() {
        return ResponseEntity.ok(
                catalogoServicio.listarTipos());
    }

    @GetMapping("/categorias/{idCategoria}/tipos-servicio")
    public ResponseEntity<List<TipoServicioCatalogoResponse>>
    listarTiposPorCategoria(
            @PathVariable Long idCategoria) {
        return ResponseEntity.ok(
                catalogoServicio.listarTiposPorCategoria(idCategoria));
    }
}