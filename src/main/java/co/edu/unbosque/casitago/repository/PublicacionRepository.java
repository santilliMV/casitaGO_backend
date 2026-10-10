package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.EstadoPublicacion;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.TipoAlojamiento;
import co.edu.unbosque.casitago.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PublicacionRepository extends JpaRepository<Publicacion, UUID> {

    List<Publicacion> findByAnfitrion(Usuario anfitrion);

    long countByEstado(EstadoPublicacion estado);

    List<Publicacion> findByAnfitrionAndEstado(Usuario anfitrion, EstadoPublicacion estado);

    // Postgres no puede deducir el tipo de un parámetro null dentro de "? IS NULL" ni dentro de LOWER().
    // Por eso: el estado va como parámetro (no escrito en la consulta), la ciudad lleva CAST, y el tipo
    // (un enum de Postgres) se filtra con un booleano que nunca es null, igual que cantidadServicios.
    @Query("SELECT DISTINCT p FROM Publicacion p "
            + "WHERE p.estado = :estado "
            + "AND (:ciudad IS NULL OR LOWER(p.ciudad) = LOWER(CAST(:ciudad AS String))) "
            + "AND (:filtrarTipo = false OR p.tipo = :tipo) "
            + "AND (:capacidad IS NULL OR p.capacidad >= :capacidad) "
            + "AND (:precioMin IS NULL OR p.precioNoche >= :precioMin) "
            + "AND (:precioMax IS NULL OR p.precioNoche <= :precioMax) "
            + "AND (:cantidadServicios = 0L OR "
            + "     (SELECT COUNT(DISTINCT LOWER(s.nombre)) FROM ServicioAlojamiento s "
            + "      WHERE s.publicacion = p AND LOWER(s.nombre) IN :servicios) = :cantidadServicios)")
    List<Publicacion> buscarPorEstado(@Param("estado") EstadoPublicacion estado,
                                      @Param("ciudad") String ciudad,
                                      @Param("filtrarTipo") boolean filtrarTipo,
                                      @Param("tipo") TipoAlojamiento tipo,
                                      @Param("capacidad") Integer capacidad,
                                      @Param("precioMin") BigDecimal precioMin,
                                      @Param("precioMax") BigDecimal precioMax,
                                      @Param("servicios") List<String> servicios,
                                      @Param("cantidadServicios") long cantidadServicios);

    default List<Publicacion> buscarActivas(String ciudad, TipoAlojamiento tipo, Integer capacidad,
                                            BigDecimal precioMin, BigDecimal precioMax,
                                            List<String> servicios, long cantidadServicios) {
        boolean filtrarTipo = tipo != null;
        TipoAlojamiento tipoConsulta = filtrarTipo ? tipo : TipoAlojamiento.OTRO;
        return buscarPorEstado(EstadoPublicacion.ACTIVA, ciudad, filtrarTipo, tipoConsulta, capacidad,
                precioMin, precioMax, servicios, cantidadServicios);
    }
}