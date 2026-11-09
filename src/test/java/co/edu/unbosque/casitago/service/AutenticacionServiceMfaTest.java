package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.common.audit.AuditService;
import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.LoginRequest;
import co.edu.unbosque.casitago.dto.LoginResponse;
import co.edu.unbosque.casitago.entity.CodigoRecuperacion;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.CodigoRecuperacionRepository;
import co.edu.unbosque.casitago.repository.RestriccionUsuarioRepository;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticacionServiceMfaTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CodigoRecuperacionRepository codigoRecuperacionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;
    @Mock private AuditService auditService;
    @Mock private RestriccionUsuarioRepository restriccionUsuarioRepository;

    private AutenticacionService autenticacionService;

    @BeforeEach
    void setUp() {
        autenticacionService = new AutenticacionService(
                usuarioRepository, codigoRecuperacionRepository, passwordEncoder,
                authenticationManager, jwtService, emailService, auditService, restriccionUsuarioRepository);
    }

    private Usuario usuarioAutenticado(UUID id, boolean mfaHabilitado) {
        Usuario usuario = new Usuario("Ana", "ana@example.com", "hash-existente", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(usuario, "id", id);
        usuario.setMfaHabilitado(mfaHabilitado);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
        return usuario;
    }

    @Test
    void login_mfaActivoConCodigoEnBlanco_pideElCodigoSinToken() {
        UUID id = UUID.randomUUID();
        usuarioAutenticado(id, true);
        LoginRequest request = new LoginRequest("ana@example.com", "clave12345");
        request.setCodigoMfa("   ");

        LoginResponse response = autenticacionService.login(request);

        assertThat(response.isMfaRequerido()).isTrue();
        assertThat(response.getToken()).isNull();
        verify(emailService).enviarCodigoMfa(eq("ana@example.com"), eq("Ana"), anyString());
        verify(jwtService, never()).generarToken(any(), any(), any());
    }

    @Test
    void login_mfaActivoConCodigoVacio_pideElCodigoSinToken() {
        UUID id = UUID.randomUUID();
        usuarioAutenticado(id, true);
        LoginRequest request = new LoginRequest("ana@example.com", "clave12345");
        request.setCodigoMfa("");

        LoginResponse response = autenticacionService.login(request);

        assertThat(response.isMfaRequerido()).isTrue();
        assertThat(response.getToken()).isNull();
        verify(emailService).enviarCodigoMfa(eq("ana@example.com"), eq("Ana"), anyString());
    }

    @Test
    void login_sinMfa_noEnviaCodigoYEntregaToken() {
        UUID id = UUID.randomUUID();
        usuarioAutenticado(id, false);
        when(jwtService.generarToken(id, "ana@example.com", "HUESPED")).thenReturn("jwt-generado");
        when(jwtService.getExpiracionSegundos()).thenReturn(1800L);

        LoginResponse response = autenticacionService.login(new LoginRequest("ana@example.com", "clave12345"));

        assertThat(response.isMfaRequerido()).isFalse();
        assertThat(response.getToken()).isEqualTo("jwt-generado");
        verify(emailService, never()).enviarCodigoMfa(any(), any(), any());
    }

    @Test
    void login_mfaActivoYCodigoValido_noVuelveAEnviarCodigoYEntregaToken() {
        UUID id = UUID.randomUUID();
        Usuario usuario = usuarioAutenticado(id, true);
        CodigoRecuperacion codigo = new CodigoRecuperacion(usuario, "123456", OffsetDateTime.now().plusMinutes(5));
        when(codigoRecuperacionRepository.findFirstByUsuarioAndCodigoOrderByExpiraEnDesc(usuario, "123456"))
                .thenReturn(Optional.of(codigo));
        when(jwtService.generarToken(id, "ana@example.com", "HUESPED")).thenReturn("jwt-generado");
        when(jwtService.getExpiracionSegundos()).thenReturn(1800L);
        LoginRequest request = new LoginRequest("ana@example.com", "clave12345");
        request.setCodigoMfa("123456");

        LoginResponse response = autenticacionService.login(request);

        assertThat(response.isMfaRequerido()).isFalse();
        assertThat(response.getToken()).isEqualTo("jwt-generado");
        verify(emailService, never()).enviarCodigoMfa(any(), any(), any());
    }
}