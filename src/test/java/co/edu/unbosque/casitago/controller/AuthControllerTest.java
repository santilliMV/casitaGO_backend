package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.common.exception.BadRequestException;
import co.edu.unbosque.casitago.common.exception.ConflictException;
import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.*;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import co.edu.unbosque.casitago.service.AuthService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test del controller: solo levanta la capa web (no seguridad real,
 * no base de datos). addFilters = false porque aquí se prueba el contrato
 * HTTP de AuthController, no JwtAuthenticationFilter (eso ya lo cubre
 * JwtServiceTest a nivel de la lógica de validación del token).
 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    // JwtAuthenticationFilter (bean tipo Filter) se instancia igual dentro del
    // contexto del slice aunque addFilters=false impida que se ejecute — por
    // eso sus dependencias también deben mockearse aquí, o el contexto no levanta.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @AfterEach
    void limpiarContextoDeSeguridad() {
        // SecurityContextHolder es un ThreadLocal estático: si no se limpia,
        // la autenticación de un test podría "filtrarse" al siguiente que
        // corra en el mismo hilo.
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
        when(authService.registrar(any(RegistroRequest.class))).thenReturn(respuesta);

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
        // Salta la validación del record (correo con formato inválido) y llega
        // como JSON crudo para probar el @Valid + GlobalExceptionHandler.
        String body = """
                {"nombre":"Ana","correo":"no-es-un-correo","contrasena":"clave12345","rol":"HUESPED"}
                """;

        mockMvc.perform(post("/api/auth/registro")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void registrar_correoDuplicado_devuelve409() throws Exception {
        when(authService.registrar(any(RegistroRequest.class)))
                .thenThrow(new ConflictException("Ya existe una cuenta registrada con ese correo."));

        String body = objectMapper.writeValueAsString(
                new RegistroRequest("Ana", "ana@example.com", "clave12345", RolUsuario.HUESPED));

        mockMvc.perform(post("/api/auth/registro")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict());
    }

    // ---------- RF-02 ----------

    @Test
    void login_credencialesValidas_devuelve200ConToken() throws Exception {
        LoginResponse respuesta = new LoginResponse("jwt-token", 1800L, UUID.randomUUID(), "Ana", RolUsuario.HUESPED);
        when(authService.login(any(LoginRequest.class))).thenReturn(respuesta);

        String body = objectMapper.writeValueAsString(new LoginRequest("ana@example.com", "clave12345"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    // ---------- RF-03 ----------

    @Test
    void solicitarRecuperacion_siempreDevuelve202_existaOnoElCorreo() throws Exception {
        String body = objectMapper.writeValueAsString(new SolicitarRecuperacionRequest("cualquiera@example.com"));

        mockMvc.perform(post("/api/auth/recuperacion/solicitar")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isAccepted());
    }

    @Test
    void confirmarRecuperacion_codigoInvalido_devuelve400() throws Exception {
        org.mockito.Mockito.doThrow(new BadRequestException("Código inválido o expirado."))
                .when(authService).confirmarRecuperacion(any(ConfirmarRecuperacionRequest.class));

        String body = objectMapper.writeValueAsString(
                new ConfirmarRecuperacionRequest("ana@example.com", "000000", "nuevaClave123"));

        mockMvc.perform(post("/api/auth/recuperacion/confirmar")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmarRecuperacion_codigoValido_devuelve204() throws Exception {
        String body = objectMapper.writeValueAsString(
                new ConfirmarRecuperacionRequest("ana@example.com", "123456", "nuevaClave123"));

        mockMvc.perform(post("/api/auth/recuperacion/confirmar")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNoContent());
    }

    // ---------- RF-04 / RF-05 / RF-06: requieren @AuthenticationPrincipal ----------
    // Se autentica poniendo directamente el Authentication en SecurityContextHolder
    // (ver autenticarComo): con addFilters=false ningún filtro está disponible
    // para leer la sesión y poblar el contexto por nosotros.

    @Test
    void obtenerPerfil_usuarioAutenticado_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        Usuario principal = usuarioDePrueba(id);
        autenticarComo(principal);
        PerfilResponse respuesta = new PerfilResponse(id, "Ana", "ana@example.com", RolUsuario.HUESPED, true, null);
        when(authService.obtenerPerfil(id)).thenReturn(respuesta);

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
        when(authService.actualizarPerfil(any(), any(ActualizarPerfilRequest.class))).thenReturn(respuesta);

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
        when(authService.cambiarEstadoCuenta(any(), any(CambiarEstadoCuentaRequest.class))).thenReturn(respuesta);

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
}