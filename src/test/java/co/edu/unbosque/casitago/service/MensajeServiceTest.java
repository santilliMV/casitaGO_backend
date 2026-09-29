package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.ConversacionResponse;
import co.edu.unbosque.casitago.dto.MensajeRequest;
import co.edu.unbosque.casitago.dto.MensajeResponse;
import co.edu.unbosque.casitago.entity.*;
import co.edu.unbosque.casitago.repository.MensajeRepository;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MensajeServiceTest {

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private MensajeRepository mensajeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private MensajeService mensajeService;

    private Usuario anfitrion;
    private Usuario huesped;

    @BeforeEach
    void setUp() {
        anfitrion = new Usuario("Ana Anfitriona", "ana@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(anfitrion, "id", UUID.randomUUID());

        huesped = new Usuario("Hugo Huesped", "hugo@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(huesped, "id", UUID.randomUUID());
    }

    private Publicacion publicacionExistente() {
        Publicacion publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
        publicacion.setAnfitrion(anfitrion);
        publicacion.setTitulo("Publicación de prueba");
        publicacion.setDescripcion("Descripción");
        publicacion.setUbicacionTextual("Ubicación");
        publicacion.setTipo(TipoAlojamiento.APARTAMENTO);
        publicacion.setCapacidad(2);
        publicacion.setPrecioNoche(new BigDecimal("100000"));
        publicacion.setEstado(EstadoPublicacion.ACTIVA);
        return publicacion;
    }

    private MensajeRequest mensajeRequest(String contenido, UUID huespedId) {
        MensajeRequest request = new MensajeRequest();
        request.setContenido(contenido);
        request.setHuespedId(huespedId);
        return request;
    }

    private Mensaje mensajeExistente(Publicacion publicacion, Usuario huesped, Usuario emisor, boolean leido) {
        Mensaje mensaje = new Mensaje();
        mensaje.setId(UUID.randomUUID());
        mensaje.setPublicacion(publicacion);
        mensaje.setHuesped(huesped);
        mensaje.setEmisor(emisor);
        mensaje.setContenido("Hola");
        mensaje.setLeido(leido);
        return mensaje;
    }

    // ---------- RF-31: enviar mensaje ----------

    @Test
    void enviarMensaje_comoHuesped_deberiaUsarSuPropioIdComoConversacion() {
        Publicacion publicacion = publicacionExistente();
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(inv -> {
            Mensaje mensaje = inv.getArgument(0);
            mensaje.setId(UUID.randomUUID());
            return mensaje;
        });

        MensajeResponse response = mensajeService.enviarMensaje(
                publicacion.getId(), huesped, mensajeRequest("Hola, ¿está disponible?", null));

        assertEquals(huesped.getId(), response.getHuespedId());
        assertEquals(huesped.getId(), response.getEmisorId());
        verify(usuarioRepository, never()).findById(any());
    }

    @Test
    void enviarMensaje_comoAnfitrionPropietario_deberiaUsarHuespedIdIndicado() {
        Publicacion publicacion = publicacionExistente();
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(usuarioRepository.findById(huesped.getId())).thenReturn(Optional.of(huesped));
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(inv -> {
            Mensaje mensaje = inv.getArgument(0);
            mensaje.setId(UUID.randomUUID());
            return mensaje;
        });

        MensajeResponse response = mensajeService.enviarMensaje(
                publicacion.getId(), anfitrion, mensajeRequest("Sí, disponible", huesped.getId()));

        assertEquals(huesped.getId(), response.getHuespedId());
        assertEquals(anfitrion.getId(), response.getEmisorId());
    }

    @Test
    void enviarMensaje_comoAnfitrionSinIndicarHuesped_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionExistente();
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mensajeService.enviarMensaje(publicacion.getId(), anfitrion, mensajeRequest("Hola", null)));

        assertEquals("Debes indicar a qué huésped le estás respondiendo", ex.getMessage());
        verify(mensajeRepository, never()).save(any());
    }

    @Test
    void enviarMensaje_comoOtroAnfitrion_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionExistente();
        Usuario otroAnfitrion = new Usuario("Otro", "otro@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(otroAnfitrion, "id", UUID.randomUUID());
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mensajeService.enviarMensaje(publicacion.getId(), otroAnfitrion,
                        mensajeRequest("Hola", huesped.getId())));

        assertEquals("Solo el ANFITRIÓN propietario puede responder en esta publicación", ex.getMessage());
    }

    @Test
    void enviarMensaje_conHuespedIdInexistente_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionExistente();
        UUID huespedIdInexistente = UUID.randomUUID();
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(usuarioRepository.findById(huespedIdInexistente)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mensajeService.enviarMensaje(publicacion.getId(), anfitrion,
                        mensajeRequest("Hola", huespedIdInexistente)));

        assertEquals("El huésped indicado no existe", ex.getMessage());
    }

    @Test
    void enviarMensaje_conHuespedIdQueNoEsHuesped_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionExistente();
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mensajeService.enviarMensaje(publicacion.getId(), anfitrion,
                        mensajeRequest("Hola", anfitrion.getId())));

        assertEquals("El usuario indicado no es un HUÉSPED", ex.getMessage());
    }

    @Test
    void enviarMensaje_conPublicacionInexistente_deberiaLanzarExcepcion() {
        UUID publicacionId = UUID.randomUUID();
        when(publicacionRepository.findById(publicacionId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mensajeService.enviarMensaje(publicacionId, huesped, mensajeRequest("Hola", null)));

        assertEquals("La publicación no existe", ex.getMessage());
    }

    // ---------- RF-31: listar conversación ----------

    @Test
    void listarConversacion_comoHuesped_deberiaMarcarComoLeidosLosMensajesDelAnfitrion() {
        Publicacion publicacion = publicacionExistente();
        Mensaje mensajeDelAnfitrion = mensajeExistente(publicacion, huesped, anfitrion, false);
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(mensajeRepository.findByPublicacionIdAndHuespedIdOrderByCreadoEnAsc(publicacion.getId(), huesped.getId()))
                .thenReturn(List.of(mensajeDelAnfitrion));

        List<MensajeResponse> resultado = mensajeService.listarConversacion(publicacion.getId(), huesped.getId(), huesped);

        assertEquals(1, resultado.size());
        assertTrue(mensajeDelAnfitrion.isLeido());
        verify(mensajeRepository).save(mensajeDelAnfitrion);
    }

    @Test
    void listarConversacion_noDeberiaMarcarComoLeidosLosMensajesPropios() {
        Publicacion publicacion = publicacionExistente();
        Mensaje mensajePropio = mensajeExistente(publicacion, huesped, huesped, false);
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(mensajeRepository.findByPublicacionIdAndHuespedIdOrderByCreadoEnAsc(publicacion.getId(), huesped.getId()))
                .thenReturn(List.of(mensajePropio));

        mensajeService.listarConversacion(publicacion.getId(), huesped.getId(), huesped);

        assertFalse(mensajePropio.isLeido());
        verify(mensajeRepository, never()).save(any());
    }

    @Test
    void listarConversacion_comoAnfitrionPropietario_deberiaPermitirlo() {
        Publicacion publicacion = publicacionExistente();
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(mensajeRepository.findByPublicacionIdAndHuespedIdOrderByCreadoEnAsc(publicacion.getId(), huesped.getId()))
                .thenReturn(List.of());

        List<MensajeResponse> resultado = mensajeService.listarConversacion(publicacion.getId(), huesped.getId(), anfitrion);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void listarConversacion_comoUsuarioAjeno_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionExistente();
        Usuario otroHuesped = new Usuario("Otro Huesped", "otro@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(otroHuesped, "id", UUID.randomUUID());
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mensajeService.listarConversacion(publicacion.getId(), huesped.getId(), otroHuesped));

        assertEquals("No tienes permiso para ver esta conversación", ex.getMessage());
    }

    @Test
    void listarConversacion_conPublicacionInexistente_deberiaLanzarExcepcion() {
        UUID publicacionId = UUID.randomUUID();
        when(publicacionRepository.findById(publicacionId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> mensajeService.listarConversacion(publicacionId, huesped.getId(), huesped));
    }

    // ---------- RF-31: listar mis conversaciones ----------

    @Test
    void listarMisConversaciones_deberiaAgruparPorPublicacionYHuesped() {
        Publicacion publicacion = publicacionExistente();
        Mensaje masReciente = mensajeExistente(publicacion, huesped, huesped, false);
        Mensaje masAntiguo = mensajeExistente(publicacion, huesped, anfitrion, true);

        when(mensajeRepository.buscarConversacionesDeUsuario(huesped.getId()))
                .thenReturn(List.of(masReciente, masAntiguo));

        List<ConversacionResponse> resultado = mensajeService.listarMisConversaciones(huesped.getId());

        assertEquals(1, resultado.size());
        assertEquals(huesped.getId(), resultado.get(0).getHuespedId());
    }

    @Test
    void listarMisConversaciones_sinMensajes_deberiaRetornarListaVacia() {
        when(mensajeRepository.buscarConversacionesDeUsuario(huesped.getId())).thenReturn(List.of());

        List<ConversacionResponse> resultado = mensajeService.listarMisConversaciones(huesped.getId());

        assertTrue(resultado.isEmpty());
    }
}