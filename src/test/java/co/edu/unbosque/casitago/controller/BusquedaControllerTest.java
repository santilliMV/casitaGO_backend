package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.PublicacionResponse;
import co.edu.unbosque.casitago.dto.UbicacionResponse;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import co.edu.unbosque.casitago.service.BusquedaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BusquedaController.class)
@AutoConfigureMockMvc(addFilters = false)
class BusquedaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BusquedaService busquedaService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private PublicacionResponse respuestaDePrueba() {
        PublicacionResponse response = new PublicacionResponse();
        response.setId(UUID.randomUUID());
        response.setTitulo("Apartamento con vista al mar");
        response.setEstado("ACTIVA");
        return response;
    }

    // ---------- RF-15 ----------

    @Test
    void buscar_sinFiltros_devuelve200() throws Exception {
        when(busquedaService.buscar(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(respuestaDePrueba()));

        mockMvc.perform(get("/api/publicaciones/buscar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void buscar_conTipoInvalido_devuelve400() throws Exception {
        when(busquedaService.buscar(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("El tipo de alojamiento indicado no es válido."));

        mockMvc.perform(get("/api/publicaciones/buscar").param("tipo", "CASTILLO"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El tipo de alojamiento indicado no es válido."));
    }

    @Test
    void buscar_conServiciosRepetidos_deberiaLlegarComoLista() throws Exception {
        when(busquedaService.buscar(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/publicaciones/buscar")
                        .param("servicios", "WiFi")
                        .param("servicios", "Piscina"))
                .andExpect(status().isOk());
    }

    // ---------- RF-16 ----------

    @Test
    void consultarDetalle_publicacionExistente_devuelve200() throws Exception {
        UUID publicacionId = UUID.randomUUID();
        when(busquedaService.consultarDetalle(publicacionId)).thenReturn(respuestaDePrueba());

        mockMvc.perform(get("/api/publicaciones/" + publicacionId + "/detalle"))
                .andExpect(status().isOk());
    }

    @Test
    void consultarDetalle_publicacionInexistente_devuelve400() throws Exception {
        UUID publicacionId = UUID.randomUUID();
        when(busquedaService.consultarDetalle(publicacionId))
                .thenThrow(new RuntimeException("Publicación no encontrada."));

        mockMvc.perform(get("/api/publicaciones/" + publicacionId + "/detalle"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Publicación no encontrada."));
    }

    // ---------- RF-30 ----------

    @Test
    void obtenerUbicacion_publicacionExistente_devuelve200() throws Exception {
        UUID publicacionId = UUID.randomUUID();
        UbicacionResponse ubicacion = new UbicacionResponse();
        ubicacion.setPublicacionId(publicacionId);
        ubicacion.setLatitud(10.4);
        ubicacion.setLongitud(-75.5);
        when(busquedaService.obtenerUbicacion(publicacionId)).thenReturn(ubicacion);

        mockMvc.perform(get("/api/publicaciones/" + publicacionId + "/ubicacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitud").value(10.4));
    }

    @Test
    void obtenerUbicacion_noSePudoGeocodificar_devuelve400() throws Exception {
        UUID publicacionId = UUID.randomUUID();
        when(busquedaService.obtenerUbicacion(publicacionId))
                .thenThrow(new RuntimeException("No se pudo ubicar esta publicación en el mapa."));

        mockMvc.perform(get("/api/publicaciones/" + publicacionId + "/ubicacion"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se pudo ubicar esta publicación en el mapa."));
    }
}