package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.NotificacionResponse;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import co.edu.unbosque.casitago.service.NotificacionService;
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

@WebMvcTest(controllers = NotificacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificacionService notificacionService;

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

    private NotificacionResponse notificacionDePrueba(boolean leida) {
        NotificacionResponse response = new NotificacionResponse();
        response.setId(UUID.randomUUID());
        response.setTipoEvento("RESERVA_CONFIRMADA");
        response.setMensaje("Tu reserva fue confirmada");
        response.setLeida(leida);
        return response;
    }

    // ---------- RF-21: listar mis notificaciones ----------

    @Test
    void listarMisNotificaciones_devuelveLista() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        when(notificacionService.listarMisNotificaciones(any()))
                .thenReturn(List.of(notificacionDePrueba(false)));

        mockMvc.perform(get("/api/notificaciones/mias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipoEvento").value("RESERVA_CONFIRMADA"))
                .andExpect(jsonPath("$[0].leida").value(false));
    }

    // ---------- RF-21: marcar como leída ----------

    @Test
    void marcarComoLeida_notificacionPropia_devuelve200() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        when(notificacionService.marcarComoLeida(any(), any())).thenReturn(notificacionDePrueba(true));

        mockMvc.perform(patch("/api/notificaciones/" + UUID.randomUUID() + "/leida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leida").value(true));
    }

    @Test
    void marcarComoLeida_sinPermiso_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        when(notificacionService.marcarComoLeida(any(), any()))
                .thenThrow(new RuntimeException("No tienes permiso para modificar esta notificación"));

        mockMvc.perform(patch("/api/notificaciones/" + UUID.randomUUID() + "/leida"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No tienes permiso para modificar esta notificación"));
    }
}