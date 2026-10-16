package com.reservar.controller;

import com.reservar.dto.servicio.BloqueoDisponibilidadRequest;
import com.reservar.dto.servicio.DisponibilidadRequest;
import com.reservar.dto.servicio.DisponibilidadResponse;
import com.reservar.entity.Usuario;
import com.reservar.exception.BusinessException;
import com.reservar.service.DisponibilidadServicioService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ofertas/{idServicio}")
public class DisponibilidadServicioController {
    private final DisponibilidadServicioService disponibilidadServicio;
    public DisponibilidadServicioController(
            DisponibilidadServicioService disponibilidadServicio) {
        this.disponibilidadServicio = disponibilidadServicio;
    }

    @PostMapping("/disponibilidad")
    public ResponseEntity<DisponibilidadResponse> agregarDisponibilidad(
            @PathVariable Long idServicio,
            @Valid @RequestBody DisponibilidadRequest request) {

        Usuario usuario = obtenerUsuarioAutenticado();
        DisponibilidadResponse response =
                disponibilidadServicio.agregarDisponibilidad(
                        usuario, idServicio, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<List<DisponibilidadResponse>> listarDisponibilidad(
            @PathVariable Long idServicio) {
        return ResponseEntity.ok(
                disponibilidadServicio.listarDisponibilidad(idServicio));
    }

    @DeleteMapping("/disponibilidad/{idDisponibilidad}")
    public ResponseEntity<Void> eliminarDisponibilidad(
            @PathVariable Long idServicio,
            @PathVariable Long idDisponibilidad) {

        Usuario usuario = obtenerUsuarioAutenticado();
        disponibilidadServicio.eliminarDisponibilidad(
                usuario, idServicio, idDisponibilidad);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bloqueos")
    public ResponseEntity<DisponibilidadResponse> crearBloqueo(
            @PathVariable Long idServicio,
            @Valid @RequestBody BloqueoDisponibilidadRequest request) {

        Usuario usuario = obtenerUsuarioAutenticado();
        DisponibilidadResponse response =
                disponibilidadServicio.crearBloqueo(
                        usuario, idServicio, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/bloqueos")
    public ResponseEntity<List<DisponibilidadResponse>> listarBloqueos(
            @PathVariable Long idServicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin) {

        return ResponseEntity.ok(
                disponibilidadServicio.listarBloqueos(
                        idServicio, fechaInicio, fechaFin));
    }

    @DeleteMapping("/bloqueos/{idBloqueo}")
    public ResponseEntity<Void> eliminarBloqueo(
            @PathVariable Long idServicio,
            @PathVariable Long idBloqueo) {

        Usuario usuario = obtenerUsuarioAutenticado();
        disponibilidadServicio.eliminarBloqueo(
                usuario, idServicio, idBloqueo);
        return ResponseEntity.noContent().build();
    }

    private Usuario obtenerUsuarioAutenticado() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
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

