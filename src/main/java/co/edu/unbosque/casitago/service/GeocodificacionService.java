package co.edu.unbosque.casitago.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class GeocodificacionService {

    private static final String URL_BASE = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=";

    private final RestTemplate restTemplate;

    public GeocodificacionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Devuelve [latitud, longitud] o null si no se encontró la ubicación
    public double[] geocodificar(String ubicacionTextual) {
        String consulta = URLEncoder.encode(ubicacionTextual, StandardCharsets.UTF_8);
        String url = URL_BASE + consulta;

        List<Map<String, Object>> resultados;
        try {
            resultados = restTemplate.getForObject(url, List.class);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo consultar el servicio de geocodificación.");
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