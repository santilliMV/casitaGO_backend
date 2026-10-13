package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.PublicacionAdminResponse;
import co.edu.unbosque.casitago.dto.UsuarioAdminResponse;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.TipoAlojamiento;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminListadosControllerTest {

    private static final String RUTA_USUARIOS = "/api/admin/usuarios";
    private static final String RUTA_PUBLICACIONES = "/api/admin/publicaciones";
    private static final String MENSAJE_NO_ADMIN = "Solo un ADMINISTRADOR puede realizar esta acción";

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

    private Usuario autenticarComo(RolUsuario rol) {
        Usuario principal = new Usuario("Usuario Prueba", "prueba@example.com", "hash", rol);
        ReflectionTestUtils.setField(principal, "id", UUID.randomUUID());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        return principal;
    }

    @Test
    void listarUsuarios_administrador_devuelveLaLista() throws Exception {
        Usuario admin = autenticarComo(RolUsuario.ADMINISTRADOR);
        UsuarioAdminResponse fila = UsuarioAdminResponse.desde(admin, true);

        when(adminService.listarUsuarios(any())).thenReturn(List.of(fila));

        mockMvc.perform(get(RUTA_USUARIOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correo").value("prueba@example.com"))
                .andExpect(jsonPath("$[0].rol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$[0].restringido").value(true));
    }

    @Test
    void listarUsuarios_noEsAdministrador_devuelve400() throws Exception {
        autenticarComo(RolUsuario.HUESPED);

        when(adminService.listarUsuarios(any()))
                .thenThrow(new IllegalStateException(MENSAJE_NO_ADMIN));

        mockMvc.perform(get(RUTA_USUARIOS))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(MENSAJE_NO_ADMIN));
    }

    @Test
    void listarPublicaciones_administrador_devuelveLaLista() throws Exception {
        Usuario admin = autenticarComo(RolUsuario.ADMINISTRADOR);
        Publicacion publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
        publicacion.setAnfitrion(admin);
        publicacion.setTitulo("Apto en Chapinero");
        publicacion.setCiudad("Bogotá");
        publicacion.setTipo(TipoAlojamiento.APARTAMENTO);
        publicacion.setPrecioNoche(new BigDecimal("150000"));
        PublicacionAdminResponse fila = PublicacionAdminResponse.desde(publicacion);

        when(adminService.listarPublicaciones(any())).thenReturn(List.of(fila));

        mockMvc.perform(get(RUTA_PUBLICACIONES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Apto en Chapinero"))
                .andExpect(jsonPath("$[0].estado").value("BORRADOR"))
                .andExpect(jsonPath("$[0].anfitrionCorreo").value("prueba@example.com"));
    }

    @Test
    void listarPublicaciones_noEsAdministrador_devuelve400() throws Exception {
        autenticarComo(RolUsuario.ANFITRION);

        when(adminService.listarPublicaciones(any()))
                .thenThrow(new IllegalStateException(MENSAJE_NO_ADMIN));

        mockMvc.perform(get(RUTA_PUBLICACIONES))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(MENSAJE_NO_ADMIN));
    }
}