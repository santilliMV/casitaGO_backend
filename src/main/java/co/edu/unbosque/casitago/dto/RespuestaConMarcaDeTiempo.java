package co.edu.unbosque.casitago.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public abstract class RespuestaConMarcaDeTiempo {

    private UUID id;
    private OffsetDateTime creadoEn;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(OffsetDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}