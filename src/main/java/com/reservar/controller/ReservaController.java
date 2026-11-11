package com.reservar.controller;

import com.reservar.dto.reserva.CrearReservaRequest;
import com.reservar.dto.reserva.ReservaResponse;
import com.reservar.entity.Usuario;
import com.reservar.exception.BusinessException;
import com.reservar.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> crearReserva(
            @Valid @RequestBody CrearReservaRequest request) {

        Usuario usuario = obtenerUsuarioAutenticado();

        ReservaResponse response =
                reservaService.crearReserva(usuario, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
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

