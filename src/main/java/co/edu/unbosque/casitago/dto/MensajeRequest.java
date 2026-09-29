package co.edu.unbosque.casitago.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class MensajeRequest {

    @NotBlank(message = "El contenido del mensaje es obligatorio")
    private String contenido;

    // Solo lo llena el ANFITRIÓN, para indicar a qué huésped le está
    // respondiendo (puede tener varias conversaciones sobre la misma publicación).
    // El HUÉSPED no lo necesita: la conversación siempre es consigo mismo.
    private UUID huespedId;

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public UUID getHuespedId() {
        return huespedId;
    }

    public void setHuespedId(UUID huespedId) {
        this.huespedId = huespedId;
    }
}