package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.common.audit.AuditService;
import co.edu.unbosque.casitago.common.exception.BadRequestException;
import co.edu.unbosque.casitago.common.exception.ConflictException;
import co.edu.unbosque.casitago.common.exception.ResourceNotFoundException;
import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.*;
import co.edu.unbosque.casitago.entity.CodigoRecuperacion;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.CodigoRecuperacionRepository;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CodigoRecuperacionRepository codigoRecuperacionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;
    @Mock private AuditService auditService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                usuarioRepository, codigoRecuperacionRepository, passwordEncoder,
                authenticationManager, jwtService, emailService, auditService);
    }

    private Usuario usuarioConId(UUID id, String nombre, String correo, RolUsuario rol) {
        Usuario usuario = new Usuario(nombre, correo, "hash-existente", rol);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    // ---------- RF-01: registro ----------

    @Test
    void registrar_correoNuevo_creaUsuarioYRetornaPerfil() {
        RegistroRequest request = new RegistroRequest("Ana Ríos", "ana@example.com", "clave12345", RolUsuario.HUESPED);
        when(usuarioRepository.existsByCorreo("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("clave12345")).thenReturn("hash-seguro");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            return u;
        });

        PerfilResponse response = authService.registrar(request);

        assertThat(response.nombre()).isEqualTo("Ana Ríos");
        assertThat(response.correo()).isEqualTo("ana@example.com");
        assertThat(response.rol()).isEqualTo(RolUsuario.HUESPED);
        assertThat(response.activo()).isTrue();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hash-seguro"); // nunca la contraseña en texto plano

        verify(auditService).registrar(any(UUID.class), eq("usuarios"), eq("REGISTRO"), eq("EXITOSO"), any());
    }

    @Test
    void registrar_correoYaExistente_lanzaConflictException() {
        RegistroRequest request = new RegistroRequest("Ana", "ana@example.com", "clave12345", RolUsuario.HUESPED);
        when(usuarioRepository.existsByCorreo("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(ConflictException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_conRolAdministrador_esRechazadoPorElDto() {
        // RF-01: ADMINISTRADOR no puede autoasignarse; esto lo valida el propio record.
        assertThatThrownBy(() ->
                new RegistroRequest("X", "x@example.com", "clave12345", RolUsuario.ADMINISTRADOR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- RF-02: login ----------

    @Test
    void login_credencialesValidas_retornaTokenYDatosDelUsuario() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.HUESPED);
        Authentication authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generarToken(id, "ana@example.com", "HUESPED")).thenReturn("jwt-generado");
        when(jwtService.getExpiracionSegundos()).thenReturn(1800L);

        LoginResponse response = authService.login(new LoginRequest("ana@example.com", "clave12345"));

        assertThat(response.token()).isEqualTo("jwt-generado");
        assertThat(response.expiraEnSegundos()).isEqualTo(1800L);
        assertThat(response.usuarioId()).isEqualTo(id);
        assertThat(response.rol()).isEqualTo(RolUsuario.HUESPED);

        verify(auditService).registrar(id, "usuarios", "LOGIN", "EXITOSO");
    }

    // Nota: credenciales inválidas / cuenta desactivada no se prueban aquí porque
    // esa lógica vive en AuthenticationManager/DaoAuthenticationProvider (Spring
    // Security), no en AuthService; se prueban con BadCredentialsException /
    // DisabledException mapeadas en GlobalExceptionHandler.

    // ---------- RF-03: recuperación de contraseña ----------

    @Test
    void solicitarRecuperacion_usuarioExiste_generaCodigoYEnviaCorreo() {
        Usuario usuario = usuarioConId(UUID.randomUUID(), "Ana", "ana@example.com", RolUsuario.HUESPED);
        when(usuarioRepository.findByCorreo("ana@example.com")).thenReturn(Optional.of(usuario));

        authService.solicitarRecuperacion(new SolicitarRecuperacionRequest("ana@example.com"));

        ArgumentCaptor<CodigoRecuperacion> captor = ArgumentCaptor.forClass(CodigoRecuperacion.class);
        verify(codigoRecuperacionRepository).save(captor.capture());
        assertThat(captor.getValue().getCodigo()).hasSize(6);
        assertThat(captor.getValue().getExpiraEn()).isAfter(OffsetDateTime.now());

        verify(emailService).enviarCodigoRecuperacion(eq("ana@example.com"), eq("Ana"), anyString());
    }

    @Test
    void solicitarRecuperacion_usuarioNoExiste_noGeneraNiEnviaNada() {
        when(usuarioRepository.findByCorreo("fantasma@example.com")).thenReturn(Optional.empty());

        // No debe lanzar excepción (evita revelar qué correos existen).
        authService.solicitarRecuperacion(new SolicitarRecuperacionRequest("fantasma@example.com"));

        verifyNoInteractions(codigoRecuperacionRepository, emailService);
    }

    @Test
    void confirmarRecuperacion_codigoValido_actualizaContrasenaYMarcaCodigoUsado() {
        Usuario usuario = usuarioConId(UUID.randomUUID(), "Ana", "ana@example.com", RolUsuario.HUESPED);
        CodigoRecuperacion codigo = new CodigoRecuperacion(usuario, "123456", OffsetDateTime.now().plusMinutes(10));

        when(usuarioRepository.findByCorreo("ana@example.com")).thenReturn(Optional.of(usuario));
        when(codigoRecuperacionRepository.findFirstByUsuarioAndCodigoOrderByExpiraEnDesc(usuario, "123456"))
                .thenReturn(Optional.of(codigo));
        when(passwordEncoder.encode("nuevaClave123")).thenReturn("hash-nuevo");

        authService.confirmarRecuperacion(new ConfirmarRecuperacionRequest("ana@example.com", "123456", "nuevaClave123"));

        assertThat(usuario.getPassword()).isEqualTo("hash-nuevo");
        assertThat(codigo.isUsado()).isTrue();
        verify(auditService).registrar(usuario.getId(), "usuarios", "CONFIRMACION_RECUPERACION_PASSWORD", "EXITOSO");
    }

    @Test
    void confirmarRecuperacion_codigoExpirado_lanzaBadRequestException() {
        Usuario usuario = usuarioConId(UUID.randomUUID(), "Ana", "ana@example.com", RolUsuario.HUESPED);
        CodigoRecuperacion codigoVencido = new CodigoRecuperacion(usuario, "123456", OffsetDateTime.now().minusMinutes(1));

        when(usuarioRepository.findByCorreo("ana@example.com")).thenReturn(Optional.of(usuario));
        when(codigoRecuperacionRepository.findFirstByUsuarioAndCodigoOrderByExpiraEnDesc(usuario, "123456"))
                .thenReturn(Optional.of(codigoVencido));

        assertThatThrownBy(() -> authService.confirmarRecuperacion(
                new ConfirmarRecuperacionRequest("ana@example.com", "123456", "nuevaClave123")))
                .isInstanceOf(BadRequestException.class);

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void confirmarRecuperacion_correoInexistente_lanzaBadRequestException() {
        when(usuarioRepository.findByCorreo("fantasma@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.confirmarRecuperacion(
                new ConfirmarRecuperacionRequest("fantasma@example.com", "123456", "nuevaClave123")))
                .isInstanceOf(BadRequestException.class);
    }

    // ---------- RF-04: consultar perfil ----------

    @Test
    void obtenerPerfil_usuarioExiste_retornaPerfil() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.ANFITRION);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        PerfilResponse response = authService.obtenerPerfil(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.rol()).isEqualTo(RolUsuario.ANFITRION);
    }

    @Test
    void obtenerPerfil_usuarioNoExiste_lanzaResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.obtenerPerfil(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- RF-05: actualizar perfil ----------

    @Test
    void actualizarPerfil_datosValidos_actualizaNombreYCorreo() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.HUESPED);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByCorreo("ana.nueva@example.com")).thenReturn(false);

        PerfilResponse response = authService.actualizarPerfil(id,
                new ActualizarPerfilRequest("Ana Ríos", "ana.nueva@example.com"));

        assertThat(response.nombre()).isEqualTo("Ana Ríos");
        assertThat(response.correo()).isEqualTo("ana.nueva@example.com");
    }

    @Test
    void actualizarPerfil_correoYaUsadoPorOtroUsuario_lanzaConflictException() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.HUESPED);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByCorreo("ocupado@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.actualizarPerfil(id,
                new ActualizarPerfilRequest("Ana Ríos", "ocupado@example.com")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void actualizarPerfil_mismoCorreoActual_noValidaDuplicado() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.HUESPED);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        PerfilResponse response = authService.actualizarPerfil(id,
                new ActualizarPerfilRequest("Ana Actualizada", "ana@example.com"));

        assertThat(response.nombre()).isEqualTo("Ana Actualizada");
        verify(usuarioRepository, never()).existsByCorreo(anyString());
    }

    // ---------- RF-06: activar/desactivar cuenta ----------

    @Test
    void cambiarEstadoCuenta_desactivar_actualizaFlagActivo() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.HUESPED);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        PerfilResponse response = authService.cambiarEstadoCuenta(id, new CambiarEstadoCuentaRequest(false));

        assertThat(response.activo()).isFalse();
        assertThat(usuario.isEnabled()).isFalse();
        verify(auditService).registrar(id, "usuarios", "DESACTIVAR_CUENTA", "EXITOSO");
    }

    @Test
    void cambiarEstadoCuenta_reactivar_actualizaFlagActivo() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.HUESPED);
        usuario.setActivo(false);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        PerfilResponse response = authService.cambiarEstadoCuenta(id, new CambiarEstadoCuentaRequest(true));

        assertThat(response.activo()).isTrue();
        verify(auditService).registrar(id, "usuarios", "ACTIVAR_CUENTA", "EXITOSO");
    }
}
