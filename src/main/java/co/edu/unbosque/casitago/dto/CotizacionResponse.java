package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.Cotizacion;

import java.math.BigDecimal;
import java.util.UUID;

public class CotizacionResponse extends RespuestaConMarcaDeTiempo {

    private UUID publicacionId;
    private Integer noches;
    private BigDecimal precioBase;
    private BigDecimal tarifaLimpieza;
    private BigDecimal tarifaServicio;
    private BigDecimal serviciosAdicionales;
    private BigDecimal total;

    public static CotizacionResponse desde(Cotizacion cotizacion) {
        CotizacionResponse response = new CotizacionResponse();
        response.setId(cotizacion.getId());
        response.publicacionId = cotizacion.getPublicacion().getId();
        response.noches = cotizacion.getNoches();
        response.precioBase = cotizacion.getPrecioBase();
        response.tarifaLimpieza = cotizacion.getTarifaLimpieza();
        response.tarifaServicio = cotizacion.getTarifaServicio();
        response.serviciosAdicionales = cotizacion.getServiciosAdicionales();
        response.total = cotizacion.getTotal();
        response.setCreadoEn(cotizacion.getCreadoEn());
        return response;
    }

    public UUID getPublicacionId() {
        return publicacionId;
    }

    public void setPublicacionId(UUID publicacionId) {
        this.publicacionId = publicacionId;
    }

    public Integer getNoches() {
        return noches;
    }

    public void setNoches(Integer noches) {
        this.noches = noches;
    }

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    public void setPrecioBase(BigDecimal precioBase) {
        this.precioBase = precioBase;
    }

    public BigDecimal getTarifaLimpieza() {
        return tarifaLimpieza;
    }

    public void setTarifaLimpieza(BigDecimal tarifaLimpieza) {
        this.tarifaLimpieza = tarifaLimpieza;
    }

    public BigDecimal getTarifaServicio() {
        return tarifaServicio;
    }

    public void setTarifaServicio(BigDecimal tarifaServicio) {
        this.tarifaServicio = tarifaServicio;
    }

    public BigDecimal getServiciosAdicionales() {
        return serviciosAdicionales;
    }

    public void setServiciosAdicionales(BigDecimal serviciosAdicionales) {
        this.serviciosAdicionales = serviciosAdicionales;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}