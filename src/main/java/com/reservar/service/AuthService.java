package com.reservar.service;

import com.reservar.dto.auth.LoginRequest;
import com.reservar.dto.auth.LoginResponse;
import com.reservar.dto.auth.RegistroClienteRequest;
import com.reservar.dto.auth.RegistroProveedorRequest;
import com.reservar.dto.auth.RegistroResponse;
import com.reservar.entity.CodigoProveedor;
import com.reservar.entity.IntentoLogueo;
import com.reservar.entity.Proveedor;
import com.reservar.entity.Sesion;
import com.reservar.entity.Token;
import com.reservar.entity.Usuario;
import com.reservar.entity.enums.EstadoCodigoProveedor;
import com.reservar.entity.enums.EstadoUsuario;
import com.reservar.entity.enums.Rol;
import com.reservar.entity.enums.TipoToken;
import com.reservar.exception.BusinessException;
import com.reservar.repository.CodigoProveedorRepository;
import com.reservar.repository.IntentoLogueoRepository;
import com.reservar.repository.ProveedorRepository;
import com.reservar.repository.SesionRepository;
import com.reservar.repository.TokenRepository;
import com.reservar.repository.UsuarioRepository;
import com.reservar.validation.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final CodigoProveedorRepository codigoProveedorRepository;
    private final ProveedorRepository proveedorRepository;
    private final IntentoLogueoRepository intentoLogueoRepository;
    private final TokenRepository tokenRepository;
    private final SesionRepository sesionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.backend.url}")
    private String backendUrl;

    private final SecureRandom secureRandom = new SecureRandom();


    // REGISTRO CLIENTE
    @Transactional
    public RegistroResponse registrarCliente(RegistroClienteRequest request) {

        validarContrasena(request.getContrasena());

        String correo = request.getCorreo().trim();

        if (usuarioRepository.existsByCorreo(correo)) {
            throw new BusinessException(
                    "CORREO_EXISTENTE",
                    "Este correo ya está registrado"
            );
        }

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(request.getNombreCompleto().trim());
        usuario.setCorreo(correo);
        usuario.setHashContrasena(passwordEncoder.encode(request.getContrasena()));
        usuario.setRol(Rol.CLIENTE);
        usuario.setEstado(EstadoUsuario.PENDIENTE);
        usuario.setIntentosFallidos(0);

        LocalDateTime ahora = LocalDateTime.now();
        usuario.setFechaCreacion(ahora);
        usuario.setFechaActualizacion(ahora);

        usuarioRepository.save(usuario);

        crearTokenVerificacion(usuario);

        return new RegistroResponse(
                usuario.getIdUsuario(),
                "Usuario registrado correctamente. Debes verificar tu correo.",
                usuario.getEstado().name()
        );
    }

    // REGISTRO PROVEEDOR
    @Transactional
    public RegistroResponse registrarProveedor(RegistroProveedorRequest request) {

        validarContrasena(request.getContrasena());

        String correo = request.getCorreo().trim();
        String codigo = request.getCodigoProveedor().trim();

        if (usuarioRepository.existsByCorreo(correo)) {
            throw new BusinessException(
                    "CORREO_EXISTENTE",
                    "Este correo ya está registrado"
            );
        }

        CodigoProveedor codigoProveedor = codigoProveedorRepository
                .findByCodigoAndEstado(codigo, EstadoCodigoProveedor.DISPONIBLE)
                .orElseThrow(() -> new BusinessException(
                        "CODIGO_PROVEEDOR_INVALIDO",
                        "El código de proveedor no es válido o no está disponible"
                ));

        LocalDateTime ahora = LocalDateTime.now();

        if (codigoProveedor.getFechaExpiracion() != null
                && codigoProveedor.getFechaExpiracion().isBefore(ahora)) {

            codigoProveedor.setEstado(EstadoCodigoProveedor.EXPIRADO);
            codigoProveedorRepository.save(codigoProveedor);

            throw new BusinessException(
                    "CODIGO_PROVEEDOR_EXPIRADO",
                    "El código de proveedor está expirado"
            );
        }

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(request.getNombreCompleto().trim());
        usuario.setCorreo(correo);
        usuario.setHashContrasena(passwordEncoder.encode(request.getContrasena()));
        usuario.setRol(Rol.PROVEEDOR);
        usuario.setEstado(EstadoUsuario.PENDIENTE);
        usuario.setIntentosFallidos(0);
        usuario.setFechaCreacion(ahora);
        usuario.setFechaActualizacion(ahora);

        usuarioRepository.save(usuario);

        Proveedor proveedor = new Proveedor();
        proveedor.setUsuario(usuario);
        proveedor.setCodigoProveedor(codigoProveedor);
        proveedor.setTipoServicio(request.getTipoServicio().trim());
        proveedor.setDescripcion(request.getDescripcion());

        proveedorRepository.save(proveedor);

        codigoProveedor.setEstado(EstadoCodigoProveedor.USADO);
        codigoProveedor.setFechaUso(ahora);

        codigoProveedorRepository.save(codigoProveedor);

        crearTokenVerificacion(usuario);

        return new RegistroResponse(
                usuario.getIdUsuario(),
                "Proveedor registrado correctamente. Debes verificar tu correo.",
                usuario.getEstado().name()
        );
    }

    // LOGIN
    @Transactional
    public LoginResponse login(LoginRequest request, String ipOrigen, String userAgent) {

        String correo = request.getCorreo().trim();

        Usuario usuario = usuarioRepository
                .findByCorreo(correo)
                .orElse(null);

        if (usuario == null) {

            registrarIntentoFallido(null, ipOrigen, userAgent);

            throw new BusinessException(
                    "CREDENCIALES_INVALIDAS",
                    "Correo o contraseña incorrectos"
            );
        }

        LocalDateTime ahora = LocalDateTime.now();

        if (usuario.getBloqueadoHasta() != null
                && usuario.getBloqueadoHasta().isAfter(ahora)) {

            registrarIntentoFallido(usuario, ipOrigen, userAgent);

            throw new BusinessException(
                    "CUENTA_BLOQUEADA",
                    "La cuenta está temporalmente bloqueada. Intenta nuevamente más tarde"
            );
        }

        if (usuario.getBloqueadoHasta() != null
                && !usuario.getBloqueadoHasta().isAfter(ahora)) {

            usuario.setBloqueadoHasta(null);
            usuario.setIntentosFallidos(0);

            usuarioRepository.save(usuario);
        }

        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {

            registrarIntentoFallido(usuario, ipOrigen, userAgent);

            throw new BusinessException(
                    "CUENTA_INACTIVA",
                    "Esta cuenta está inactiva. Contacte al administrador"
            );
        }

        if (usuario.getEstado() == EstadoUsuario.PENDIENTE) {

            registrarIntentoFallido(usuario, ipOrigen, userAgent);

            throw new BusinessException(
                    "CUENTA_NO_VERIFICADA",
                    "Debes verificar tu correo antes de iniciar sesión"
            );
        }

        boolean contrasenaCorrecta =
                passwordEncoder.matches(
                        request.getContrasena(),
                        usuario.getHashContrasena()
                );

        if (!contrasenaCorrecta) {

            manejarIntentoFallido(usuario, ipOrigen, userAgent);

            throw new BusinessException(
                    "CREDENCIALES_INVALIDAS",
                    "Correo o contraseña incorrectos"
            );
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);

        usuarioRepository.save(usuario);

        registrarIntentoExitoso(usuario, ipOrigen, userAgent);

        String tokenSesion = crearSesion(usuario, ipOrigen, userAgent);

        return new LoginResponse(
                usuario.getIdUsuario(),
                usuario.getNombreCompleto(),
                usuario.getCorreo(),
                usuario.getRol().name(),
                usuario.getEstado().name(),
                tokenSesion,
                "Inicio de sesión exitoso"
        );
    }

    // VERIFICACIÓN DE CORREO
    @Transactional
    public void verificarCorreo(String token) {

        if (token == null || token.isBlank()) {

            throw new BusinessException(
                    "TOKEN_INVALIDO",
                    "El token de verificación no es válido"
            );
        }

        String hashToken = hashToken(token);

        Token tokenEntity =
                tokenRepository
                        .findByHashTokenAndTipoAndFechaUsoIsNull(
                                hashToken,
                                TipoToken.VERIFICACION
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "TOKEN_INVALIDO",
                                        "El token de verificación no es válido"
                                )
                        );

        LocalDateTime ahora = LocalDateTime.now();

        if (tokenEntity.getFechaExpiracion().isBefore(ahora)) {

            throw new BusinessException(
                    "TOKEN_EXPIRADO",
                    "El enlace de verificación ha expirado"
            );
        }

        Usuario usuario = tokenEntity.getUsuario();

        if (usuario.getEstado() == EstadoUsuario.ACTIVO) {

            throw new BusinessException(
                    "CUENTA_YA_VERIFICADA",
                    "La cuenta ya se encuentra verificada"
            );
        }

        usuario.setEstado(EstadoUsuario.ACTIVO);

        usuarioRepository.save(usuario);

        tokenEntity.setFechaUso(ahora);

        tokenRepository.save(tokenEntity);
    }

    // SOLICITAR RECUPERACIÓN DE CONTRASEÑA
    @Transactional
    public void solicitarRecuperacion(String correo) {

        String correoNormalizado = correo.trim();

        Usuario usuario = usuarioRepository
                .findByCorreo(correoNormalizado)
                .orElse(null);

        /*
         * Por seguridad, no revelamos si el correo existe o no. Si el usuario existe, generamos el token;
         * si no,simplemente no hacemos nada, pero el controller siempre responde el mismo mensaje genérico.
         */
        if (usuario != null) {
            crearTokenRecuperacion(usuario);
        }
    }

    // CONFIRMAR RECUPERACIÓN DE CONTRASEÑA
    @Transactional
    public void confirmarRecuperacion(String token, String nuevaContrasena) {

        if (token == null || token.isBlank()) {
            throw new BusinessException(
                    "TOKEN_INVALIDO",
                    "El token de recuperación no es válido"
            );
        }

        validarContrasena(nuevaContrasena);

        String hashToken = hashToken(token);

        Token tokenEntity =
                tokenRepository
                        .findByHashTokenAndTipoAndFechaUsoIsNull(
                                hashToken,
                                TipoToken.RECUPERACION
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "TOKEN_INVALIDO",
                                        "El token de recuperación no es válido"
                                )
                        );

        LocalDateTime ahora = LocalDateTime.now();

        if (tokenEntity.getFechaExpiracion().isBefore(ahora)) {
            throw new BusinessException(
                    "TOKEN_EXPIRADO",
                    "El enlace de recuperación ha expirado"
            );
        }

        Usuario usuario = tokenEntity.getUsuario();

        usuario.setHashContrasena(
                passwordEncoder.encode(nuevaContrasena)
        );

        usuarioRepository.save(usuario);

        tokenEntity.setFechaUso(ahora);
        tokenRepository.save(tokenEntity);

        /*
         * Invalidar todas las sesiones activas del usuario,
         * como se solicita en HU-05: "se invalidan las sesiones activas anteriores por seguridad".
         */
        List<Sesion> sesionesActivas =
                sesionRepository
                        .findByUsuarioIdUsuarioAndFechaRevocacionIsNull(
                                usuario.getIdUsuario()
                        );

        for (Sesion sesion : sesionesActivas) {
            sesion.setFechaRevocacion(ahora);
        }

        sesionRepository.saveAll(sesionesActivas);
    }

    // CREAR TOKEN DE VERIFICACIÓN
   private void crearTokenVerificacion(Usuario usuario) {

        String token = generarToken();

        Token tokenEntity = new Token();

        tokenEntity.setUsuario(usuario);
        tokenEntity.setHashToken(hashToken(token));
        tokenEntity.setTipo(TipoToken.VERIFICACION);

        tokenEntity.setFechaExpiracion(
                LocalDateTime.now().plusHours(24)
        );

        tokenRepository.save(tokenEntity);

        /*
         * El enlace apunta directo al backend (GET /api/auth/verificar/{token}),
         * ya que no hay frontend en este sprint: al hacer clic, la verificación se completa sin pasos adicionales.
         */
        String enlace = backendUrl + "/api/auth/verificar/" + token;

        emailService.enviarCorreoVerificacion(
                usuario.getCorreo(),
                enlace,
                tokenEntity.getFechaExpiracion()
        );

        // Se mantiene el log en consola como respaldo durante el desarrollo
        System.out.println();
        System.out.println("==================================================");
        System.out.println("TOKEN DE VERIFICACIÓN (enviado también por correo)");
        System.out.println("Usuario: " + usuario.getCorreo());
        System.out.println("Enlace: " + enlace);
        System.out.println("Expira: " + tokenEntity.getFechaExpiracion());
        System.out.println("==================================================");
        System.out.println();
    }

    // CREAR TOKEN DE RECUPERACIÓN
    private void crearTokenRecuperacion(Usuario usuario) {

        String token = generarToken();

        Token tokenEntity = new Token();

        tokenEntity.setUsuario(usuario);
        tokenEntity.setHashToken(hashToken(token));
        tokenEntity.setTipo(TipoToken.RECUPERACION);

        // Expiración corta según HU-05 (1 hora)
        tokenEntity.setFechaExpiracion(
                LocalDateTime.now().plusHours(1)
        );

        tokenRepository.save(tokenEntity);

        String enlace = frontendUrl + "/recuperar-contrasena?token=" + token;

        emailService.enviarCorreoRecuperacion(
                usuario.getCorreo(),
                enlace,
                tokenEntity.getFechaExpiracion()
        );
    }

    // CREAR SESIÓN
    private String crearSesion(Usuario usuario, String ipOrigen, String userAgent) {

        String token = generarToken();

        LocalDateTime ahora = LocalDateTime.now();

        Sesion sesion = new Sesion();

        sesion.setUsuario(usuario);
        sesion.setHashToken(hashToken(token));
        sesion.setFechaCreacion(ahora);
        sesion.setFechaExpiracion(ahora.plusHours(24));
        sesion.setIpOrigen(ipOrigen);
        sesion.setUserAgent(userAgent);

        sesionRepository.save(sesion);

        return token;
    }

    // GENERAR TOKEN ALEATORIO
    private String generarToken() {

        byte[] bytes = new byte[32];

        secureRandom.nextBytes(bytes);

        return HexFormat.of().formatHex(bytes);
    }

    // GENERAR HASH SHA-256
    private String hashToken(String token) {
        return com.reservar.security.TokenHasher.hash(token);
    }

    // MANEJAR INTENTO FALLIDO
    private void manejarIntentoFallido(Usuario usuario, String ipOrigen, String userAgent) {

        int intentos = usuario.getIntentosFallidos() + 1;

        usuario.setIntentosFallidos(intentos);

        if (intentos >= 5) {

            usuario.setBloqueadoHasta(
                    LocalDateTime.now().plusMinutes(15)
            );

            usuario.setIntentosFallidos(0);
        }

        usuarioRepository.save(usuario);

        registrarIntentoFallido(usuario, ipOrigen, userAgent);
    }

    // REGISTRAR INTENTO FALLIDO
    private void registrarIntentoFallido(Usuario usuario, String ipOrigen, String userAgent) {

        IntentoLogueo intento = new IntentoLogueo();

        intento.setUsuario(usuario);
        intento.setExitoso(false);
        intento.setIpOrigen(ipOrigen);
        intento.setUserAgent(userAgent);

        intentoLogueoRepository.save(intento);
    }

    // REGISTRAR LOGIN EXITOSO
    private void registrarIntentoExitoso(Usuario usuario, String ipOrigen, String userAgent) {

        IntentoLogueo intento = new IntentoLogueo();

        intento.setUsuario(usuario);
        intento.setExitoso(true);
        intento.setIpOrigen(ipOrigen);
        intento.setUserAgent(userAgent);

        intentoLogueoRepository.save(intento);
    }

    // VALIDAR CONTRASEÑA
    private void validarContrasena(String contrasena) {

        List<String> errores = PasswordValidator.validate(contrasena);

        if (!errores.isEmpty()) {

            throw new BusinessException(
                    "CONTRASENA_INVALIDA",
                    String.join(". ", errores)
            );
        }
    }
}