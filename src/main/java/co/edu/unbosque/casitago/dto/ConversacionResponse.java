package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.Mensaje;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ConversacionResponse {

    private UUID publicacionId;
    private UUID huespedId;
    private String ultimoMensaje;
    private OffsetDateTime fechaUltimoMensaje;
    private String tituloPublicacion;
    private String huespedNombre;
    private String anfitrionNombre;

    public static ConversacionResponse desde(Mensaje ultimoMensaje) {
        ConversacionResponse response = new ConversacionResponse();
        response.publicacionId = ultimoMensaje.getPublicacion().getId();
        response.huespedId = ultimoMensaje.getHuesped().getId();
        response.ultimoMensaje = ultimoMensaje.getContenido();
        response.fechaUltimoMensaje = ultimoMensaje.getCreadoEn();
        response.tituloPublicacion = ultimoMensaje.getPublicacion().getTitulo();
        response.huespedNombre = ultimoMensaje.getHuesped().getNombre();
        response.anfitrionNombre = ultimoMensaje.getPublicacion().getAnfitrion().getNombre();
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
    public String getTituloPublicacion() {
        return tituloPublicacion;
    }

    public void setTituloPublicacion(String tituloPublicacion) {
        this.tituloPublicacion = tituloPublicacion;
    }

    public String getHuespedNombre() {
        return huespedNombre;
    }

    public void setHuespedNombre(String huespedNombre) {
        this.huespedNombre = huespedNombre;
    }

    public String getAnfitrionNombre() {
        return anfitrionNombre;
    }

    public void setAnfitrionNombre(String anfitrionNombre) {
        this.anfitrionNombre = anfitrionNombre;
    }

    public void setFechaUltimoMensaje(OffsetDateTime fechaUltimoMensaje) {
        this.fechaUltimoMensaje = fechaUltimoMensaje;
    }
}