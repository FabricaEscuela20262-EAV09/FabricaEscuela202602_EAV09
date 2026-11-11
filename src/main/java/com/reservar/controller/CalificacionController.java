package com.reservar.controller;

import com.reservar.dto.reserva.CalificacionRequest;
import com.reservar.dto.reserva.CalificacionResponse;
import com.reservar.entity.Usuario;
import com.reservar.exception.BusinessException;
import com.reservar.service.CalificacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservas/{idReserva}/calificacion")
public class CalificacionController {
    private final CalificacionService service;

    public CalificacionController(CalificacionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CalificacionResponse> crear(@PathVariable Long idReserva,
            @Valid @RequestBody CalificacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(usuario(), idReserva, request));
    }

    @PutMapping
    public ResponseEntity<CalificacionResponse> editar(@PathVariable Long idReserva,
            @Valid @RequestBody CalificacionRequest request) {
        return ResponseEntity.ok(service.editar(usuario(), idReserva, request));
    }

    private Usuario usuario() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Usuario usuario)) {
            throw new BusinessException("USUARIO_NO_AUTENTICADO", "Debes iniciar sesión para realizar esta operación.");
        }
        return usuario;
    }
}
