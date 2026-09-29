package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.ResenaRequest;
import co.edu.unbosque.casitago.dto.ResenaResponse;
import co.edu.unbosque.casitago.dto.ResenasPublicacionResponse;
import co.edu.unbosque.casitago.entity.*;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import co.edu.unbosque.casitago.repository.ReservaRepository;
import co.edu.unbosque.casitago.repository.ResenaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResenaServiceTest {

    @Mock
    private ResenaRepository resenaRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private PublicacionRepository publicacionRepository;

    @InjectMocks
    private ResenaService resenaService;

    private Usuario huesped;
    private Publicacion publicacion;

    @BeforeEach
    void setUp() {
        huesped = new Usuario("Hugo Huesped", "hugo@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(huesped, "id", UUID.randomUUID());

        publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
    }

    private Reserva reservaDe(Usuario dueno, EstadoReserva estado, LocalDate salida) {
        Reserva reserva = new Reserva();
        reserva.setId(UUID.randomUUID());
        reserva.setHuesped(dueno);
        reserva.setPublicacion(publicacion);
        reserva.setFechaLlegada(salida.minusDays(3));
        reserva.setFechaSalida(salida);
        reserva.setEstado(estado);
        return reserva;
    }

    private ResenaRequest requestDePrueba(int calificacion) {
        ResenaRequest request = new ResenaRequest();
        request.setCalificacion(calificacion);
        request.setComentario("Excelente estadía");
        return request;
    }

    private Resena resenaConCalificacion(int calificacion) {
        Resena resena = new Resena();
        resena.setId(UUID.randomUUID());
        resena.setHuesped(huesped);
        resena.setReserva(reservaDe(huesped, EstadoReserva.CONFIRMADA, LocalDate.now().minusDays(5)));
        resena.setPublicacion(publicacion);
        resena.setCalificacion(calificacion);
        return resena;
    }

    // ---------- RF-22: crear reseña ----------

    @Test
    void crearResena_estanciaTerminada_deberiaGuardar() {
        Reserva reserva = reservaDe(huesped, EstadoReserva.CONFIRMADA, LocalDate.now().minusDays(1));
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(resenaRepository.existsByReservaId(reserva.getId())).thenReturn(false);
        when(resenaRepository.save(any(Resena.class))).thenAnswer(inv -> {
            Resena resena = inv.getArgument(0);
            resena.setId(UUID.randomUUID());
            return resena;
        });

        ResenaResponse response = resenaService.crearResena(reserva.getId(), huesped, requestDePrueba(5));

        assertEquals(5, response.getCalificacion());
        assertEquals("Excelente estadía", response.getComentario());
        assertEquals(reserva.getId(), response.getReservaId());
        assertEquals(publicacion.getId(), response.getPublicacionId());
        assertEquals("Hugo Huesped", response.getHuespedNombre());
    }

    @Test
    void crearResena_reservaInexistente_deberiaLanzarExcepcion() {
        UUID reservaId = UUID.randomUUID();
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> resenaService.crearResena(reservaId, huesped, requestDePrueba(5)));

        assertEquals("La reserva no existe", ex.getMessage());
    }

    @Test
    void crearResena_deOtroHuesped_deberiaLanzarExcepcion() {
        Usuario otro = new Usuario("Otro Huesped", "otro@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(otro, "id", UUID.randomUUID());
        Reserva reserva = reservaDe(otro, EstadoReserva.CONFIRMADA, LocalDate.now().minusDays(1));
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> resenaService.crearResena(reserva.getId(), huesped, requestDePrueba(5)));

        assertEquals("No tienes permiso para reseñar esta reserva", ex.getMessage());
        verify(resenaRepository, never()).save(any());
    }

    @Test
    void crearResena_estanciaAunNoTermina_deberiaLanzarExcepcion() {
        Reserva reserva = reservaDe(huesped, EstadoReserva.CONFIRMADA, LocalDate.now().plusDays(5));
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> resenaService.crearResena(reserva.getId(), huesped, requestDePrueba(5)));

        assertEquals("Solo puedes reseñar una estancia completada", ex.getMessage());
        verify(resenaRepository, never()).save(any());
    }

    @Test
    void crearResena_reservaCancelada_deberiaLanzarExcepcion() {
        Reserva reserva = reservaDe(huesped, EstadoReserva.CANCELADA, LocalDate.now().minusDays(5));
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> resenaService.crearResena(reserva.getId(), huesped, requestDePrueba(5)));

        assertEquals("Solo puedes reseñar una estancia completada", ex.getMessage());
    }

    @Test
    void crearResena_yaReseñada_deberiaLanzarExcepcion() {
        Reserva reserva = reservaDe(huesped, EstadoReserva.CONFIRMADA, LocalDate.now().minusDays(1));
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(resenaRepository.existsByReservaId(reserva.getId())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> resenaService.crearResena(reserva.getId(), huesped, requestDePrueba(5)));

        assertEquals("Ya reseñaste esta reserva", ex.getMessage());
        verify(resenaRepository, never()).save(any());
    }

    // ---------- RF-23: consultar reseñas de una publicación ----------

    @Test
    void listarPorPublicacion_deberiaCalcularPromedioYTotal() {
        when(publicacionRepository.existsById(publicacion.getId())).thenReturn(true);
        when(resenaRepository.findByPublicacionIdAndOcultaFalseOrderByCreadoEnDesc(publicacion.getId()))
                .thenReturn(List.of(resenaConCalificacion(5), resenaConCalificacion(4)));

        ResenasPublicacionResponse response = resenaService.listarPorPublicacion(publicacion.getId());

        assertEquals(2, response.getTotal());
        assertEquals(4.5, response.getPromedio());
        assertEquals(2, response.getResenas().size());
    }

    @Test
    void listarPorPublicacion_sinReseñas_deberiaDevolverPromedioCero() {
        when(publicacionRepository.existsById(publicacion.getId())).thenReturn(true);
        when(resenaRepository.findByPublicacionIdAndOcultaFalseOrderByCreadoEnDesc(publicacion.getId()))
                .thenReturn(List.of());

        ResenasPublicacionResponse response = resenaService.listarPorPublicacion(publicacion.getId());

        assertEquals(0, response.getTotal());
        assertEquals(0.0, response.getPromedio());
        assertTrue(response.getResenas().isEmpty());
    }

    @Test
    void listarPorPublicacion_publicacionInexistente_deberiaLanzarExcepcion() {
        UUID publicacionId = UUID.randomUUID();
        when(publicacionRepository.existsById(publicacionId)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> resenaService.listarPorPublicacion(publicacionId));

        assertEquals("La publicación no existe", ex.getMessage());
    }
}