package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.EstadoPublicacion;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.TipoAlojamiento;
import co.edu.unbosque.casitago.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublicacionResponseTest {

    @Test
    void desde_incluyeElIdYElNombreDelAnfitrion() {
        Usuario anfitrion = new Usuario("Ana Anfitriona", "ana@correo.com", "hash", RolUsuario.ANFITRION);
        UUID anfitrionId = UUID.randomUUID();
        ReflectionTestUtils.setField(anfitrion, "id", anfitrionId);
        Publicacion publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
        publicacion.setAnfitrion(anfitrion);
        publicacion.setTitulo("Apartamento de prueba");
        publicacion.setDescripcion("Descripcion de prueba");
        publicacion.setUbicacionTextual("Calle 100");
        publicacion.setCiudad("Bogota");
        publicacion.setTipo(TipoAlojamiento.APARTAMENTO);
        publicacion.setCapacidad(2);
        publicacion.setPrecioNoche(new BigDecimal("100000"));
        publicacion.setEstado(EstadoPublicacion.ACTIVA);

        PublicacionResponse response = PublicacionResponse.desde(publicacion);

        assertEquals(anfitrionId, response.getAnfitrionId());
        assertEquals("Ana Anfitriona", response.getAnfitrionNombre());
    }

    @Test
    void setAnfitrionNombre_guardaElValor() {
        PublicacionResponse response = new PublicacionResponse();

        response.setAnfitrionNombre("Ana Anfitriona");

        assertEquals("Ana Anfitriona", response.getAnfitrionNombre());
    }
}