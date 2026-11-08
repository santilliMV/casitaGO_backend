package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.PublicacionResponse;
import co.edu.unbosque.casitago.entity.EstadoPublicacion;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.TipoAlojamiento;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import co.edu.unbosque.casitago.service.PublicacionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicacionDesbloqueoControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    private Usuario autenticarComo(RolUsuario rol) {
        Usuario usuario = new Usuario("Usuario Prueba", "prueba@example.com", "hash", rol);
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
        return usuario;
    }

    private PublicacionResponse respuestaPausada() {
        Usuario anfitrion = new Usuario("Ana Anfitriona", "ana@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(anfitrion, "id", UUID.randomUUID());

        Publicacion publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
        publicacion.setAnfitrion(anfitrion);
        publicacion.setTipo(TipoAlojamiento.APARTAMENTO);
        publicacion.setEstado(EstadoPublicacion.PAUSADA);
        return PublicacionResponse.desde(publicacion);
    }

    @Test
    void desbloquear_administrador_devuelve200ConPublicacionPausada() throws Exception {
        autenticarComo(RolUsuario.ADMINISTRADOR);
        when(publicacionService.desbloquearPublicacion(any(), any())).thenReturn(respuestaPausada());

        mockMvc.perform(patch("/api/publicaciones/" + UUID.randomUUID() + "/desbloquear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAUSADA"));
    }

    @Test
    void desbloquear_cuandoElServiceRechaza_devuelve400ConElMensaje() throws Exception {
        autenticarComo(RolUsuario.ANFITRION);
        when(publicacionService.desbloquearPublicacion(any(), any()))
                .thenThrow(new IllegalStateException("Solo un ADMINISTRADOR puede desbloquear una publicación."));

        mockMvc.perform(patch("/api/publicaciones/" + UUID.randomUUID() + "/desbloquear"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Solo un ADMINISTRADOR puede desbloquear una publicación."));
    }
}