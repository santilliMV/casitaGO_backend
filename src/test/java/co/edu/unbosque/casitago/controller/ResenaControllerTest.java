package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.ResenaResponse;
import co.edu.unbosque.casitago.dto.ResenasPublicacionResponse;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import co.edu.unbosque.casitago.service.ResenaService;
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

@WebMvcTest(controllers = ResenaController.class)
@AutoConfigureMockMvc(addFilters = false)
class ResenaControllerTest {

    private static final String BODY_RESENA = "{\"calificacion\":5,\"comentario\":\"Excelente estadía\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResenaService resenaService;

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

    private ResenaResponse resenaDePrueba() {
        ResenaResponse response = new ResenaResponse();
        response.setId(UUID.randomUUID());
        response.setCalificacion(5);
        response.setComentario("Excelente estadía");
        response.setHuespedNombre("Usuario Prueba");
        return response;
    }

    // ---------- RF-22: crear reseña ----------

    @Test
    void crearResena_datosValidos_devuelve200() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        when(resenaService.crearResena(any(), any(), any())).thenReturn(resenaDePrueba());

        mockMvc.perform(post("/api/reservas/" + UUID.randomUUID() + "/resena")
                        .contentType("application/json")
                        .content(BODY_RESENA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calificacion").value(5))
                .andExpect(jsonPath("$.comentario").value("Excelente estadía"));
    }

    @Test
    void crearResena_reglaDeNegocioIncumplida_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        when(resenaService.crearResena(any(), any(), any()))
                .thenThrow(new RuntimeException("Ya reseñaste esta reserva"));

        mockMvc.perform(post("/api/reservas/" + UUID.randomUUID() + "/resena")
                        .contentType("application/json")
                        .content(BODY_RESENA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Ya reseñaste esta reserva"));
    }

    @Test
    void crearResena_calificacionFueraDeRango_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        mockMvc.perform(post("/api/reservas/" + UUID.randomUUID() + "/resena")
                        .contentType("application/json")
                        .content("{\"calificacion\":6,\"comentario\":\"Muy bueno\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- RF-23: consultar reseñas de una publicación ----------

    @Test
    void listarResenas_devuelvePromedioYLista() throws Exception {
        ResenasPublicacionResponse respuesta = new ResenasPublicacionResponse();
        respuesta.setPromedio(4.5);
        respuesta.setTotal(1);
        respuesta.setResenas(List.of(resenaDePrueba()));

        when(resenaService.listarPorPublicacion(any())).thenReturn(respuesta);

        mockMvc.perform(get("/api/publicaciones/" + UUID.randomUUID() + "/resenas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promedio").value(4.5))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.resenas.length()").value(1));
    }

    @Test
    void listarResenas_publicacionInexistente_devuelve400() throws Exception {
        when(resenaService.listarPorPublicacion(any()))
                .thenThrow(new RuntimeException("La publicación no existe"));

        mockMvc.perform(get("/api/publicaciones/" + UUID.randomUUID() + "/resenas"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("La publicación no existe"));
    }
}