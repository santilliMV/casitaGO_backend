package co.edu.unbosque.casitago.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class GeocodificacionService {

    private static final Logger LOG = LoggerFactory.getLogger(GeocodificacionService.class);

    private static final String URL_BASE =
            "https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=co&q=";

    // Nominatim exige que cada aplicación se identifique; con el User-Agent genérico de Java puede rechazar la petición.
    private static final String USER_AGENT = "CasitaGO/1.0 (proyecto academico, Universidad El Bosque)";

    private final RestTemplate restTemplate;

    public GeocodificacionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Devuelve [latitud, longitud] o null si no se encontró la ubicación
    public double[] geocodificar(String ubicacionTextual) {
        String consulta = URLEncoder.encode(ubicacionTextual, StandardCharsets.UTF_8);
        String url = URL_BASE + consulta;

        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.set(HttpHeaders.USER_AGENT, USER_AGENT);

        List<Map<String, Object>> resultados;
        try {
            ResponseEntity<List<Map<String, Object>>> respuesta = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(cabeceras),
                    new ParameterizedTypeReference<List<Map<String, Object>>>() { });
            resultados = respuesta.getBody();
        } catch (RestClientException e) {
            LOG.warn("Falló la consulta a Nominatim: {}", e.toString());
            throw new IllegalStateException("No se pudo consultar el servicio de geocodificación.", e);
        }

        if (resultados == null || resultados.isEmpty()) {
            return null;
        }

        Map<String, Object> primero = resultados.get(0);
        double latitud = Double.parseDouble(primero.get("lat").toString());
        double longitud = Double.parseDouble(primero.get("lon").toString());

        return new double[]{latitud, longitud};
    }
}