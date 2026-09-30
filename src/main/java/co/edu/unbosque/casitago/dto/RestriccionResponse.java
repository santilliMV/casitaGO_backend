package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.RestriccionUsuario;

import java.util.UUID;

public class RestriccionResponse extends RespuestaConMarcaDeTiempo {

    private UUID usuarioId;
    private String motivo;
    private UUID creadoPorId;
    private boolean activa;

    public static RestriccionResponse desde(RestriccionUsuario restriccion) {
        RestriccionResponse response = new RestriccionResponse();
        response.setId(restriccion.getId());
        response.setCreadoEn(restriccion.getCreadoEn());
        response.setUsuarioId(restriccion.getUsuario().getId());
        response.setMotivo(restriccion.getMotivo());
        response.setCreadoPorId(restriccion.getCreadoPor().getId());
        response.setActiva(restriccion.isActiva());
        return response;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public UUID getCreadoPorId() {
        return creadoPorId;
    }

    public void setCreadoPorId(UUID creadoPorId) {
        this.creadoPorId = creadoPorId;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}