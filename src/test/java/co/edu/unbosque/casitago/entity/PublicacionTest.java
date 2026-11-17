package co.edu.unbosque.casitago.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicacionTest {

    @Test
    void nuevaPublicacion_empiezaEnBorradorYSinServiciosReglasNiImagenes() {
        Publicacion publicacion = new Publicacion();

        assertEquals(EstadoPublicacion.BORRADOR, publicacion.getEstado());
        assertTrue(publicacion.getServicios().isEmpty());
        assertTrue(publicacion.getReglas().isEmpty());
        assertTrue(publicacion.getImagenes().isEmpty());
        assertNotNull(publicacion.getCreadoEn());
        assertNotNull(publicacion.getActualizadoEn());
    }

    @Test
    void gettersYSetters_funcionanCorrectamente() {
        Publicacion publicacion = new Publicacion();
        UUID id = UUID.randomUUID();
        Usuario anfitrion = new Usuario();
        LocalDateTime fecha = LocalDateTime.of(2026, 11, 16, 10, 0);
        List<ServicioAlojamiento> servicios = List.of(new ServicioAlojamiento());
        List<ReglaAlojamiento> reglas = List.of(new ReglaAlojamiento());
        List<ImagenPublicacion> imagenes = List.of(new ImagenPublicacion());

        publicacion.setId(id);
        publicacion.setAnfitrion(anfitrion);
        publicacion.setTitulo("Cabaña en Villa de Leyva");
        publicacion.setDescripcion("Cabaña con chimenea");
        publicacion.setUbicacionTextual("Vereda El Roble, Villa de Leyva");
        publicacion.setCiudad("Villa de Leyva");
        publicacion.setLatitud(5.63);
        publicacion.setLongitud(-73.52);
        publicacion.setTipo(TipoAlojamiento.CASA);
        publicacion.setCapacidad(4);
        publicacion.setPrecioNoche(new BigDecimal("250000"));
        publicacion.setEstado(EstadoPublicacion.ACTIVA);
        publicacion.setCreadoEn(fecha);
        publicacion.setActualizadoEn(fecha);
        publicacion.setServicios(servicios);
        publicacion.setReglas(reglas);
        publicacion.setImagenes(imagenes);

        assertEquals(id, publicacion.getId());
        assertSame(anfitrion, publicacion.getAnfitrion());
        assertEquals("Cabaña en Villa de Leyva", publicacion.getTitulo());
        assertEquals("Cabaña con chimenea", publicacion.getDescripcion());
        assertEquals("Vereda El Roble, Villa de Leyva", publicacion.getUbicacionTextual());
        assertEquals("Villa de Leyva", publicacion.getCiudad());
        assertEquals(5.63, publicacion.getLatitud());
        assertEquals(-73.52, publicacion.getLongitud());
        assertEquals(TipoAlojamiento.CASA, publicacion.getTipo());
        assertEquals(4, publicacion.getCapacidad());
        assertEquals(new BigDecimal("250000"), publicacion.getPrecioNoche());
        assertEquals(EstadoPublicacion.ACTIVA, publicacion.getEstado());
        assertEquals(fecha, publicacion.getCreadoEn());
        assertEquals(fecha, publicacion.getActualizadoEn());
        assertSame(servicios, publicacion.getServicios());
        assertSame(reglas, publicacion.getReglas());
        assertSame(imagenes, publicacion.getImagenes());
    }
}
