package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.common.audit.AuditService;
import co.edu.unbosque.casitago.dto.ResenaRequest;
import co.edu.unbosque.casitago.entity.EstadoReserva;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.Resena;
import co.edu.unbosque.casitago.entity.Reserva;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResenaAuditoriaTest {

    @Mock
    private ResenaRepository resenaRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private AuditService auditService;

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

    private ResenaRequest requestDePrueba() {
        ResenaRequest request = new ResenaRequest();
        request.setCalificacion(4);
        request.setComentario("Muy buena estadía");
        return request;
    }

    @Test
    void crearResena_estanciaTerminada_deberiaRegistrarEventoDeAuditoria() {
        Reserva reserva = reservaDe(huesped, EstadoReserva.CONFIRMADA, LocalDate.now().minusDays(1));
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(resenaRepository.existsByReservaId(reserva.getId())).thenReturn(false);
        when(resenaRepository.save(any(Resena.class))).thenAnswer(inv -> {
            Resena resena = inv.getArgument(0);
            resena.setId(UUID.randomUUID());
            return resena;
        });

        resenaService.crearResena(reserva.getId(), huesped, requestDePrueba());

        verify(auditService).registrar(eq(huesped.getId()), eq("resenas"), eq("CREAR_RESENA"), eq("EXITOSO"),
                argThat(detalle -> reserva.getId().toString().equals(detalle.get("reservaId"))
                        && publicacion.getId().toString().equals(detalle.get("publicacionId"))
                        && "4".equals(detalle.get("calificacion"))));
    }

    @Test
    void crearResena_deOtroHuesped_noDeberiaRegistrarEvento() {
        Usuario otro = new Usuario("Otro Huesped", "otro@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(otro, "id", UUID.randomUUID());
        Reserva reserva = reservaDe(otro, EstadoReserva.CONFIRMADA, LocalDate.now().minusDays(1));
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        UUID reservaId = reserva.getId();
        ResenaRequest request = requestDePrueba();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> resenaService.crearResena(reservaId, huesped, request));

        assertEquals("No tienes permiso para reseñar esta reserva", ex.getMessage());
        verifyNoInteractions(auditService);
    }

    @Test
    void crearResena_estanciaSinTerminar_noDeberiaRegistrarEvento() {
        Reserva reserva = reservaDe(huesped, EstadoReserva.CONFIRMADA, LocalDate.now().plusDays(5));
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        UUID reservaId = reserva.getId();
        ResenaRequest request = requestDePrueba();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> resenaService.crearResena(reservaId, huesped, request));

        assertEquals("Solo puedes reseñar una estancia completada", ex.getMessage());
        verifyNoInteractions(auditService);
    }

    @Test
    void crearResena_yaReseñada_noDeberiaGuardarNiRegistrarEvento() {
        Reserva reserva = reservaDe(huesped, EstadoReserva.COMPLETADA, LocalDate.now().minusDays(1));
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(resenaRepository.existsByReservaId(reserva.getId())).thenReturn(true);
        UUID reservaId = reserva.getId();
        ResenaRequest request = requestDePrueba();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> resenaService.crearResena(reservaId, huesped, request));

        assertEquals("Ya reseñaste esta reserva", ex.getMessage());
        verify(resenaRepository, never()).save(any(Resena.class));
        verifyNoInteractions(auditService);
    }
}