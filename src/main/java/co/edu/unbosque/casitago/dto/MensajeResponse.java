package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.Mensaje;

import java.util.UUID;

public class MensajeResponse extends RespuestaConMarcaDeTiempo {

    private UUID publicacionId;
    private UUID huespedId;
    private UUID emisorId;
    private String contenido;
    private boolean leido;

    public static MensajeResponse desde(Mensaje mensaje) {
        MensajeResponse response = new MensajeResponse();
        response.setId(mensaje.getId());
        response.publicacionId = mensaje.getPublicacion().getId();
        response.huespedId = mensaje.getHuesped().getId();
        response.emisorId = mensaje.getEmisor().getId();
        response.contenido = mensaje.getContenido();
        response.leido = mensaje.isLeido();
        response.setCreadoEn(mensaje.getCreadoEn());
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

    public UUID getEmisorId() {
        return emisorId;
    }

    public void setEmisorId(UUID emisorId) {
        this.emisorId = emisorId;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public boolean isLeido() {
        return leido;
    }

    public void setLeido(boolean leido) {
        this.leido = leido;
    }
}