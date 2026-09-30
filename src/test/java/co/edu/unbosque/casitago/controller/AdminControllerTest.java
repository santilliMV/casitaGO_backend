package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.EventoAuditoriaResponse;
import co.edu.unbosque.casitago.dto.RestriccionResponse;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import co.edu.unbosque.casitago.service.AdminService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    private static final String BODY_RESTRICCION = "{\"motivo\":\"Incumple las políticas\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

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

    private Usuario usuarioDePrueba(UUID id, RolUsuario rol) {
        Usuario usuario = new Usuario("Usuario Prueba", "prueba@example.com", "hash", rol);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    private RestriccionResponse restriccionDePrueba(boolean activa) {
        RestriccionResponse response = new RestriccionResponse();
        response.setId(UUID.randomUUID());
        response.setUsuarioId(UUID.randomUUID());
        response.setMotivo("Incumple las políticas");
        response.setActiva(activa);
        return response;
    }

    // ---------- RF-25: restringir ----------

    @Test
    void restringirUsuario_datosValidos_devuelve200() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.ADMINISTRADOR));

        when(adminService.restringirUsuario(any(), any(), any())).thenReturn(restriccionDePrueba(true));

        mockMvc.perform(post("/api/admin/usuarios/" + UUID.randomUUID() + "/restringir")
                        .contentType("application/json")
                        .content(BODY_RESTRICCION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(true))
                .andExpect(jsonPath("$.motivo").value("Incumple las políticas"));
    }

    @Test
    void restringirUsuario_sinMotivo_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.ADMINISTRADOR));

        mockMvc.perform(post("/api/admin/usuarios/" + UUID.randomUUID() + "/restringir")
                        .contentType("application/json")
                        .content("{\"motivo\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void restringirUsuario_sinPermiso_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        when(adminService.restringirUsuario(any(), any(), any()))
                .thenThrow(new RuntimeException("Solo un ADMINISTRADOR puede realizar esta acción"));

        mockMvc.perform(post("/api/admin/usuarios/" + UUID.randomUUID() + "/restringir")
                        .contentType("application/json")
                        .content(BODY_RESTRICCION))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Solo un ADMINISTRADOR puede realizar esta acción"));
    }

    // ---------- RF-25: levantar restricción ----------

    @Test
    void levantarRestriccion_devuelve200() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.ADMINISTRADOR));

        when(adminService.levantarRestriccion(any(), any())).thenReturn(restriccionDePrueba(false));

        mockMvc.perform(patch("/api/admin/usuarios/" + UUID.randomUUID() + "/levantar-restriccion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));
    }

    @Test
    void levantarRestriccion_sinRestriccionActiva_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.ADMINISTRADOR));

        when(adminService.levantarRestriccion(any(), any()))
                .thenThrow(new RuntimeException("El usuario no tiene una restricción activa"));

        mockMvc.perform(patch("/api/admin/usuarios/" + UUID.randomUUID() + "/levantar-restriccion"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El usuario no tiene una restricción activa"));
    }

    // ---------- RF-26: auditoría ----------

    @Test
    void consultarAuditoria_devuelveLista() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.ADMINISTRADOR));

        EventoAuditoriaResponse evento = new EventoAuditoriaResponse();
        evento.setEntidad("usuarios");
        evento.setAccion("LOGIN");
        evento.setResultado("EXITOSO");
        when(adminService.consultarAuditoria(any(), any(), any(), any())).thenReturn(List.of(evento));

        mockMvc.perform(get("/api/admin/auditoria").param("entidad", "usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accion").value("LOGIN"));
    }

    @Test
    void consultarAuditoria_sinPermiso_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        when(adminService.consultarAuditoria(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Solo un ADMINISTRADOR puede realizar esta acción"));

        mockMvc.perform(get("/api/admin/auditoria"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Solo un ADMINISTRADOR puede realizar esta acción"));
    }
}