package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MensajeRepository extends JpaRepository<Mensaje, UUID> {

    // Historial completo de una conversación, del más antiguo al más reciente
    List<Mensaje> findByPublicacionIdAndHuespedIdOrderByCreadoEnAsc(UUID publicacionId, UUID huespedId);

    // Todos los mensajes donde el usuario participa (como huésped o como anfitrión
    // dueño de la publicación), del más reciente al más antiguo. El service
    // se encarga de agrupar esto en conversaciones.
    @Query("SELECT m FROM Mensaje m "
            + "WHERE m.huesped.id = :usuarioId OR m.publicacion.anfitrion.id = :usuarioId "
            + "ORDER BY m.creadoEn DESC")
    List<Mensaje> buscarConversacionesDeUsuario(@Param("usuarioId") UUID usuarioId);
}