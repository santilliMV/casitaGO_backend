package co.edu.unbosque.casitago.entity;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReglaAlojamientoTest {

    @Test
    void gettersYSetters_funcionanCorrectamente() {
        ReglaAlojamiento regla = new ReglaAlojamiento();
        UUID id = UUID.randomUUID();
        Publicacion publicacion = new Publicacion();

        regla.setId(id);
        regla.setPublicacion(publicacion);
        regla.setDescripcion("No fiestas");

        assertEquals(id, regla.getId());
        assertEquals(publicacion, regla.getPublicacion());
        assertEquals("No fiestas", regla.getDescripcion());
    }
}