package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.DashboardResponse;
import co.edu.unbosque.casitago.dto.DemandaCiudadResponse;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import co.edu.unbosque.casitago.service.ReporteService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReporteController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReporteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteService reporteService;

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

    private Usuario usuarioDePrueba(RolUsuario rol) {
        Usuario usuario = new Usuario("Usuario Prueba", "prueba@example.com", "hash", rol);
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        return usuario;
    }

    private DashboardResponse dashboardDePrueba() {
        Map<String, Long> reservasPorEstado = new LinkedHashMap<>();
        reservasPorEstado.put("CONFIRMADA", 2L);

        DashboardResponse dashboard = new DashboardResponse();
        dashboard.setTotalUsuarios(10L);
        dashboard.setPublicacionesActivas(4L);
        dashboard.setReservasPorEstado(reservasPorEstado);
        dashboard.setIngresosEstimados(new BigDecimal("630050.00"));
        dashboard.setCiudadesConMayorDemanda(List.of(new DemandaCiudadResponse("Bogota", 3L)));
        return dashboard;
    }

    // ---------- RF-27: dashboard ----------

    @Test
    void obtenerDashboard_administrador_devuelve200() throws Exception {
        autenticarComo(usuarioDePrueba(RolUsuario.ADMINISTRADOR));

        when(reporteService.obtenerDashboard(any())).thenReturn(dashboardDePrueba());

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsuarios").value(10))
                .andExpect(jsonPath("$.publicacionesActivas").value(4))
                .andExpect(jsonPath("$.reservasPorEstado.CONFIRMADA").value(2))
                .andExpect(jsonPath("$.ciudadesConMayorDemanda[0].ciudad").value("Bogota"));
    }

    @Test
    void obtenerDashboard_sinPermiso_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(RolUsuario.HUESPED));

        when(reporteService.obtenerDashboard(any()))
                .thenThrow(new RuntimeException("Solo un ADMINISTRADOR puede realizar esta acción"));

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Solo un ADMINISTRADOR puede realizar esta acción"));
    }

    // ---------- RF-28: reporte PDF ----------

    @Test
    void descargarReporteGestion_administrador_devuelvePdf() throws Exception {
        autenticarComo(usuarioDePrueba(RolUsuario.ADMINISTRADOR));

        byte[] pdf = "%PDF-1.4 contenido de prueba".getBytes();
        when(reporteService.generarReportePdf(any())).thenReturn(pdf);

        mockMvc.perform(get("/api/admin/reportes/gestion"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=reporte-gestion.pdf"))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void descargarReporteGestion_sinPermiso_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(RolUsuario.ANFITRION));

        when(reporteService.generarReportePdf(any()))
                .thenThrow(new RuntimeException("Solo un ADMINISTRADOR puede realizar esta acción"));

        mockMvc.perform(get("/api/admin/reportes/gestion"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Solo un ADMINISTRADOR puede realizar esta acción"));
    }
}