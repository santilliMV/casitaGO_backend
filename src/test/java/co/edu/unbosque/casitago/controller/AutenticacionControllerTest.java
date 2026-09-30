package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.*;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import co.edu.unbosque.casitago.service.AutenticacionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AutenticacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class AutenticacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AutenticacionService autenticacionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Usuario principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    // ---------- RF-01 ----------

    @Test
    void registrar_datosValidos_devuelve201() throws Exception {
        PerfilResponse respuesta = new PerfilResponse(
                UUID.randomUUID(), "Ana Ríos", "ana@example.com", RolUsuario.HUESPED, true, null);
        when(autenticacionService.registrar(any(RegistroRequest.class))).thenReturn(respuesta);

        String body = objectMapper.writeValueAsString(
                new RegistroRequest("Ana Ríos", "ana@example.com", "clave12345", RolUsuario.HUESPED));

        mockMvc.perform(post("/api/auth/registro")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("ana@example.com"))
                .andExpect(jsonPath("$.rol").value("HUESPED"));
    }

    @Test
    void registrar_correoInvalido_devuelve400() throws Exception {
        String body = """
                {"nombre":"Ana","correo":"no-es-un-correo","contrasena":"clave12345","rol":"HUESPED"}
                """;

        mockMvc.perform(post("/api/auth/registro")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrar_correoDuplicado_devuelve400() throws Exception {
        when(autenticacionService.registrar(any(RegistroRequest.class)))
                .thenThrow(new RuntimeException("Ya existe una cuenta registrada con ese correo."));

        String body = objectMapper.writeValueAsString(
                new RegistroRequest("Ana", "ana@example.com", "clave12345", RolUsuario.HUESPED));

        mockMvc.perform(post("/api/auth/registro")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Ya existe una cuenta registrada con ese correo."));
    }

    // ---------- RF-02 ----------

    @Test
    void login_credencialesValidas_devuelve200ConToken() throws Exception {
        LoginResponse respuesta = new LoginResponse("jwt-token", 1800L, UUID.randomUUID(), "Ana", RolUsuario.HUESPED);
        when(autenticacionService.login(any(LoginRequest.class))).thenReturn(respuesta);

        String body = objectMapper.writeValueAsString(new LoginRequest("ana@example.com", "clave12345"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_credencialesInvalidas_devuelve400ConMensajeFijo() throws Exception {
        when(autenticacionService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("mal"));

        String body = objectMapper.writeValueAsString(new LoginRequest("ana@example.com", "incorrecta"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Correo o contraseña inválidos"));
    }

    // ---------- RF-03 ----------

    @Test
    void solicitarRecuperacion_siempreDevuelve200_existaOnoElCorreo() throws Exception {
        String body = objectMapper.writeValueAsString(new SolicitarRecuperacionRequest("cualquiera@example.com"));

        mockMvc.perform(post("/api/auth/recuperacion/solicitar")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void confirmarRecuperacion_codigoInvalido_devuelve400() throws Exception {
        doThrow(new RuntimeException("Código inválido o expirado."))
                .when(autenticacionService).confirmarRecuperacion(any(ConfirmarRecuperacionRequest.class));

        String body = objectMapper.writeValueAsString(
                new ConfirmarRecuperacionRequest("ana@example.com", "000000", "nuevaClave123"));

        mockMvc.perform(post("/api/auth/recuperacion/confirmar")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Código inválido o expirado."));
    }

    @Test
    void confirmarRecuperacion_codigoValido_devuelve200() throws Exception {
        String body = objectMapper.writeValueAsString(
                new ConfirmarRecuperacionRequest("ana@example.com", "123456", "nuevaClave123"));

        mockMvc.perform(post("/api/auth/recuperacion/confirmar")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());
    }

    // ---------- RF-04 / RF-05 / RF-06 ----------

    @Test
    void obtenerPerfil_usuarioAutenticado_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        Usuario principal = usuarioDePrueba(id);
        autenticarComo(principal);
        PerfilResponse respuesta = new PerfilResponse(id, "Ana", "ana@example.com", RolUsuario.HUESPED, true, null);
        when(autenticacionService.obtenerPerfil(id)).thenReturn(respuesta);

        mockMvc.perform(get("/api/auth/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana@example.com"));
    }

    @Test
    void actualizarPerfil_usuarioAutenticado_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        Usuario principal = usuarioDePrueba(id);
        autenticarComo(principal);
        PerfilResponse respuesta = new PerfilResponse(id, "Ana Ríos", "ana.nueva@example.com", RolUsuario.HUESPED, true, null);
        when(autenticacionService.actualizarPerfil(any(), any(ActualizarPerfilRequest.class))).thenReturn(respuesta);

        String body = objectMapper.writeValueAsString(new ActualizarPerfilRequest("Ana Ríos", "ana.nueva@example.com"));

        mockMvc.perform(put("/api/auth/perfil")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Ríos"));
    }

    @Test
    void cambiarEstadoCuenta_usuarioAutenticado_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        Usuario principal = usuarioDePrueba(id);
        autenticarComo(principal);
        PerfilResponse respuesta = new PerfilResponse(id, "Ana", "ana@example.com", RolUsuario.HUESPED, false, null);
        when(autenticacionService.cambiarEstadoCuenta(any(), any(CambiarEstadoCuentaRequest.class))).thenReturn(respuesta);

        String body = objectMapper.writeValueAsString(new CambiarEstadoCuentaRequest(false));

        mockMvc.perform(patch("/api/auth/perfil/estado")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    private Usuario usuarioDePrueba(UUID id) {
        Usuario usuario = new Usuario("Ana", "ana@example.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    @Test
    void actualizarPerfil_correoDuplicado_devuelve400() throws Exception {
        UUID id = UUID.randomUUID();
        Usuario principal = usuarioDePrueba(id);
        autenticarComo(principal);
        when(autenticacionService.actualizarPerfil(any(), any(ActualizarPerfilRequest.class)))
                .thenThrow(new RuntimeException("Ya existe una cuenta registrada con ese correo."));

        String body = objectMapper.writeValueAsString(new ActualizarPerfilRequest("Ana Ríos", "ocupado@example.com"));

        mockMvc.perform(put("/api/auth/perfil")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Ya existe una cuenta registrada con ese correo."));
    }
    // ---------- RF-33: login con MFA ----------

    @Test
    void login_conMfaActivoSinCodigo_devuelve400ConElMensajeDelCodigo() throws Exception {
        when(autenticacionService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException(
                        "Se envió un código de verificación a tu correo. Vuelve a iniciar sesión incluyendo el código."));

        String body = objectMapper.writeValueAsString(new LoginRequest("ana@example.com", "clave12345"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "Se envió un código de verificación a tu correo. Vuelve a iniciar sesión incluyendo el código."));
    }

    @Test
    void login_conMfaActivoYCodigoValido_devuelve200ConToken() throws Exception {
        LoginResponse respuesta = new LoginResponse("jwt-token", 1800L, UUID.randomUUID(), "Ana", RolUsuario.HUESPED);
        when(autenticacionService.login(any(LoginRequest.class))).thenReturn(respuesta);

        String body = """
                {"correo":"ana@example.com","contrasena":"clave12345","codigoMfa":"123456"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    // ---------- RF-33: activar/desactivar MFA ----------

    @Test
    void cambiarMfa_datosValidos_devuelve200() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID()));

        String body = """
                {"habilitado":true,"contrasena":"clave12345"}
                """;

        mockMvc.perform(patch("/api/auth/mfa")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("Autenticación multifactor activada."));
    }

    @Test
    void cambiarMfa_desactivar_devuelveMensajeDeDesactivada() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID()));

        String body = """
                {"habilitado":false,"contrasena":"clave12345"}
                """;

        mockMvc.perform(patch("/api/auth/mfa")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("Autenticación multifactor desactivada."));
    }

    @Test
    void cambiarMfa_contrasenaIncorrecta_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID()));

        doThrow(new RuntimeException("La contraseña es incorrecta."))
                .when(autenticacionService).cambiarMfa(any(), any(CambiarMfaRequest.class));

        String body = """
                {"habilitado":true,"contrasena":"incorrecta"}
                """;

        mockMvc.perform(patch("/api/auth/mfa")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("La contraseña es incorrecta."));
    }

    @Test
    void cambiarMfa_sinContrasena_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID()));

        String body = """
                {"habilitado":true}
                """;

        mockMvc.perform(patch("/api/auth/mfa")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ---------- RF-06 con restricción (arreglo del controller) ----------

    @Test
    void cambiarEstadoCuenta_conRestriccionActiva_devuelve400ConElMensaje() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID()));

        when(autenticacionService.cambiarEstadoCuenta(any(), any(CambiarEstadoCuentaRequest.class)))
                .thenThrow(new RuntimeException("Tu cuenta tiene una restricción activa y no puede reactivarse."));

        String body = objectMapper.writeValueAsString(new CambiarEstadoCuentaRequest(true));

        mockMvc.perform(patch("/api/auth/perfil/estado")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tu cuenta tiene una restricción activa y no puede reactivarse."));
    }
}