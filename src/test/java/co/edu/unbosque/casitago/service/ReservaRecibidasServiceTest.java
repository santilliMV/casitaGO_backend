package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.ReservaRecibidaResponse;
import co.edu.unbosque.casitago.entity.Cotizacion;
import co.edu.unbosque.casitago.entity.EstadoReserva;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.Reserva;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaRecibidasServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @InjectMocks
    private ReservaService reservaService;

    private Usuario anfitrion;
    private Usuario huesped;

    @BeforeEach
    void setUp() {
        anfitrion = new Usuario("Ana Anfitriona", "ana@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(anfitrion, "id", UUID.randomUUID());

        huesped = new Usuario("Luis Huesped", "luis@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(huesped, "id", UUID.randomUUID());
    }

    @Test
    void listarRecibidas_anfitrion_deberiaDevolverReservasConTituloYHuesped() {
        Publicacion publicacion = new Publicacion();
        ReflectionTestUtils.setField(publicacion, "id", UUID.randomUUID());
        publicacion.setTitulo("Apartamento en Chapinero");
        publicacion.setAnfitrion(anfitrion);

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setTotal(new BigDecimal("340000"));

        Reserva reserva = new Reserva();
        ReflectionTestUtils.setField(reserva, "id", UUID.randomUUID());
        reserva.setHuesped(huesped);
        reserva.setPublicacion(publicacion);
        reserva.setCotizacion(cotizacion);
        reserva.setFechaLlegada(LocalDate.of(2026, 12, 1));
        reserva.setFechaSalida(LocalDate.of(2026, 12, 3));
        reserva.setEstado(EstadoReserva.CONFIRMADA);

        when(reservaRepository.findByPublicacionAnfitrionIdOrderByCreadoEnDesc(anfitrion.getId()))
                .thenReturn(List.of(reserva));

        List<ReservaRecibidaResponse> resultado = reservaService.listarRecibidas(anfitrion);

        assertEquals(1, resultado.size());
        assertEquals("Apartamento en Chapinero", resultado.get(0).getTituloPublicacion());
        assertEquals("Luis Huesped", resultado.get(0).getHuespedNombre());
        assertEquals(EstadoReserva.CONFIRMADA, resultado.get(0).getEstado());
        assertEquals(0, new BigDecimal("340000").compareTo(resultado.get(0).getTotal()));
    }

    @Test
    void listarRecibidas_sinReservas_deberiaDevolverListaVacia() {
        when(reservaRepository.findByPublicacionAnfitrionIdOrderByCreadoEnDesc(anfitrion.getId()))
                .thenReturn(List.of());

        List<ReservaRecibidaResponse> resultado = reservaService.listarRecibidas(anfitrion);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void listarRecibidas_noEsAnfitrion_deberiaLanzarExcepcion() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> reservaService.listarRecibidas(huesped));

        assertEquals("Solo un ANFITRIÓN puede consultar las reservas recibidas", ex.getMessage());
        verifyNoInteractions(reservaRepository);
    }
}