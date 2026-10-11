package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.config.JwtService;
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

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminExportarControllerTest {

    private static final String RUTA = "/api/admin/auditoria/exportar";
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

    private void autenticarComo(RolUsuario rol) {
        Usuario principal = new Usuario("Usuario Prueba", "prueba@example.com", "hash", rol);
        ReflectionTestUtils.setField(principal, "id", UUID.randomUUID());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void exportarAuditoria_administrador_devuelveElArchivoCsv() throws Exception {
        autenticarComo(RolUsuario.ADMINISTRADOR);

        when(adminService.exportarAuditoriaCsv(any(), any(), any(), any()))
                .thenReturn("a;b".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=auditoria.csv"))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string("a;b"));
    }

    @Test
    void exportarAuditoria_conFiltros_losPasaAlServicio() throws Exception {
        autenticarComo(RolUsuario.ADMINISTRADOR);
        UUID usuarioId = UUID.randomUUID();

        when(adminService.exportarAuditoriaCsv(any(), any(), any(), any()))
                .thenReturn("a;b".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get(RUTA)
                        .param("entidad", "usuarios")
                        .param("accion", "LOGIN")
                        .param("usuarioId", usuarioId.toString()))
                .andExpect(status().isOk());

        verify(adminService).exportarAuditoriaCsv(any(), eq("usuarios"), eq("LOGIN"), eq(usuarioId));
    }

    @Test
    void exportarAuditoria_noEsAdministrador_devuelve400() throws Exception {
        autenticarComo(RolUsuario.HUESPED);

        when(adminService.exportarAuditoriaCsv(any(), any(), any(), any()))
                .thenThrow(new RuntimeException(MENSAJE_NO_ADMIN));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(MENSAJE_NO_ADMIN));
    }
}