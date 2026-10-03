package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.common.audit.AuditService;
import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.*;
import co.edu.unbosque.casitago.entity.CodigoRecuperacion;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.CodigoRecuperacionRepository;
import co.edu.unbosque.casitago.repository.RestriccionUsuarioRepository;
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
import org.springframework.security.authentication.DisabledException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticacionServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CodigoRecuperacionRepository codigoRecuperacionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;
    @Mock private AuditService auditService;
    @Mock
    private RestriccionUsuarioRepository restriccionUsuarioRepository;

    private AutenticacionService autenticacionService;

    @BeforeEach
    void setUp() {
        autenticacionService = new AutenticacionService(
                usuarioRepository, codigoRecuperacionRepository, passwordEncoder,
                authenticationManager, jwtService, emailService, auditService, restriccionUsuarioRepository);
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

        PerfilResponse response = autenticacionService.registrar(request);

        assertThat(response.getNombre()).isEqualTo("Ana Ríos");
        assertThat(response.getCorreo()).isEqualTo("ana@example.com");
        assertThat(response.getRol()).isEqualTo(RolUsuario.HUESPED);
        assertThat(response.isActivo()).isTrue();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hash-seguro");

        verify(auditService).registrar(any(UUID.class), eq("usuarios"), eq("REGISTRO"), eq("EXITOSO"), any());
    }

    @Test
    void registrar_correoYaExistente_lanzaExcepcion() {
        RegistroRequest request = new RegistroRequest("Ana", "ana@example.com", "clave12345", RolUsuario.HUESPED);
        when(usuarioRepository.existsByCorreo("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> autenticacionService.registrar(request))
                .isInstanceOf(RuntimeException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_conRolAdministrador_lanzaExcepcion() {
        RegistroRequest request = new RegistroRequest("X", "x@example.com", "clave12345", RolUsuario.ADMINISTRADOR);

        assertThatThrownBy(() -> autenticacionService.registrar(request))
                .isInstanceOf(RuntimeException.class);

        verify(usuarioRepository, never()).save(any());
    }

    // ---------- RF-02: login ----------

    @Test
    void login_cuentaDesactivadaConContrasenaCorrecta_lanzaMensajeDeCuentaDesactivada() {
        Usuario usuario = usuarioConId(UUID.randomUUID(), "Ana", "ana@example.com", RolUsuario.HUESPED);
        usuario.setActivo(false);
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("User is disabled"));
        when(usuarioRepository.findByCorreo("ana@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave12345", "hash-existente")).thenReturn(true);
        LoginRequest request = new LoginRequest("ana@example.com", "clave12345");

        assertThatThrownBy(() -> autenticacionService.login(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Tu cuenta está desactivada.");

        verify(jwtService, never()).generarToken(any(), any(), any());
    }

    @Test
    void login_cuentaDesactivadaConContrasenaIncorrecta_relanzaElErrorDeAutenticacion() {
        Usuario usuario = usuarioConId(UUID.randomUUID(), "Ana", "ana@example.com", RolUsuario.HUESPED);
        usuario.setActivo(false);
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("User is disabled"));
        when(usuarioRepository.findByCorreo("ana@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta", "hash-existente")).thenReturn(false);
        LoginRequest request = new LoginRequest("ana@example.com", "incorrecta");

        assertThatThrownBy(() -> autenticacionService.login(request))
                .isInstanceOf(DisabledException.class);

        verify(jwtService, never()).generarToken(any(), any(), any());
    }

    @Test
    void login_cuentaDesactivadaConCorreoInexistente_relanzaElErrorDeAutenticacion() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("User is disabled"));
        when(usuarioRepository.findByCorreo("fantasma@example.com")).thenReturn(Optional.empty());
        LoginRequest request = new LoginRequest("fantasma@example.com", "clave12345");

        assertThatThrownBy(() -> autenticacionService.login(request))
                .isInstanceOf(DisabledException.class);

        verifyNoInteractions(passwordEncoder);
    }

    // ---------- RF-03: recuperación de contraseña ----------

    @Test
    void solicitarRecuperacion_usuarioExiste_generaCodigoYEnviaCorreo() {
        Usuario usuario = usuarioConId(UUID.randomUUID(), "Ana", "ana@example.com", RolUsuario.HUESPED);
        when(usuarioRepository.findByCorreo("ana@example.com")).thenReturn(Optional.of(usuario));

        autenticacionService.solicitarRecuperacion(new SolicitarRecuperacionRequest("ana@example.com"));

        ArgumentCaptor<CodigoRecuperacion> captor = ArgumentCaptor.forClass(CodigoRecuperacion.class);
        verify(codigoRecuperacionRepository).save(captor.capture());
        assertThat(captor.getValue().getCodigo()).hasSize(6);
        assertThat(captor.getValue().getExpiraEn()).isAfter(OffsetDateTime.now());

        verify(emailService).enviarCodigoRecuperacion(eq("ana@example.com"), eq("Ana"), anyString());
    }

    @Test
    void solicitarRecuperacion_usuarioNoExiste_noGeneraNiEnviaNada() {
        when(usuarioRepository.findByCorreo("fantasma@example.com")).thenReturn(Optional.empty());

        autenticacionService.solicitarRecuperacion(new SolicitarRecuperacionRequest("fantasma@example.com"));

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

        autenticacionService.confirmarRecuperacion(new ConfirmarRecuperacionRequest("ana@example.com", "123456", "nuevaClave123"));

        assertThat(usuario.getPassword()).isEqualTo("hash-nuevo");
        assertThat(codigo.isUsado()).isTrue();
        verify(auditService).registrar(usuario.getId(), "usuarios", "CONFIRMACION_RECUPERACION_PASSWORD", "EXITOSO");
    }

    @Test
    void confirmarRecuperacion_codigoExpirado_lanzaExcepcion() {
        Usuario usuario = usuarioConId(UUID.randomUUID(), "Ana", "ana@example.com", RolUsuario.HUESPED);
        CodigoRecuperacion codigoVencido = new CodigoRecuperacion(usuario, "123456", OffsetDateTime.now().minusMinutes(1));

        when(usuarioRepository.findByCorreo("ana@example.com")).thenReturn(Optional.of(usuario));
        when(codigoRecuperacionRepository.findFirstByUsuarioAndCodigoOrderByExpiraEnDesc(usuario, "123456"))
                .thenReturn(Optional.of(codigoVencido));

        assertThatThrownBy(() -> autenticacionService.confirmarRecuperacion(
                new ConfirmarRecuperacionRequest("ana@example.com", "123456", "nuevaClave123")))
                .isInstanceOf(RuntimeException.class);

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void confirmarRecuperacion_correoInexistente_lanzaExcepcion() {
        when(usuarioRepository.findByCorreo("fantasma@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> autenticacionService.confirmarRecuperacion(
                new ConfirmarRecuperacionRequest("fantasma@example.com", "123456", "nuevaClave123")))
                .isInstanceOf(RuntimeException.class);
    }

    // ---------- RF-04: consultar perfil ----------

    @Test
    void obtenerPerfil_usuarioExiste_retornaPerfil() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.ANFITRION);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        PerfilResponse response = autenticacionService.obtenerPerfil(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getRol()).isEqualTo(RolUsuario.ANFITRION);
    }

    @Test
    void obtenerPerfil_usuarioNoExiste_lanzaExcepcion() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> autenticacionService.obtenerPerfil(id))
                .isInstanceOf(RuntimeException.class);
    }

    // ---------- RF-05: actualizar perfil ----------

    @Test
    void actualizarPerfil_datosValidos_actualizaNombreYCorreo() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.HUESPED);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByCorreo("ana.nueva@example.com")).thenReturn(false);

        PerfilResponse response = autenticacionService.actualizarPerfil(id,
                new ActualizarPerfilRequest("Ana Ríos", "ana.nueva@example.com"));

        assertThat(response.getNombre()).isEqualTo("Ana Ríos");
        assertThat(response.getCorreo()).isEqualTo("ana.nueva@example.com");
    }

    @Test
    void actualizarPerfil_correoYaUsadoPorOtroUsuario_lanzaExcepcion() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.HUESPED);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByCorreo("ocupado@example.com")).thenReturn(true);

        assertThatThrownBy(() -> autenticacionService.actualizarPerfil(id,
                new ActualizarPerfilRequest("Ana Ríos", "ocupado@example.com")))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void actualizarPerfil_mismoCorreoActual_noValidaDuplicado() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.HUESPED);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        PerfilResponse response = autenticacionService.actualizarPerfil(id,
                new ActualizarPerfilRequest("Ana Actualizada", "ana@example.com"));

        assertThat(response.getNombre()).isEqualTo("Ana Actualizada");
        verify(usuarioRepository, never()).existsByCorreo(anyString());
    }

    // ---------- RF-06: activar/desactivar cuenta ----------

    @Test
    void cambiarEstadoCuenta_desactivar_actualizaFlagActivo() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.HUESPED);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        PerfilResponse response = autenticacionService.cambiarEstadoCuenta(id, new CambiarEstadoCuentaRequest(false));

        assertThat(response.isActivo()).isFalse();
        assertThat(usuario.isEnabled()).isFalse();
        verify(auditService).registrar(id, "usuarios", "DESACTIVAR_CUENTA", "EXITOSO");
    }

    @Test
    void cambiarEstadoCuenta_reactivarConRestriccionActiva_lanzaExcepcion() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.HUESPED);
        usuario.setActivo(false);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(restriccionUsuarioRepository.existsByUsuarioIdAndActivaTrue(id)).thenReturn(true);

        assertThatThrownBy(() -> autenticacionService.cambiarEstadoCuenta(id, new CambiarEstadoCuentaRequest(true)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Tu cuenta tiene una restricción activa y no puede reactivarse.");

        assertThat(usuario.isEnabled()).isFalse();
    }

    @Test
    void cambiarEstadoCuenta_reactivar_actualizaFlagActivo() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.HUESPED);
        usuario.setActivo(false);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        PerfilResponse response = autenticacionService.cambiarEstadoCuenta(id, new CambiarEstadoCuentaRequest(true));

        assertThat(response.isActivo()).isTrue();
        verify(auditService).registrar(id, "usuarios", "ACTIVAR_CUENTA", "EXITOSO");
    }

    // ---------- RF-33: login con MFA ----------

    @Test
    void login_conMfaActivoSinCodigo_enviaCodigoYLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.HUESPED);
        usuario.setMfaHabilitado(true);
        Authentication authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        assertThatThrownBy(() -> autenticacionService.login(new LoginRequest("ana@example.com", "clave12345")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Se envió un código de verificación");

        verify(codigoRecuperacionRepository).save(any(CodigoRecuperacion.class));
        verify(emailService).enviarCodigoMfa(eq("ana@example.com"), eq("Ana"), anyString());
        verify(jwtService, never()).generarToken(any(), any(), any());
    }

    @Test
    void login_conMfaActivoYCodigoValido_retornaToken() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.HUESPED);
        usuario.setMfaHabilitado(true);
        Authentication authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
        CodigoRecuperacion codigo = new CodigoRecuperacion(usuario, "123456", OffsetDateTime.now().plusMinutes(5));

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(codigoRecuperacionRepository.findFirstByUsuarioAndCodigoOrderByExpiraEnDesc(usuario, "123456"))
                .thenReturn(Optional.of(codigo));
        when(jwtService.generarToken(id, "ana@example.com", "HUESPED")).thenReturn("jwt-generado");
        when(jwtService.getExpiracionSegundos()).thenReturn(1800L);

        LoginRequest request = new LoginRequest("ana@example.com", "clave12345");
        request.setCodigoMfa("123456");

        LoginResponse response = autenticacionService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-generado");
        assertThat(codigo.isUsado()).isTrue();
        verify(codigoRecuperacionRepository).save(codigo);
        verify(auditService).registrar(id, "usuarios", "LOGIN", "EXITOSO");
    }

    @Test
    void login_conMfaActivoYCodigoInvalido_lanzaExcepcionYNoEntregaToken() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.HUESPED);
        usuario.setMfaHabilitado(true);
        Authentication authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(codigoRecuperacionRepository.findFirstByUsuarioAndCodigoOrderByExpiraEnDesc(usuario, "000000"))
                .thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("ana@example.com", "clave12345");
        request.setCodigoMfa("000000");

        assertThatThrownBy(() -> autenticacionService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Código inválido o expirado.");

        verify(auditService).registrar(id, "usuarios", "LOGIN_MFA", "FALLIDO");
        verify(jwtService, never()).generarToken(any(), any(), any());
    }

    // ---------- RF-33: activar/desactivar MFA ----------

    @Test
    void cambiarMfa_activarConContrasenaCorrecta_activaMfa() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.HUESPED);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave12345", "hash-existente")).thenReturn(true);

        CambiarMfaRequest request = new CambiarMfaRequest();
        request.setHabilitado(true);
        request.setContrasena("clave12345");

        autenticacionService.cambiarMfa(id, request);

        assertThat(usuario.isMfaHabilitado()).isTrue();
        verify(auditService).registrar(id, "usuarios", "ACTIVAR_MFA", "EXITOSO");
    }

    @Test
    void cambiarMfa_desactivarConContrasenaCorrecta_desactivaMfa() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.HUESPED);
        usuario.setMfaHabilitado(true);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave12345", "hash-existente")).thenReturn(true);

        CambiarMfaRequest request = new CambiarMfaRequest();
        request.setHabilitado(false);
        request.setContrasena("clave12345");

        autenticacionService.cambiarMfa(id, request);

        assertThat(usuario.isMfaHabilitado()).isFalse();
        verify(auditService).registrar(id, "usuarios", "DESACTIVAR_MFA", "EXITOSO");
    }

    @Test
    void cambiarMfa_conContrasenaIncorrecta_lanzaExcepcionYNoCambiaNada() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioConId(id, "Ana", "ana@example.com", RolUsuario.HUESPED);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta", "hash-existente")).thenReturn(false);

        CambiarMfaRequest request = new CambiarMfaRequest();
        request.setHabilitado(true);
        request.setContrasena("incorrecta");

        assertThatThrownBy(() -> autenticacionService.cambiarMfa(id, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("La contraseña es incorrecta.");

        assertThat(usuario.isMfaHabilitado()).isFalse();
        verifyNoInteractions(auditService);
    }
}