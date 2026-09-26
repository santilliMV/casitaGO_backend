package co.edu.unbosque.casitago.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cotizaciones")
public class Cotizacion {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @Column(nullable = false)
    private Integer noches;

    @Column(name = "precio_base", nullable = false)
    private BigDecimal precioBase;

    @Column(name = "tarifa_limpieza", nullable = false)
    private BigDecimal tarifaLimpieza = BigDecimal.ZERO;

    @Column(name = "tarifa_servicio", nullable = false)
    private BigDecimal tarifaServicio = BigDecimal.ZERO;

    @Column(name = "servicios_adicionales", nullable = false)
    private BigDecimal serviciosAdicionales = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }

    public void setPublicacion(Publicacion publicacion) {
        this.publicacion = publicacion;
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

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(OffsetDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}