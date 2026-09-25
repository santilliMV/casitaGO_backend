package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.PublicacionResponse;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import co.edu.unbosque.casitago.service.FavoritoService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FavoritoController.class)
@AutoConfigureMockMvc(addFilters = false)
class FavoritoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoritoService favoritoService;

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

    private Usuario usuarioDePrueba(UUID id) {
        Usuario usuario = new Usuario("Hugo Huesped", "hugo@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    // ---------- RF-32: agregar ----------

    @Test
    void agregarFavorito_valido_devuelve200() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID()));

        mockMvc.perform(post("/api/publicaciones/" + UUID.randomUUID() + "/favoritos"))
                .andExpect(status().isOk())
                .andExpect(content().string("Publicación agregada a favoritos."));
    }

    @Test
    void agregarFavorito_yaExistente_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID()));

        doThrow(new RuntimeException("Esta publicación ya está en tus favoritos."))
                .when(favoritoService).agregarFavorito(any(), any());

        mockMvc.perform(post("/api/publicaciones/" + UUID.randomUUID() + "/favoritos"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Esta publicación ya está en tus favoritos."));
    }

    // ---------- RF-32: eliminar ----------

    @Test
    void eliminarFavorito_valido_devuelve200() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID()));

        mockMvc.perform(delete("/api/publicaciones/" + UUID.randomUUID() + "/favoritos"))
                .andExpect(status().isOk())
                .andExpect(content().string("Publicación eliminada de favoritos."));
    }

    @Test
    void eliminarFavorito_inexistente_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID()));

        doThrow(new RuntimeException("Esta publicación no está en tus favoritos."))
                .when(favoritoService).eliminarFavorito(any(), any());

        mockMvc.perform(delete("/api/publicaciones/" + UUID.randomUUID() + "/favoritos"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Esta publicación no está en tus favoritos."));
    }

    // ---------- RF-32: listar ----------

    @Test
    void listarFavoritos_devuelveLista() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID()));

        PublicacionResponse response = new PublicacionResponse();
        response.setId(UUID.randomUUID());
        response.setTitulo("Apartamento con vista al mar");

        when(favoritoService.listarFavoritos(any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/favoritos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}