package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.BloqueoRequest;
import co.edu.unbosque.casitago.dto.DisponibilidadResponse;
import co.edu.unbosque.casitago.dto.PeriodoResponse;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import co.edu.unbosque.casitago.service.DisponibilidadService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DisponibilidadController.class)
@AutoConfigureMockMvc(addFilters = false)
class DisponibilidadControllerTest {

    private static final String BODY_BLOQUEO = "{\"fechaInicio\":\"2026-12-01\",\"fechaFin\":\"2026-12-05\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DisponibilidadService disponibilidadService;

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

    private PeriodoResponse periodoDePrueba() {
        PeriodoResponse response = new PeriodoResponse();
        response.setId(UUID.randomUUID());
        response.setFechaInicio(LocalDate.of(2026, 12, 1));
        response.setFechaFin(LocalDate.of(2026, 12, 5));
        response.setEstado("BLOQUEADO");
        return response;
    }

    // ---------- RF-13: crear bloqueo ----------

    @Test
    void crearBloqueo_datosValidos_devuelve200() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.ANFITRION));

        when(disponibilidadService.crearBloqueo(any(), any(), any(BloqueoRequest.class)))
                .thenReturn(periodoDePrueba());

        mockMvc.perform(post("/api/publicaciones/" + UUID.randomUUID() + "/bloqueos")
                        .contentType("application/json")
                        .content(BODY_BLOQUEO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADO"));
    }

    @Test
    void crearBloqueo_conRangoSolapado_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.ANFITRION));

        when(disponibilidadService.crearBloqueo(any(), any(), any(BloqueoRequest.class)))
                .thenThrow(new RuntimeException("El rango se cruza con otro bloqueo o con una reserva existente."));

        mockMvc.perform(post("/api/publicaciones/" + UUID.randomUUID() + "/bloqueos")
                        .contentType("application/json")
                        .content(BODY_BLOQUEO))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El rango se cruza con otro bloqueo o con una reserva existente."));
    }

    // ---------- RF-13: listar periodos ----------

    @Test
    void listarPeriodos_propietario_devuelveLista() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.ANFITRION));

        when(disponibilidadService.listarPeriodos(any(), any()))
                .thenReturn(List.of(periodoDePrueba()));

        mockMvc.perform(get("/api/publicaciones/" + UUID.randomUUID() + "/periodos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listarPeriodos_sinPermiso_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.ANFITRION));

        when(disponibilidadService.listarPeriodos(any(), any()))
                .thenThrow(new RuntimeException("Solo el ANFITRIÓN propietario puede realizar esta acción."));

        mockMvc.perform(get("/api/publicaciones/" + UUID.randomUUID() + "/periodos"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Solo el ANFITRIÓN propietario puede realizar esta acción."));
    }

    // ---------- RF-13: eliminar bloqueo ----------

    @Test
    void eliminarBloqueo_propietario_devuelve200() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.ANFITRION));

        mockMvc.perform(delete("/api/publicaciones/" + UUID.randomUUID() + "/bloqueos/" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(content().string("Bloqueo eliminado correctamente."));
    }

    @Test
    void eliminarBloqueo_periodoReservado_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.ANFITRION));

        doThrow(new RuntimeException("Solo se pueden eliminar periodos en estado BLOQUEADO."))
                .when(disponibilidadService).eliminarBloqueo(any(), any(), any());

        mockMvc.perform(delete("/api/publicaciones/" + UUID.randomUUID() + "/bloqueos/" + UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Solo se pueden eliminar periodos en estado BLOQUEADO."));
    }

    // ---------- RF-14: consultar disponibilidad ----------

    @Test
    void consultarDisponibilidad_rangoValido_devuelve200() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        DisponibilidadResponse respuesta = new DisponibilidadResponse();
        respuesta.setDisponible(true);
        when(disponibilidadService.consultarDisponibilidad(any(), any(), any())).thenReturn(respuesta);

        mockMvc.perform(get("/api/publicaciones/" + UUID.randomUUID() + "/disponibilidad")
                        .param("desde", "2026-12-01")
                        .param("hasta", "2026-12-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true));
    }

    @Test
    void consultarDisponibilidad_rangoInvalido_devuelve400() throws Exception {
        autenticarComo(usuarioDePrueba(UUID.randomUUID(), RolUsuario.HUESPED));

        when(disponibilidadService.consultarDisponibilidad(any(), any(), any()))
                .thenThrow(new RuntimeException("La fecha de fin debe ser posterior a la fecha de inicio."));

        mockMvc.perform(get("/api/publicaciones/" + UUID.randomUUID() + "/disponibilidad")
                        .param("desde", "2026-12-05")
                        .param("hasta", "2026-12-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("La fecha de fin debe ser posterior a la fecha de inicio."));
    }
}