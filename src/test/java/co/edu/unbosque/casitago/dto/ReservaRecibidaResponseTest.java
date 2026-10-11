package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.EstadoReserva;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReservaRecibidaResponseTest {

    @Test
    void settersYGetters_deberianGuardarLosValores() {
        UUID publicacionId = UUID.randomUUID();
        ReservaRecibidaResponse response = new ReservaRecibidaResponse();

        response.setPublicacionId(publicacionId);
        response.setTituloPublicacion("Apartamento en Chapinero");
        response.setHuespedNombre("Luis Huesped");
        response.setFechaLlegada(LocalDate.of(2026, 12, 1));
        response.setFechaSalida(LocalDate.of(2026, 12, 3));
        response.setEstado(EstadoReserva.PENDIENTE);
        response.setTotal(new BigDecimal("340000"));

        assertEquals(publicacionId, response.getPublicacionId());
        assertEquals("Apartamento en Chapinero", response.getTituloPublicacion());
        assertEquals("Luis Huesped", response.getHuespedNombre());
        assertEquals(LocalDate.of(2026, 12, 1), response.getFechaLlegada());
        assertEquals(LocalDate.of(2026, 12, 3), response.getFechaSalida());
        assertEquals(EstadoReserva.PENDIENTE, response.getEstado());
        assertEquals(0, new BigDecimal("340000").compareTo(response.getTotal()));
    }
}