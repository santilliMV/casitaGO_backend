package co.edu.unbosque.casitago.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import co.edu.unbosque.casitago.entity.ImagenPublicacion;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.ReglaAlojamiento;
import co.edu.unbosque.casitago.entity.ServicioAlojamiento;
import java.util.stream.Collectors;

public class PublicacionResponse {

    private UUID id;
    private UUID anfitrionId;
    private String titulo;
    private String descripcion;
    private String ubicacionTextual;
    private String tipo;
    private Integer capacidad;
    private BigDecimal precioNoche;
    private String estado;
    private List<String> servicios;
    private List<String> reglas;
    private List<String> imagenes;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    public static PublicacionResponse desde(Publicacion publicacion) {
        PublicacionResponse response = new PublicacionResponse();
        response.id = publicacion.getId();
        response.anfitrionId = publicacion.getAnfitrion().getId();
        response.titulo = publicacion.getTitulo();
        response.descripcion = publicacion.getDescripcion();
        response.ubicacionTextual = publicacion.getUbicacionTextual();
        response.tipo = publicacion.getTipo().name();
        response.capacidad = publicacion.getCapacidad();
        response.precioNoche = publicacion.getPrecioNoche();
        response.estado = publicacion.getEstado().name();
        response.servicios = publicacion.getServicios().stream()
                .map(ServicioAlojamiento::getNombre)
                .collect(Collectors.toList());
        response.reglas = publicacion.getReglas().stream()
                .map(ReglaAlojamiento::getDescripcion)
                .collect(Collectors.toList());
        response.imagenes = publicacion.getImagenes().stream()
                .map(ImagenPublicacion::getUrl)
                .collect(Collectors.toList());
        response.creadoEn = publicacion.getCreadoEn();
        response.actualizadoEn = publicacion.getActualizadoEn();
        return response;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAnfitrionId() {
        return anfitrionId;
    }

    public void setAnfitrionId(UUID anfitrionId) {
        this.anfitrionId = anfitrionId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getUbicacionTextual() {
        return ubicacionTextual;
    }

    public void setUbicacionTextual(String ubicacionTextual) {
        this.ubicacionTextual = ubicacionTextual;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(Integer capacidad) {
        this.capacidad = capacidad;
    }

    public BigDecimal getPrecioNoche() {
        return precioNoche;
    }

    public void setPrecioNoche(BigDecimal precioNoche) {
        this.precioNoche = precioNoche;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public List<String> getServicios() {
        return servicios;
    }

    public void setServicios(List<String> servicios) {
        this.servicios = servicios;
    }

    public List<String> getReglas() {
        return reglas;
    }

    public void setReglas(List<String> reglas) {
        this.reglas = reglas;
    }

    public List<String> getImagenes() {
        return imagenes;
    }

    public void setImagenes(List<String> imagenes) {
        this.imagenes = imagenes;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(LocalDateTime actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}