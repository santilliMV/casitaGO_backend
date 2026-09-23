package co.edu.unbosque.casitago.entity;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ServicioAlojamientoTest {

    @Test
    void gettersYSetters_funcionanCorrectamente() {
        ServicioAlojamiento servicio = new ServicioAlojamiento();
        UUID id = UUID.randomUUID();
        Publicacion publicacion = new Publicacion();

        servicio.setId(id);
        servicio.setPublicacion(publicacion);
        servicio.setNombre("WiFi");

        assertEquals(id, servicio.getId());
        assertEquals(publicacion, servicio.getPublicacion());
        assertEquals("WiFi", servicio.getNombre());
    }
}