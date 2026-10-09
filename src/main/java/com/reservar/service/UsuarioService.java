package com.reservar.service;

import com.reservar.dto.usuario.PerfilResponse;
import com.reservar.dto.usuario.ActualizarPerfilRequest;
import com.reservar.entity.Proveedor;
import com.reservar.entity.Token;
import com.reservar.entity.Usuario;
import com.reservar.entity.enums.Rol;
import com.reservar.entity.enums.TipoToken;
import com.reservar.exception.BusinessException;
import com.reservar.repository.ProveedorRepository;
import com.reservar.repository.TokenRepository;
import com.reservar.repository.UsuarioRepository;
import com.reservar.security.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ProveedorRepository proveedorRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${app.backend.url}")
    private String backendUrl;

    private final SecureRandom secureRandom = new SecureRandom();

    // revisión de datos de perfil antes de actualización: funcionalidad agregada.
    @Transactional(readOnly = true)
    public PerfilResponse obtenerPerfil() {

        Usuario usuario = obtenerUsuarioAutenticado();

        String tipoServicio = null;
        String descripcion = null;

        if (usuario.getRol() == Rol.PROVEEDOR) {

            Proveedor proveedor = proveedorRepository
                    .findByUsuarioIdUsuario(usuario.getIdUsuario())
                    .orElse(null);

            if (proveedor != null) {
                descripcion = proveedor.getDescripcion();
            }
        }

        return new PerfilResponse(
                usuario.getNombreCompleto(),
                usuario.getCorreo(),
                usuario.getCorreoPendiente(),
                usuario.getTelefono(),
                usuario.getRol().name(),
                tipoServicio,
                descripcion
        );
    }

    @Transactional
    public void actualizarPerfil(ActualizarPerfilRequest request) {

        Usuario usuario = obtenerUsuarioAutenticado();

        // Nombre y teléfono
        usuario.setNombreCompleto(request.getNombreCompleto().trim());

        if (request.getTelefono() == null || request.getTelefono().isBlank()) {
            usuario.setTelefono(null);
        } else {
            usuario.setTelefono(request.getTelefono().trim());
        }

        // Cambio de correo
        String nuevoCorreo = request.getCorreo().trim();

        if (!nuevoCorreo.equalsIgnoreCase(usuario.getCorreo())) {

            boolean correoEnUso = usuarioRepository.existsByCorreo(nuevoCorreo);

            if (correoEnUso) {
                throw new BusinessException(
                        "CORREO_EN_USO",
                        "Este correo ya está en uso"
                );
            }

            usuario.setCorreoPendiente(nuevoCorreo);

            crearTokenActualizacion(usuario);
        }

        // Datos adicionales del proveedor
        if (usuario.getRol().name().equals("PROVEEDOR")) {

            Proveedor proveedor = proveedorRepository
                    .findByUsuarioIdUsuario(usuario.getIdUsuario())
                    .orElseThrow(() -> new BusinessException(
                            "PROVEEDOR_NO_ENCONTRADO",
                            "No se encontraron los datos del proveedor"
                    ));

            if (request.getDescripcion() != null) {
                proveedor.setDescripcion(request.getDescripcion().trim());
            }

            proveedorRepository.save(proveedor);
        }

        usuarioRepository.save(usuario);
    }

    @Transactional
    public void confirmarCambioCorreo(String tokenPlano) {

        if (tokenPlano == null || tokenPlano.isBlank()) {
            throw new BusinessException(
                    "TOKEN_INVALIDO",
                    "El token de actualización es obligatorio"
            );
        }

        String hashToken = TokenHasher.hash(tokenPlano);

        Token token = tokenRepository
                .findByHashTokenAndTipoAndFechaUsoIsNull(
                        hashToken,
                        TipoToken.ACTUALIZACION
                )
                .orElseThrow(() -> new BusinessException(
                        "TOKEN_INVALIDO",
                        "El token de actualización no es válido"
                ));

        if (token.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new BusinessException(
                    "TOKEN_EXPIRADO",
                    "El token de actualización ha expirado"
            );
        }

        Usuario usuario = token.getUsuario();

        if (usuario.getCorreoPendiente() == null
                || usuario.getCorreoPendiente().isBlank()) {
            throw new BusinessException(
                    "CAMBIO_CORREO_NO_PENDIENTE",
                    "No existe un cambio de correo pendiente"
            );
        }

        usuario.setCorreo(usuario.getCorreoPendiente());
        usuario.setCorreoPendiente(null);

        token.setFechaUso(LocalDateTime.now());

        usuarioRepository.save(usuario);
        tokenRepository.save(token);
    }

    private Usuario obtenerUsuarioAutenticado() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof Usuario usuario)) {

            throw new BusinessException(
                    "USUARIO_NO_AUTENTICADO",
                    "El usuario no está autenticado"
            );
        }

        return usuario;
    }

    private void crearTokenActualizacion(Usuario usuario) {

        // Eliminar tokens de actualización anteriores no utilizados
        tokenRepository.deleteByUsuarioIdUsuarioAndTipo(
                usuario.getIdUsuario(),
                TipoToken.ACTUALIZACION
        );

        String tokenPlano = generarToken();

        Token token = new Token();
        token.setUsuario(usuario);
        token.setHashToken(TokenHasher.hash(tokenPlano));
        token.setTipo(TipoToken.ACTUALIZACION);
        token.setFechaExpiracion(LocalDateTime.now().plusHours(1));

        tokenRepository.save(token);

        /*
         * El enlace apunta directo al backend (GET /api/usuario/correo/confirmar/{token}),
         * ya que no hay frontend en este sprint: al hacer clic, la confirmación se completa sin pasos adicionales.
         */
        String enlace = backendUrl + "/api/usuario/correo/confirmar/" + tokenPlano;

        emailService.enviarCorreoActualizacion(
                usuario.getCorreoPendiente(),
                enlace,
                token.getFechaExpiracion()
        );
    }

    private String generarToken() {

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        return HexFormat.of().formatHex(bytes);
    }
}