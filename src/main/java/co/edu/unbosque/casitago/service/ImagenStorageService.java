package co.edu.unbosque.casitago.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class ImagenStorageService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String supabaseServiceKey;

    @Value("${supabase.bucket}")
    private String bucket;

    public String subirImagen(UUID publicacionId, MultipartFile archivo) {
        if (archivo.isEmpty()) {
            throw new RuntimeException("El archivo de imagen está vacío.");
        }
        if (archivo.getContentType() == null || !archivo.getContentType().startsWith("image/")) {
            throw new RuntimeException("El archivo debe ser una imagen.");
        }

        String nombreArchivo = UUID.randomUUID() + "-" + archivo.getOriginalFilename();
        String ruta = publicacionId + "/" + nombreArchivo;

        String urlSubida = supabaseUrl + "/storage/v1/object/" + bucket + "/" + ruta;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(supabaseServiceKey);
        headers.set("apikey", supabaseServiceKey);
        headers.setContentType(MediaType.parseMediaType(archivo.getContentType()));

        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo de imagen.");
        }

        HttpEntity<byte[]> peticion = new HttpEntity<>(contenido, headers);

        try {
            restTemplate.exchange(urlSubida, HttpMethod.POST, peticion, String.class);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo subir la imagen a Supabase Storage.");
        }

        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + ruta;
    }
}