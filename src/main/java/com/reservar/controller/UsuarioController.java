package com.reservar.controller;

import com.reservar.dto.usuario.ActualizarPerfilRequest;
import com.reservar.dto.usuario.PerfilResponse;
import com.reservar.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuario")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/perfil")
    public ResponseEntity<PerfilResponse> obtenerPerfil() {

        PerfilResponse response = usuarioService.obtenerPerfil();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/perfil")
    public ResponseEntity<String> actualizarPerfil(
            @Valid @RequestBody ActualizarPerfilRequest request) {

        usuarioService.actualizarPerfil(request);

        return ResponseEntity.ok(
                "Perfil actualizado correctamente, si solicitó cambio de correo recuerdo confirmarlo"
        );
    }

    @GetMapping("/correo/confirmar/{token}")
    public ResponseEntity<String> confirmarCambioCorreo(
            @PathVariable String token) {

        usuarioService.confirmarCambioCorreo(token);

        return ResponseEntity.ok(
                "Correo actualizado correctamente"
        );
    }
}