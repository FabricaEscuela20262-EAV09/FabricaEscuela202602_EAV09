package com.reservar.controller;

import com.reservar.dto.auth.ConfirmarRecuperacionRequest;
import com.reservar.dto.auth.LoginRequest;
import com.reservar.dto.auth.LoginResponse;
import com.reservar.dto.auth.RegistroClienteRequest;
import com.reservar.dto.auth.RegistroProveedorRequest;
import com.reservar.dto.auth.RegistroResponse;
import com.reservar.dto.auth.SolicitudRecuperacionRequest;
import com.reservar.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // REGISTRO CLIENTE
    @PostMapping("/registro/cliente")
    public ResponseEntity<RegistroResponse> registrarCliente(
            @Valid @RequestBody RegistroClienteRequest request) {

        RegistroResponse response =
                authService.registrarCliente(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // REGISTRO PROVEEDOR
    @PostMapping("/registro/proveedor")
    public ResponseEntity<RegistroResponse> registrarProveedor(
            @Valid @RequestBody RegistroProveedorRequest request) {

        RegistroResponse response =
                authService.registrarProveedor(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // VERIFICACIÓN DE CORREO
    @GetMapping("/verificar/{token}")
    public ResponseEntity<String> verificarCorreo(
            @PathVariable String token) {

        authService.verificarCorreo(token);

        return ResponseEntity.ok(
                "Correo verificado correctamente. Ya puedes iniciar sesión."
        );
    }

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipOrigen = extraerIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginResponse response =
                authService.login(request, ipOrigen, userAgent);

        return ResponseEntity.ok(response);
    }

    // SOLICITAR RECUPERACIÓN DE CONTRASEÑA
    @PostMapping("/recuperacion/solicitar")
    public ResponseEntity<String> solicitarRecuperacion(
            @Valid @RequestBody SolicitudRecuperacionRequest request) {

        authService.solicitarRecuperacion(request.getCorreo());

        return ResponseEntity.ok(
                "Si el correo está registrado, se ha generado un enlace de recuperación."
        );
    }

    // CONFIRMAR RECUPERACIÓN DE CONTRASEÑA
    @PostMapping("/recuperacion/confirmar")
    public ResponseEntity<String> confirmarRecuperacion(
            @Valid @RequestBody ConfirmarRecuperacionRequest request) {

        authService.confirmarRecuperacion(
                request.getToken(),
                request.getNuevaContrasena()
        );

        return ResponseEntity.ok(
                "Contraseña actualizada correctamente. Las sesiones anteriores han sido invalidadas."
        );
    }

    // EXTRAER IP REAL
    private String extraerIp(HttpServletRequest request) {

        String header = request.getHeader("X-Forwarded-For");

        if (header != null && !header.isBlank()) {
            // Puede venir una lista separada por comas;
            // la primera corresponde a la IP original del cliente.
            return header.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}