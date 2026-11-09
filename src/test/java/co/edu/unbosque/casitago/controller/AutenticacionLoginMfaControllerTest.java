package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.config.JwtService;
import co.edu.unbosque.casitago.dto.LoginRequest;
import co.edu.unbosque.casitago.dto.LoginResponse;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import co.edu.unbosque.casitago.service.AutenticacionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AutenticacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class AutenticacionLoginMfaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AutenticacionService autenticacionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private String cuerpoDeLogin() throws Exception {
        return objectMapper.writeValueAsString(new LoginRequest("ana@example.com", "clave12345"));
    }

    @Test
    void login_conMfaActivoSinCodigo_devuelve200ConElIndicadorYSinToken() throws Exception {
        when(autenticacionService.login(any(LoginRequest.class)))
                .thenReturn(LoginResponse.codigoMfaRequerido());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(cuerpoDeLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequerido").value(true))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void login_conCodigoInvalido_sigueDevolviendo400ConElMensaje() throws Exception {
        when(autenticacionService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Código inválido o expirado."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(cuerpoDeLogin()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Código inválido o expirado."));
    }

    @Test
    void login_normal_devuelve200ConTokenYSinPedirCodigo() throws Exception {
        LoginResponse respuesta = new LoginResponse("jwt-token", 1800L, UUID.randomUUID(), "Ana", RolUsuario.HUESPED);
        when(autenticacionService.login(any(LoginRequest.class))).thenReturn(respuesta);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(cuerpoDeLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.mfaRequerido").value(false));
    }
}