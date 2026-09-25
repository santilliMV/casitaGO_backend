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

    List<Publicacion> findByAnfitrionAndEstado(Usuario anfitrion, EstadoPublicacion estado);

    @Query("SELECT DISTINCT p FROM Publicacion p "
            + "WHERE p.estado = co.edu.unbosque.casitago.entity.EstadoPublicacion.ACTIVA "
            + "AND (:ciudad IS NULL OR LOWER(p.ciudad) = LOWER(:ciudad)) "
            + "AND (:tipo IS NULL OR p.tipo = :tipo) "
            + "AND (:capacidad IS NULL OR p.capacidad >= :capacidad) "
            + "AND (:precioMin IS NULL OR p.precioNoche >= :precioMin) "
            + "AND (:precioMax IS NULL OR p.precioNoche <= :precioMax) "
            + "AND (:cantidadServicios = 0L OR "
            + "     (SELECT COUNT(DISTINCT LOWER(s.nombre)) FROM ServicioAlojamiento s "
            + "      WHERE s.publicacion = p AND LOWER(s.nombre) IN :servicios) = :cantidadServicios)")
    List<Publicacion> buscarActivas(@Param("ciudad") String ciudad,
                                    @Param("tipo") TipoAlojamiento tipo,
                                    @Param("capacidad") Integer capacidad,
                                    @Param("precioMin") BigDecimal precioMin,
                                    @Param("precioMax") BigDecimal precioMax,
                                    @Param("servicios") List<String> servicios,
                                    @Param("cantidadServicios") long cantidadServicios);
}