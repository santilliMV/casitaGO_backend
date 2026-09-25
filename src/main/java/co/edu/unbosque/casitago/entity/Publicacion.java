package co.edu.unbosque.casitago.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "publicaciones")
public class Publicacion {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 100)
    private String ciudad;

    private Double latitud;

    private Double longitud;

    @ManyToOne
    @JoinColumn(name = "anfitrion_id", nullable = false)
    private Usuario anfitrion;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "ubicacion_textual", nullable = false, length = 250)
    private String ubicacionTextual;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private TipoAlojamiento tipo;

    @Column(nullable = false)
    private Integer capacidad;

    @Column(name = "precio_noche", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioNoche;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private EstadoPublicacion estado = EstadoPublicacion.BORRADOR;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServicioAlojamiento> servicios = new ArrayList<>();

    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReglaAlojamiento> reglas = new ArrayList<>();

    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImagenPublicacion> imagenes = new ArrayList<>();

    // getters y setters

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Usuario getAnfitrion() {
        return anfitrion;
    }

    public void setAnfitrion(Usuario anfitrion) {
        this.anfitrion = anfitrion;
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

    public TipoAlojamiento getTipo() {
        return tipo;
    }

    public void setTipo(TipoAlojamiento tipo) {
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

    public EstadoPublicacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoPublicacion estado) {
        this.estado = estado;
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

    public List<ServicioAlojamiento> getServicios() {
        return servicios;
    }

    public void setServicios(List<ServicioAlojamiento> servicios) {
        this.servicios = servicios;
    }

    public List<ReglaAlojamiento> getReglas() {
        return reglas;
    }

    public void setReglas(List<ReglaAlojamiento> reglas) {
        this.reglas = reglas;
    }

    public List<ImagenPublicacion> getImagenes() {
        return imagenes;
    }

    public void setImagenes(List<ImagenPublicacion> imagenes) {
        this.imagenes = imagenes;
    }
}