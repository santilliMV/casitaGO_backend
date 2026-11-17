package co.edu.unbosque.casitago.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SecurityConfigCorsTest {

    private CorsConfiguration configuracionCon(List<String> origenes) {
        SecurityConfig securityConfig = new SecurityConfig(null);
        ReflectionTestUtils.setField(securityConfig, "origenesPermitidos", origenes);
        return securityConfig.corsConfigurationSource().getCorsConfiguration(new MockHttpServletRequest("GET", "/api/publicaciones/buscar"));
    }

    @Test
    void cors_aceptaElDominioConfiguradoDelFrontend() {
        CorsConfiguration config = configuracionCon(List.of("https://casitago.onrender.com"));

        assertEquals("https://casitago.onrender.com", config.checkOrigin("https://casitago.onrender.com"));
    }

    @Test
    void cors_rechazaDominiosNoConfigurados() {
        CorsConfiguration config = configuracionCon(List.of("https://casitago.onrender.com"));

        assertNull(config.checkOrigin("https://sitio-malicioso.com"));
    }

    @Test
    void cors_admiteVariosDominiosYComodines() {
        CorsConfiguration config = configuracionCon(List.of("http://localhost:4200", " https://*.onrender.com "));

        assertNotNull(config.checkOrigin("http://localhost:4200"));
        assertNotNull(config.checkOrigin("https://casitago-front.onrender.com"));
        assertNull(config.checkOrigin("https://casitago.vercel.app"));
    }
}
