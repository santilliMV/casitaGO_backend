package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.ImagenPublicacion;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.ReglaAlojamiento;
import co.edu.unbosque.casitago.entity.ServicioAlojamiento;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PublicacionResponse extends DatosPublicacion {

    private UUID id;
    private UUID anfitrionId;
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
        response.setTitulo(publicacion.getTitulo());
        response.setDescripcion(publicacion.getDescripcion());
        response.setUbicacionTextual(publicacion.getUbicacionTextual());
        response.setTipo(publicacion.getTipo().name());
        response.setCapacidad(publicacion.getCapacidad());
        response.setPrecioNoche(publicacion.getPrecioNoche());
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