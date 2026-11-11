package com.reservar.controller;

import com.reservar.dto.reserva.MotivoReservaRequest;
import com.reservar.dto.reserva.ReservaResponse;
import com.reservar.dto.reserva.HistorialReservaResponse;
import com.reservar.entity.enums.EstadoReserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import java.util.List;
import com.reservar.entity.Usuario;
import com.reservar.exception.BusinessException;
import com.reservar.service.GestionReservaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservas")
public class GestionReservaController {
    private final GestionReservaService service;

    public GestionReservaController(GestionReservaService service) {
        this.service = service;
    }

    @PatchMapping("/{idReserva}/confirmar")
    public ResponseEntity<ReservaResponse> confirmar(@PathVariable Long idReserva) {
        return ResponseEntity.ok(service.confirmar(obtenerUsuarioAutenticado(), idReserva));
    }

    @PatchMapping("/{idReserva}/rechazar")
    public ResponseEntity<ReservaResponse> rechazar(@PathVariable Long idReserva,
            @Valid @RequestBody(required = false) MotivoReservaRequest request) {
        return ResponseEntity.ok(service.rechazar(obtenerUsuarioAutenticado(), idReserva,
                request == null ? null : request.getMotivo()));
    }

    @PatchMapping("/{idReserva}/cancelar")
    public ResponseEntity<ReservaResponse> cancelar(@PathVariable Long idReserva,
            @Valid @RequestBody(required = false) MotivoReservaRequest request) {
        return ResponseEntity.ok(service.cancelar(obtenerUsuarioAutenticado(), idReserva,
                request == null ? null : request.getMotivo()));
    }

    @GetMapping
    public ResponseEntity<Page<ReservaResponse>> historial(@RequestParam(required = false) EstadoReserva estado,
            @PageableDefault(size = 10, sort = "fechaInicio", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.historial(obtenerUsuarioAutenticado(), estado, pageable));
    }

    @GetMapping("/{idReserva}")
    public ResponseEntity<ReservaResponse> detalle(@PathVariable Long idReserva) {
        return ResponseEntity.ok(service.detalle(obtenerUsuarioAutenticado(), idReserva));
    }

    @GetMapping("/{idReserva}/historial")
    public ResponseEntity<List<HistorialReservaResponse>> auditoria(@PathVariable Long idReserva) {
        return ResponseEntity.ok(service.auditoria(obtenerUsuarioAutenticado(), idReserva));
    }

    private Usuario obtenerUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Usuario usuario)) {
            throw new BusinessException("USUARIO_NO_AUTENTICADO", "Debes iniciar sesión para realizar esta operación.");
        }
        return usuario;
    }
}
