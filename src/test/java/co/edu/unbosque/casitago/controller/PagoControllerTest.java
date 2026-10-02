package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.TransaccionPagoResponse;
import co.edu.unbosque.casitago.entity.EstadoPago;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import co.edu.unbosque.casitago.service.PagoService;
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

@WebMvcTest(controllers = PagoController.class)
@AutoConfigureMockMvc(addFilters = false)
class PagoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PagoService pagoService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComoHuesped() {
        Usuario usuario = new Usuario("Hugo Huesped", "hugo@example.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }

    private TransaccionPagoResponse pagoDePrueba() {
        TransaccionPagoResponse response = new TransaccionPagoResponse();
        response.setId(UUID.randomUUID());
        response.setEstado(EstadoPago.EXITOSO);
        response.setMonto(new BigDecimal("315025.00"));
        return response;
    }

    @Test
    void pagar_datosValidos_devuelve200() throws Exception {
        autenticarComoHuesped();
        when(pagoService.pagar(any(), any(), any())).thenReturn(pagoDePrueba());

        mockMvc.perform(post("/api/reservas/" + UUID.randomUUID() + "/pagar")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EXITOSO"));
    }

    @Test
    void pagar_siFallaElCobro_devuelve400() throws Exception {
        autenticarComoHuesped();
        when(pagoService.pagar(any(), any(), any()))
                .thenThrow(new RuntimeException("El proveedor de pagos rechazó el cobro"));

        mockMvc.perform(post("/api/reservas/" + UUID.randomUUID() + "/pagar")
                        .contentType("application/json")
                        .content("{\"metodoPago\":\"pm_card_chargeDeclined\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El proveedor de pagos rechazó el cobro"));
    }

    @Test
    void listarPagos_devuelveLista() throws Exception {
        autenticarComoHuesped();
        when(pagoService.listarPorReserva(any(), any())).thenReturn(List.of(pagoDePrueba()));

        mockMvc.perform(get("/api/reservas/" + UUID.randomUUID() + "/pagos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listarPagos_deOtroHuesped_devuelve400() throws Exception {
        autenticarComoHuesped();
        when(pagoService.listarPorReserva(any(), any()))
                .thenThrow(new RuntimeException("No tienes permiso sobre esta reserva"));

        mockMvc.perform(get("/api/reservas/" + UUID.randomUUID() + "/pagos"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No tienes permiso sobre esta reserva"));
    }
}