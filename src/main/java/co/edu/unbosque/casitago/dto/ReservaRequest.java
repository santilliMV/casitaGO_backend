package co.edu.unbosque.casitago.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class ReservaRequest extends RangoFechasRequest {

    @NotNull
    private UUID cotizacionId;

    public UUID getCotizacionId() {
        return cotizacionId;
    }

    public void setCotizacionId(UUID cotizacionId) {
        this.cotizacionId = cotizacionId;
    }
}