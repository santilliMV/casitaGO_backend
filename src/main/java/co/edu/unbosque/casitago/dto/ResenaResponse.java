package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.Resena;

import java.util.UUID;

public class ResenaResponse extends RespuestaConMarcaDeTiempo {

    private UUID reservaId;
    private UUID publicacionId;
    private String huespedNombre;
    private int calificacion;
    private String comentario;

    public static ResenaResponse desde(Resena resena) {
        ResenaResponse response = new ResenaResponse();
        response.setId(resena.getId());
        response.setCreadoEn(resena.getCreadoEn());
        response.setReservaId(resena.getReserva().getId());
        response.setPublicacionId(resena.getPublicacion().getId());
        response.setHuespedNombre(resena.getHuesped().getNombre());
        response.setCalificacion(resena.getCalificacion());
        response.setComentario(resena.getComentario());
        return response;
    }

    public UUID getReservaId() {
        return reservaId;
    }

    public void setReservaId(UUID reservaId) {
        this.reservaId = reservaId;
    }

    public UUID getPublicacionId() {
        return publicacionId;
    }

    public void setPublicacionId(UUID publicacionId) {
        this.publicacionId = publicacionId;
    }

    public String getHuespedNombre() {
        return huespedNombre;
    }

    public void setHuespedNombre(String huespedNombre) {
        this.huespedNombre = huespedNombre;
    }

    public int getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(int calificacion) {
        this.calificacion = calificacion;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
}