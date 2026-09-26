package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.CancelacionResponse;
import co.edu.unbosque.casitago.dto.CotizacionResponse;
import co.edu.unbosque.casitago.dto.ReservaResponse;
import co.edu.unbosque.casitago.entity.EstadoReserva;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import co.edu.unbosque.casitago.service.ReservaService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReservaController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReservaControllerTest {

    private static final String BODY_COTIZACION =
            "{\"publicacionId\":\"" + UUID.randomUUID() + "\",\"fechaLlegada\":\"2026-12-01\",\"fechaSalida\":\"2026-12-04\"}";

    private static final String BODY_RESERVA =
            "{\"cotizacionId\":\"" + UUID.randomUUID() + "\",\"fechaLlegada\":\"2026-12-01\",\"fechaSalida\":\"2026-12-04\"}";

    private static final String BODY_CANCELACION = "{\"motivo\":\"Cambio de planes\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaService reservaService;

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

    private CotizacionResponse cotizacionDePrueba() {
        CotizacionResponse response = new CotizacionResponse();
        response.setId(UUID.randomUUID());
        response.setNoches(3);
        response.setPrecioBase(new BigDecimal("300000"));
        response.setTarifaLimpieza(new BigDecimal("25"));
        response.setTarifaServicio(new BigDecimal("15000"));
        response.setServiciosAdicionales(BigDecimal.ZERO);
        response.setTotal(new BigDecimal("315025"));
        return response;
    }

    private ReservaResponse reservaDePrueba() {
        ReservaResponse response = new ReservaResponse();
        response.setId(UUID.randomUUID());
        response.setEstado(EstadoReserva.CONFIRMADA);
        response.setTotal(new BigDecimal("315025"));
        return response;
    }

    // ---------- RF-17: generar cotización ----------

    @Test
    void generarCotizacion_datosValidos_devuelve200() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        when(reservaService.generarCotizacion(any())).thenReturn(cotizacionDePrueba());

        mockMvc.perform(post("/api/reservas/cotizacion")
                        .contentType("application/json")
                        .content(BODY_COTIZACION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noches").value(3));
    }

    @Test
    void generarCotizacion_fechasNoDisponibles_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        when(reservaService.generarCotizacion(any()))
                .thenThrow(new RuntimeException("Las fechas seleccionadas no están disponibles"));

        mockMvc.perform(post("/api/reservas/cotizacion")
                        .contentType("application/json")
                        .content(BODY_COTIZACION))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Las fechas seleccionadas no están disponibles"));
    }

    // ---------- RF-18: crear reserva ----------

    @Test
    void crearReserva_datosValidos_devuelve200() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        when(reservaService.crearReserva(any(), any())).thenReturn(reservaDePrueba());

        mockMvc.perform(post("/api/reservas")
                        .contentType("application/json")
                        .content(BODY_RESERVA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    void crearReserva_conConflictoDeFechas_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        when(reservaService.crearReserva(any(), any()))
                .thenThrow(new RuntimeException("Las fechas seleccionadas no están disponibles"));

        mockMvc.perform(post("/api/reservas")
                        .contentType("application/json")
                        .content(BODY_RESERVA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Las fechas seleccionadas no están disponibles"));
    }

    // ---------- RF-19: listar historial ----------

    @Test
    void listarMisReservas_devuelveLista() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        when(reservaService.listarPorHuesped(any())).thenReturn(List.of(reservaDePrueba()));

        mockMvc.perform(get("/api/reservas/mias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ---------- RF-20: cancelar reserva ----------

    @Test
    void cancelarReserva_datosValidos_devuelve200() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        CancelacionResponse cancelacion = new CancelacionResponse();
        cancelacion.setId(UUID.randomUUID());
        cancelacion.setEstadoFinal(EstadoReserva.CANCELADA);
        cancelacion.setValorDevolucion(new BigDecimal("315025"));

        when(reservaService.cancelarReserva(any(), any(), any())).thenReturn(cancelacion);

        mockMvc.perform(patch("/api/reservas/" + UUID.randomUUID() + "/cancelar")
                        .contentType("application/json")
                        .content(BODY_CANCELACION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoFinal").value("CANCELADA"));
    }

    @Test
    void cancelarReserva_sinPermiso_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        when(reservaService.cancelarReserva(any(), any(), any()))
                .thenThrow(new RuntimeException("No tienes permiso para cancelar esta reserva"));

        mockMvc.perform(patch("/api/reservas/" + UUID.randomUUID() + "/cancelar")
                        .contentType("application/json")
                        .content(BODY_CANCELACION))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No tienes permiso para cancelar esta reserva"));
    }
}