package co.edu.unbosque.casitago.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GeocodificacionServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GeocodificacionService geocodificacionService;

    @Test
    void geocodificar_conResultado_deberiaRetornarLatitudYLongitud() {
        doReturn(ResponseEntity.ok(List.of(Map.of("lat", "4.65", "lon", "-74.06"))))
                .when(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                        any(ParameterizedTypeReference.class));

        double[] resultado = geocodificacionService.geocodificar("Bogotá");

        assertEquals(4.65, resultado[0]);
        assertEquals(-74.06, resultado[1]);
    }

    @Test
    void geocodificar_sinResultados_deberiaRetornarNull() {
        doReturn(ResponseEntity.ok(List.of()))
                .when(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                        any(ParameterizedTypeReference.class));

        assertNull(geocodificacionService.geocodificar("Calle inexistente"));
    }

    @Test
    void geocodificar_cuandoFallaLaConsulta_deberiaLanzarExcepcionConMensajeClaro() {
        doThrow(new RestClientException("sin conexion"))
                .when(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                        any(ParameterizedTypeReference.class));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> geocodificacionService.geocodificar("Bogotá"));

        assertEquals("No se pudo consultar el servicio de geocodificación.", ex.getMessage());
    }

    @Test
    void geocodificar_deberiaIdentificarseConUserAgentYRestringirseAColombia() {
        doReturn(ResponseEntity.ok(List.of()))
                .when(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                        any(ParameterizedTypeReference.class));

        geocodificacionService.geocodificar("Bogotá");

        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity> entidad = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(url.capture(), eq(HttpMethod.GET), entidad.capture(),
                any(ParameterizedTypeReference.class));

        assertTrue(url.getValue().contains("countrycodes=co"));
        assertTrue(url.getValue().contains("q=Bogot%C3%A1"));
        String agente = entidad.getValue().getHeaders().getFirst(HttpHeaders.USER_AGENT);
        assertNotNull(agente);
        assertTrue(agente.startsWith("CasitaGO"));
    }
}