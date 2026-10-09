package com.reservar.controller;

import com.reservar.dto.servicio.ActualizarOfertaRequest;
import com.reservar.dto.servicio.CrearOfertaRequest;
import com.reservar.dto.servicio.OfertaServicioResponse;
import com.reservar.entity.Usuario;
import com.reservar.exception.BusinessException;
import com.reservar.service.OfertaServicioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/ofertas")
public class OfertaServicioController {
    private final OfertaServicioService ofertaServicio;

    public OfertaServicioController(OfertaServicioService ofertaServicio) {
        this.ofertaServicio = ofertaServicio;
    }

    @PostMapping
    public ResponseEntity<OfertaServicioResponse> crearOferta(
            @Valid @RequestBody CrearOfertaRequest request) {
        Usuario usuario = obtenerUsuarioAutenticado();
        OfertaServicioResponse response =
                ofertaServicio.crearOferta(usuario, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OfertaServicioResponse> actualizarOferta(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarOfertaRequest request) {
        Usuario usuario = obtenerUsuarioAutenticado();
        OfertaServicioResponse response =
                ofertaServicio.actualizarOferta(usuario, id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivarOferta(
            @PathVariable Long id) {
        Usuario usuario = obtenerUsuarioAutenticado();
        ofertaServicio.desactivarOferta(usuario, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfertaServicioResponse> obtenerDetalle(
            @PathVariable Long id) {
        return ResponseEntity.ok(ofertaServicio.obtenerDetalle(id));
    }

    @GetMapping
    public ResponseEntity<Page<OfertaServicioResponse>> buscarOfertas(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Long idCategoria,
            @PageableDefault(
                    size = 10,
                    sort = "fechaCreacion",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {
        System.out.println(">>> ENTRO A buscarOfertas()");
        return ResponseEntity.ok(
                ofertaServicio.buscarOfertas(texto, idCategoria, pageable)
        );
    }

    @GetMapping("/mis-ofertas")
    public ResponseEntity<Page<OfertaServicioResponse>> listarMisOfertas(
            @PageableDefault(
                    size = 10,
                    sort = "fechaCreacion",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {
        Usuario usuario = obtenerUsuarioAutenticado();
        return ResponseEntity.ok(
                ofertaServicio.listarMisOfertas(usuario, pageable)
        );
    }

    private Usuario obtenerUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Usuario usuario)) {
            throw new BusinessException(
                    "USUARIO_NO_AUTENTICADO",
                    "Debes iniciar sesión para realizar esta operación."
            );
        }
        return usuario;
    }
}

