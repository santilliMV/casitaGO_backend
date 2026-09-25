package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.BloquearPublicacionRequest;
import co.edu.unbosque.casitago.dto.PublicacionRequest;
import co.edu.unbosque.casitago.dto.PublicacionResponse;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import co.edu.unbosque.casitago.service.PublicacionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PublicacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PublicacionService publicacionService;

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

    private PublicacionRequest requestValido() {
        PublicacionRequest request = new PublicacionRequest();
        request.setTitulo("Apartamento con vista al mar");
        request.setDescripcion("Cómodo y luminoso");
        request.setUbicacionTextual("Cartagena, Bolívar");
        request.setTipo("APARTAMENTO");
        request.setCapacidad(4);
        request.setPrecioNoche(new BigDecimal("250000"));
        request.setServicios(List.of("WiFi"));
        request.setReglas(List.of("No fiestas"));
        return request;
    }

    private PublicacionResponse respuestaDePrueba(UUID id, String estado) {
        PublicacionResponse response = new PublicacionResponse();
        response.setId(id);
        response.setTitulo("Apartamento con vista al mar");
        response.setEstado(estado);
        return response;
    }

    // ---------- RF-08 ----------

    @Test
    void crear_datosValidos_devuelve200() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        autenticarComo(usuarioDePrueba(usuarioId, RolUsuario.ANFITRION));

        UUID publicacionId = UUID.randomUUID();
        when(publicacionService.crearPublicacion(any(), any(PublicacionRequest.class)))
                .thenReturn(respuestaDePrueba(publicacionId, "BORRADOR"));

        String body = objectMapper.writeValueAsString(requestValido());

        mockMvc.perform(post("/api/publicaciones")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    void crear_tituloVacio_devuelve400() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        autenticarComo(usuarioDePrueba(usuarioId, RolUsuario.ANFITRION));

        PublicacionRequest request = requestValido();
        request.setTitulo("");
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/publicaciones")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_comoHuesped_devuelve400() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        autenticarComo(usuarioDePrueba(usuarioId, RolUsuario.HUESPED));

        when(publicacionService.crearPublicacion(any(), any(PublicacionRequest.class)))
                .thenThrow(new RuntimeException("Solo un usuario con rol ANFITRIÓN puede crear publicaciones."));

        String body = objectMapper.writeValueAsString(requestValido());

        mockMvc.perform(post("/api/publicaciones")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Solo un usuario con rol ANFITRIÓN puede crear publicaciones."));
    }

    // ---------- RF-09 ----------

    @Test
    void editar_propietario_devuelve200() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        autenticarComo(usuarioDePrueba(usuarioId, RolUsuario.ANFITRION));

        UUID publicacionId = UUID.randomUUID();
        when(publicacionService.editarPublicacion(any(), any(), any(PublicacionRequest.class)))
                .thenReturn(respuestaDePrueba(publicacionId, "BORRADOR"));

        String body = objectMapper.writeValueAsString(requestValido());

        mockMvc.perform(put("/api/publicaciones/" + publicacionId)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void editar_sinPermiso_devuelve400() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        autenticarComo(usuarioDePrueba(usuarioId, RolUsuario.ANFITRION));

        when(publicacionService.editarPublicacion(any(), any(), any(PublicacionRequest.class)))
                .thenThrow(new RuntimeException("No tienes permiso para editar esta publicación."));

        String body = objectMapper.writeValueAsString(requestValido());

        mockMvc.perform(put("/api/publicaciones/" + UUID.randomUUID())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No tienes permiso para editar esta publicación."));
    }

    // ---------- RF-10 ----------

    @Test
    void activar_conImagenes_devuelve200() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        autenticarComo(usuarioDePrueba(usuarioId, RolUsuario.ANFITRION));

        UUID publicacionId = UUID.randomUUID();
        when(publicacionService.activarPublicacion(any(), any()))
                .thenReturn(respuestaDePrueba(publicacionId, "ACTIVA"));

        mockMvc.perform(patch("/api/publicaciones/" + publicacionId + "/activar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    @Test
    void activar_sinImagenes_devuelve400() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        autenticarComo(usuarioDePrueba(usuarioId, RolUsuario.ANFITRION));

        when(publicacionService.activarPublicacion(any(), any()))
                .thenThrow(new RuntimeException("La publicación necesita al menos una imagen antes de activarse."));

        mockMvc.perform(patch("/api/publicaciones/" + UUID.randomUUID() + "/activar"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("La publicación necesita al menos una imagen antes de activarse."));
    }

    // ---------- RF-11 ----------

    @Test
    void pausar_propietario_devuelve200() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        autenticarComo(usuarioDePrueba(usuarioId, RolUsuario.ANFITRION));

        UUID publicacionId = UUID.randomUUID();
        when(publicacionService.pausarPublicacion(any(), any()))
                .thenReturn(respuestaDePrueba(publicacionId, "PAUSADA"));

        mockMvc.perform(patch("/api/publicaciones/" + publicacionId + "/pausar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAUSADA"));
    }

    // ---------- RF-12 ----------

    @Test
    void misPublicaciones_devuelveLista() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        autenticarComo(usuarioDePrueba(usuarioId, RolUsuario.ANFITRION));

        when(publicacionService.consultarPublicacionesDeAnfitrion(any()))
                .thenReturn(List.of(respuestaDePrueba(UUID.randomUUID(), "BORRADOR")));

        mockMvc.perform(get("/api/publicaciones/mias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ---------- RF-24 ----------

    @Test
    void bloquear_administrador_devuelve200() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        autenticarComo(usuarioDePrueba(usuarioId, RolUsuario.ADMINISTRADOR));

        UUID publicacionId = UUID.randomUUID();
        when(publicacionService.bloquearPublicacion(any(), any(), any(BloquearPublicacionRequest.class)))
                .thenReturn(respuestaDePrueba(publicacionId, "BLOQUEADA"));

        BloquearPublicacionRequest request = new BloquearPublicacionRequest();
        request.setMotivo("Información engañosa");
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(patch("/api/publicaciones/" + publicacionId + "/bloquear")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADA"));
    }

    @Test
    void bloquear_comoAnfitrion_devuelve400() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        autenticarComo(usuarioDePrueba(usuarioId, RolUsuario.ANFITRION));

        when(publicacionService.bloquearPublicacion(any(), any(), any(BloquearPublicacionRequest.class)))
                .thenThrow(new RuntimeException("Solo un ADMINISTRADOR puede bloquear una publicación."));

        BloquearPublicacionRequest request = new BloquearPublicacionRequest();
        request.setMotivo("Cualquier motivo");
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(patch("/api/publicaciones/" + UUID.randomUUID() + "/bloquear")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Solo un ADMINISTRADOR puede bloquear una publicación."));
    }

    // ---------- RF-08 (imágenes) ----------

    @Test
    void agregarImagen_propietario_devuelve200() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        autenticarComo(usuarioDePrueba(usuarioId, RolUsuario.ANFITRION));

        UUID publicacionId = UUID.randomUUID();
        when(publicacionService.agregarImagen(any(), any(), any()))
                .thenReturn(respuestaDePrueba(publicacionId, "BORRADOR"));

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "foto.jpg", "image/jpeg", "contenido-falso".getBytes());

        mockMvc.perform(multipart("/api/publicaciones/" + publicacionId + "/imagenes")
                        .file(archivo))
                .andExpect(status().isOk());
    }

    @Test
    void agregarImagen_sinPermiso_devuelve400() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        autenticarComo(usuarioDePrueba(usuarioId, RolUsuario.ANFITRION));

        when(publicacionService.agregarImagen(any(), any(), any()))
                .thenThrow(new RuntimeException("Solo el ANFITRIÓN propietario puede realizar esta acción."));

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "foto.jpg", "image/jpeg", "contenido-falso".getBytes());

        mockMvc.perform(multipart("/api/publicaciones/" + UUID.randomUUID() + "/imagenes")
                        .file(archivo))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Solo el ANFITRIÓN propietario puede realizar esta acción."));
    }
}