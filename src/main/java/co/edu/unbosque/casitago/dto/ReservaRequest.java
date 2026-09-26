package co.edu.unbosque.casitago.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public class ReservaRequest {

    @NotNull
    private UUID cotizacionId;

    @NotNull
    @FutureOrPresent
    private LocalDate fechaLlegada;

    @NotNull
    @FutureOrPresent
    private LocalDate fechaSalida;

    public UUID getCotizacionId() {
        return cotizacionId;
    }

    public void setCotizacionId(UUID cotizacionId) {
        this.cotizacionId = cotizacionId;
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
}