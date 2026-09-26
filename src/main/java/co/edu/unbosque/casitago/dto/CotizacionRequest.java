package co.edu.unbosque.casitago.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class CotizacionRequest {

    @NotNull
    private UUID publicacionId;

    @NotNull
    @FutureOrPresent
    private LocalDate fechaLlegada;

    @NotNull
    @FutureOrPresent
    private LocalDate fechaSalida;

    @PositiveOrZero
    private BigDecimal serviciosAdicionales = BigDecimal.ZERO;

    public UUID getPublicacionId() {
        return publicacionId;
    }

    public void setPublicacionId(UUID publicacionId) {
        this.publicacionId = publicacionId;
    }

    public LocalDate getFechaLlegada() {
        return fechaLlegada;
    }

    public void setFechaLlegada(LocalDate fechaLlegada) {
        this.fechaLlegada = fechaLlegada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public BigDecimal getServiciosAdicionales() {
        return serviciosAdicionales;
    }

    public void setServiciosAdicionales(BigDecimal serviciosAdicionales) {
        this.serviciosAdicionales = serviciosAdicionales;
    }
}