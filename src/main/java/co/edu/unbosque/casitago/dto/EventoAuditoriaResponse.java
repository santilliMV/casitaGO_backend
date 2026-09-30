package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.EventoAuditoria;

import java.util.Map;
import java.util.UUID;

public class EventoAuditoriaResponse extends RespuestaConMarcaDeTiempo {

    private UUID usuarioId;
    private String entidad;
    private String accion;
    private String resultado;
    private Map<String, Object> detalle;

    public static EventoAuditoriaResponse desde(EventoAuditoria evento) {
        EventoAuditoriaResponse response = new EventoAuditoriaResponse();
        response.setId(evento.getId());
        response.setCreadoEn(evento.getCreadoEn());
        response.setUsuarioId(evento.getUsuarioId());
        response.setEntidad(evento.getEntidad());
        response.setAccion(evento.getAccion());
        response.setResultado(evento.getResultado());
        response.setDetalle(evento.getDetalle());
        return response;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public Map<String, Object> getDetalle() {
        return detalle;
    }

    public void setDetalle(Map<String, Object> detalle) {
        this.detalle = detalle;
    }
}