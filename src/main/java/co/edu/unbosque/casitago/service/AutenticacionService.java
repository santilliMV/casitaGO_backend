package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.common.audit.AuditService;
import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.*;
import co.edu.unbosque.casitago.entity.CodigoRecuperacion;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.CodigoRecuperacionRepository;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class AutenticacionService {

    private static final int CODIGO_LONGITUD = 6;
    private static final long CODIGO_VIGENCIA_MINUTOS = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final CodigoRecuperacionRepository codigoRecuperacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AuditService auditService;

    public AutenticacionService(
            UsuarioRepository usuarioRepository,
            CodigoRecuperacionRepository codigoRecuperacionRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            EmailService emailService,
            AuditService auditService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.codigoRecuperacionRepository = codigoRecuperacionRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    // ---------- RF-01: registro ----------
    @Transactional
    public PerfilResponse registrar(RegistroRequest request) {
        if (request.getRol() == RolUsuario.ADMINISTRADOR) {
            throw new RuntimeException("El rol ADMINISTRADOR no puede autoasignarse en el registro.");
        }
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new RuntimeException("Ya existe una cuenta registrada con ese correo.");
        }

        Usuario usuario = new Usuario(
                request.getNombre(),
                request.getCorreo(),
                passwordEncoder.encode(request.getContrasena()),
                request.getRol()
        );
        usuario = usuarioRepository.save(usuario);

        auditService.registrar(usuario.getId(), "usuarios", "REGISTRO", "EXITOSO",
                Map.of("rol", usuario.getRol().name()));

        return PerfilResponse.desde(usuario);
    }

    // ---------- RF-02: login ----------
    public LoginResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getContrasena()));

        Usuario usuario = (Usuario) authentication.getPrincipal();
        String token = jwtService.generarToken(usuario.getId(), usuario.getCorreo(), usuario.getRol().name());

        auditService.registrar(usuario.getId(), "usuarios", "LOGIN", "EXITOSO");

        return new LoginResponse(
                token,
                jwtService.getExpiracionSegundos(),
                usuario.getId(),
                usuario.getNombre(),
                usuario.getRol()
        );
    }

    // ---------- RF-03: recuperación de contraseña ----------
    @Transactional
    public void solicitarRecuperacion(SolicitarRecuperacionRequest request) {
        // No se revela si el correo existe o no; si no existe, simplemente no
        // se genera/envía nada, pero la respuesta al cliente es la misma.
        usuarioRepository.findByCorreo(request.getCorreo()).ifPresent(usuario -> {
            String codigo = generarCodigoNumerico();
            OffsetDateTime expiraEn = OffsetDateTime.now().plusMinutes(CODIGO_VIGENCIA_MINUTOS);

            codigoRecuperacionRepository.save(new CodigoRecuperacion(usuario, codigo, expiraEn));
            emailService.enviarCodigoRecuperacion(usuario.getCorreo(), usuario.getNombre(), codigo);

            auditService.registrar(usuario.getId(), "usuarios", "SOLICITUD_RECUPERACION_PASSWORD", "EXITOSO");
        });
    }

    @Transactional
    public void confirmarRecuperacion(ConfirmarRecuperacionRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new RuntimeException("Código inválido o expirado."));

        CodigoRecuperacion codigo = codigoRecuperacionRepository
                .findFirstByUsuarioAndCodigoOrderByExpiraEnDesc(usuario, request.getCodigo())
                .filter(c -> c.esValido(request.getCodigo()))
                .orElse(null);

        if (codigo == null) {
            auditService.registrar(usuario.getId(), "usuarios", "CONFIRMACION_RECUPERACION_PASSWORD", "FALLIDO");
            throw new RuntimeException("Código inválido o expirado.");
        }

        usuario.setContrasenaHash(passwordEncoder.encode(request.getNuevaContrasena()));
        codigo.marcarUsado();

        auditService.registrar(usuario.getId(), "usuarios", "CONFIRMACION_RECUPERACION_PASSWORD", "EXITOSO");
    }

    // ---------- RF-04: consultar perfil propio ----------
    public PerfilResponse obtenerPerfil(UUID usuarioId) {
        Usuario usuario = buscarPorId(usuarioId);
        return PerfilResponse.desde(usuario);
    }

    // ---------- RF-05: actualizar datos básicos del perfil ----------
    @Transactional
    public PerfilResponse actualizarPerfil(UUID usuarioId, ActualizarPerfilRequest request) {
        Usuario usuario = buscarPorId(usuarioId);

        if (!usuario.getCorreo().equalsIgnoreCase(request.getCorreo())
                && usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new RuntimeException("Ya existe una cuenta registrada con ese correo.");
        }

        usuario.setNombre(request.getNombre());
        usuario.setCorreo(request.getCorreo());
        auditService.registrar(usuario.getId(), "usuarios", "ACTUALIZAR_PERFIL", "EXITOSO");

        return PerfilResponse.desde(usuario);
    }

    // ---------- RF-06: activar/desactivar cuenta ----------
    @Transactional
    public PerfilResponse cambiarEstadoCuenta(UUID usuarioId, CambiarEstadoCuentaRequest request) {
        Usuario usuario = buscarPorId(usuarioId);
        usuario.setActivo(request.getActivo());

        auditService.registrar(usuario.getId(), "usuarios",
                request.getActivo() ? "ACTIVAR_CUENTA" : "DESACTIVAR_CUENTA", "EXITOSO");

        return PerfilResponse.desde(usuario);
    }

    private Usuario buscarPorId(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
    }

    private String generarCodigoNumerico() {
        StringBuilder sb = new StringBuilder(CODIGO_LONGITUD);
        for (int i = 0; i < CODIGO_LONGITUD; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}