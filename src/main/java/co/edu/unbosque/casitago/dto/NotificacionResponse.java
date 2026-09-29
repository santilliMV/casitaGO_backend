package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.Notificacion;

public class NotificacionResponse extends RespuestaConMarcaDeTiempo {

    private String tipoEvento;
    private String mensaje;
    private boolean leida;

    public static NotificacionResponse desde(Notificacion notificacion) {
        NotificacionResponse response = new NotificacionResponse();
        response.setId(notificacion.getId());
        response.setCreadoEn(notificacion.getCreadoEn());
        response.setTipoEvento(notificacion.getTipoEvento());
        response.setMensaje(notificacion.getMensaje());
        response.setLeida(notificacion.isLeida());
        return response;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }
}