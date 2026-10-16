package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.ConversacionResponse;
import co.edu.unbosque.casitago.entity.Mensaje;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversacionDatosTest {

    private Usuario anfitrion;
    private Usuario huesped;
    private Publicacion publicacion;
    private Mensaje mensaje;

    @BeforeEach
    void setUp() {
        anfitrion = new Usuario("Ana Anfitriona", "ana@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(anfitrion, "id", UUID.randomUUID());

        huesped = new Usuario("Hugo Huesped", "hugo@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(huesped, "id", UUID.randomUUID());

        publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
        publicacion.setAnfitrion(anfitrion);
        publicacion.setTitulo("Apartamento en Chapinero");

        mensaje = new Mensaje();
        mensaje.setId(UUID.randomUUID());
        mensaje.setPublicacion(publicacion);
        mensaje.setHuesped(huesped);
        mensaje.setEmisor(huesped);
        mensaje.setContenido("Hola, ¿está disponible?");
    }

    @Test
    void desde_deberiaIncluirElTituloYLosNombresDeLosParticipantes() {
        ConversacionResponse response = ConversacionResponse.desde(mensaje);

        assertEquals("Apartamento en Chapinero", response.getTituloPublicacion());
        assertEquals("Hugo Huesped", response.getHuespedNombre());
        assertEquals("Ana Anfitriona", response.getAnfitrionNombre());
    }

    @Test
    void desde_deberiaConservarLosDatosQueYaTraia() {
        ConversacionResponse response = ConversacionResponse.desde(mensaje);

        assertEquals(publicacion.getId(), response.getPublicacionId());
        assertEquals(huesped.getId(), response.getHuespedId());
        assertEquals("Hola, ¿está disponible?", response.getUltimoMensaje());
        assertEquals(mensaje.getCreadoEn(), response.getFechaUltimoMensaje());
    }
}