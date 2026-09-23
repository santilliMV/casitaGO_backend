package co.edu.unbosque.casitago.entity;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ImagenPublicacionTest {

    @Test
    void gettersYSetters_funcionanCorrectamente() {
        ImagenPublicacion imagen = new ImagenPublicacion();
        UUID id = UUID.randomUUID();
        Publicacion publicacion = new Publicacion();

        imagen.setId(id);
        imagen.setPublicacion(publicacion);
        imagen.setUrl("https://ejemplo.com/foto.jpg");
        imagen.setOrden(2);

        assertEquals(id, imagen.getId());
        assertEquals(publicacion, imagen.getPublicacion());
        assertEquals("https://ejemplo.com/foto.jpg", imagen.getUrl());
        assertEquals(2, imagen.getOrden());
    }
}