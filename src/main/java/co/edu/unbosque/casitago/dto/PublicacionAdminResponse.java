package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.Publicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PublicacionAdminResponse {

    private UUID id;
    private String titulo;
    private String ciudad;
    private String tipo;
    private String estado;
    private BigDecimal precioNoche;
    private UUID anfitrionId;
    private String anfitrionNombre;
    private String anfitrionCorreo;
    private LocalDateTime creadoEn;

    public static PublicacionAdminResponse desde(Publicacion publicacion) {
        PublicacionAdminResponse response = new PublicacionAdminResponse();
        response.setId(publicacion.getId());
        response.setTitulo(publicacion.getTitulo());
        response.setCiudad(publicacion.getCiudad());
        response.setTipo(publicacion.getTipo().name());
        response.setEstado(publicacion.getEstado().name());
        response.setPrecioNoche(publicacion.getPrecioNoche());
        response.setAnfitrionId(publicacion.getAnfitrion().getId());
        response.setAnfitrionNombre(publicacion.getAnfitrion().getNombre());
        response.setAnfitrionCorreo(publicacion.getAnfitrion().getCorreo());
        response.setCreadoEn(publicacion.getCreadoEn());
        return response;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public BigDecimal getPrecioNoche() {
        return precioNoche;
    }

    public void setPrecioNoche(BigDecimal precioNoche) {
        this.precioNoche = precioNoche;
    }

    public UUID getAnfitrionId() {
        return anfitrionId;
    }

    public void setAnfitrionId(UUID anfitrionId) {
        this.anfitrionId = anfitrionId;
    }

    public String getAnfitrionNombre() {
        return anfitrionNombre;
    }

    public void setAnfitrionNombre(String anfitrionNombre) {
        this.anfitrionNombre = anfitrionNombre;
    }

    public String getAnfitrionCorreo() {
        return anfitrionCorreo;
    }

    public void setAnfitrionCorreo(String anfitrionCorreo) {
        this.anfitrionCorreo = anfitrionCorreo;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}