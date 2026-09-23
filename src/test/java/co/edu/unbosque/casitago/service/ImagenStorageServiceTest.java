package co.edu.unbosque.casitago.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImagenStorageServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private ImagenStorageService imagenStorageService;

    @BeforeEach
    void setUp() {
        imagenStorageService = new ImagenStorageService(restTemplate);
        ReflectionTestUtils.setField(imagenStorageService, "supabaseUrl", "https://ejemplo.supabase.co");
        ReflectionTestUtils.setField(imagenStorageService, "supabaseServiceKey", "clave-de-prueba");
        ReflectionTestUtils.setField(imagenStorageService, "bucket", "publicaciones-imagenes");
    }

    @Test
    void subirImagen_archivoValido_devuelveUrlPublica() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "foto.jpg", "image/jpeg", "contenido".getBytes());

        String url = imagenStorageService.subirImagen(UUID.randomUUID(), archivo);

        assertTrue(url.startsWith("https://ejemplo.supabase.co/storage/v1/object/public/publicaciones-imagenes/"));
        assertTrue(url.endsWith(".jpg"));
    }

    @Test
    void subirImagen_conNombreMalicioso_ignoraElNombreOriginal() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "../../etc/passwd.jpg", "image/jpeg", "contenido".getBytes());

        String url = imagenStorageService.subirImagen(UUID.randomUUID(), archivo);

        assertFalse(url.contains(".."));
        assertFalse(url.contains("passwd"));
    }

    @Test
    void subirImagen_archivoVacio_lanzaExcepcion() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[0]);

        assertThrows(RuntimeException.class, () -> imagenStorageService.subirImagen(UUID.randomUUID(), archivo));
    }

    @Test
    void subirImagen_tipoNoPermitido_lanzaExcepcion() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "documento.pdf", "application/pdf", "contenido".getBytes());

        assertThrows(RuntimeException.class, () -> imagenStorageService.subirImagen(UUID.randomUUID(), archivo));
    }

    @Test
    void subirImagen_fallaLaSubida_lanzaExcepcion() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenThrow(new RuntimeException("caída de red"));

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "foto.jpg", "image/jpeg", "contenido".getBytes());

        assertThrows(RuntimeException.class, () -> imagenStorageService.subirImagen(UUID.randomUUID(), archivo));
    }

    private static <T> T eq(T valor) {
        return org.mockito.ArgumentMatchers.eq(valor);
    }
}