package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.Mensaje;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ConversacionResponse {

    private UUID publicacionId;
    private UUID huespedId;
    private String ultimoMensaje;
    private OffsetDateTime fechaUltimoMensaje;

    public static ConversacionResponse desde(Mensaje ultimoMensaje) {
        ConversacionResponse response = new ConversacionResponse();
        response.publicacionId = ultimoMensaje.getPublicacion().getId();
        response.huespedId = ultimoMensaje.getHuesped().getId();
        response.ultimoMensaje = ultimoMensaje.getContenido();
        response.fechaUltimoMensaje = ultimoMensaje.getCreadoEn();
        return response;
    }

    public UUID getPublicacionId() {
        return publicacionId;
    }

    public void setPublicacionId(UUID publicacionId) {
        this.publicacionId = publicacionId;
    }

    public UUID getHuespedId() {
        return huespedId;
    }

    public void setHuespedId(UUID huespedId) {
        this.huespedId = huespedId;
    }

    public String getUltimoMensaje() {
        return ultimoMensaje;
    }

    public void setUltimoMensaje(String ultimoMensaje) {
        this.ultimoMensaje = ultimoMensaje;
    }

    public OffsetDateTime getFechaUltimoMensaje() {
        return fechaUltimoMensaje;
    }

    public void setFechaUltimoMensaje(OffsetDateTime fechaUltimoMensaje) {
        this.fechaUltimoMensaje = fechaUltimoMensaje;
    }
}