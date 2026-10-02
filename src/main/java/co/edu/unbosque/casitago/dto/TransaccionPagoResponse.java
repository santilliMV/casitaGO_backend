package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.EstadoPago;
import co.edu.unbosque.casitago.entity.TransaccionPago;

import java.math.BigDecimal;
import java.util.UUID;

public class TransaccionPagoResponse extends RespuestaConMarcaDeTiempo {

    private UUID reservaId;
    private String proveedor;
    private EstadoPago estado;
    private BigDecimal monto;
    private String referenciaExterna;

    public static TransaccionPagoResponse desde(TransaccionPago transaccion) {
        TransaccionPagoResponse response = new TransaccionPagoResponse();
        response.setId(transaccion.getId());
        response.reservaId = transaccion.getReserva().getId();
        response.proveedor = transaccion.getProveedor();
        response.estado = transaccion.getEstado();
        response.monto = transaccion.getMonto();
        response.referenciaExterna = transaccion.getReferenciaExterna();
        response.setCreadoEn(transaccion.getCreadoEn());
        return response;
    }

    public UUID getReservaId() {
        return reservaId;
    }

    public void setReservaId(UUID reservaId) {
        this.reservaId = reservaId;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public void setEstado(EstadoPago estado) {
        this.estado = estado;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
    }

    public void setReferenciaExterna(String referenciaExterna) {
        this.referenciaExterna = referenciaExterna;
    }
}