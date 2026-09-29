package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.ConversacionResponse;
import co.edu.unbosque.casitago.dto.MensajeResponse;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import co.edu.unbosque.casitago.service.MensajeService;
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

@WebMvcTest(controllers = MensajeController.class)
@AutoConfigureMockMvc(addFilters = false)
class MensajeControllerTest {

    private static final String BODY_MENSAJE = "{\"contenido\":\"Hola, ¿está disponible?\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MensajeService mensajeService;

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

    private MensajeResponse mensajeDePrueba() {
        MensajeResponse response = new MensajeResponse();
        response.setId(UUID.randomUUID());
        response.setContenido("Hola, ¿está disponible?");
        response.setLeido(false);
        return response;
    }

    // ---------- RF-31: enviar mensaje ----------

    @Test
    void enviarMensaje_datosValidos_devuelve200() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        when(mensajeService.enviarMensaje(any(), any(), any())).thenReturn(mensajeDePrueba());

        mockMvc.perform(post("/api/publicaciones/" + UUID.randomUUID() + "/mensajes")
                        .contentType("application/json")
                        .content(BODY_MENSAJE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").value("Hola, ¿está disponible?"));
    }

    @Test
    void enviarMensaje_sinPermiso_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.ANFITRION));

        when(mensajeService.enviarMensaje(any(), any(), any()))
                .thenThrow(new RuntimeException("Solo el ANFITRIÓN propietario puede responder en esta publicación"));

        mockMvc.perform(post("/api/publicaciones/" + UUID.randomUUID() + "/mensajes")
                        .contentType("application/json")
                        .content(BODY_MENSAJE))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Solo el ANFITRIÓN propietario puede responder en esta publicación"));
    }

    // ---------- RF-31: listar conversación ----------

    @Test
    void listarConversacion_permitido_devuelveLista() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        when(mensajeService.listarConversacion(any(), any(), any())).thenReturn(List.of(mensajeDePrueba()));

        mockMvc.perform(get("/api/publicaciones/" + UUID.randomUUID() + "/mensajes/" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listarConversacion_sinPermiso_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        when(mensajeService.listarConversacion(any(), any(), any()))
                .thenThrow(new RuntimeException("No tienes permiso para ver esta conversación"));

        mockMvc.perform(get("/api/publicaciones/" + UUID.randomUUID() + "/mensajes/" + UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No tienes permiso para ver esta conversación"));
    }

    // ---------- RF-31: listar mis conversaciones ----------

    @Test
    void listarMisConversaciones_devuelveLista() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        ConversacionResponse conversacion = new ConversacionResponse();
        conversacion.setUltimoMensaje("Hola");
        when(mensajeService.listarMisConversaciones(any())).thenReturn(List.of(conversacion));

        mockMvc.perform(get("/api/mensajes/mias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}