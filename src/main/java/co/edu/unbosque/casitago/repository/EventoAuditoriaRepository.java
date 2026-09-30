package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.EventoAuditoria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, UUID> {

    // RF-26: consulta de eventos por parte de un ADMINISTRADOR.
    // Un filtro vacío ("" o filtrarUsuario = false) significa "no filtrar por ese campo".
    @Query("SELECT e FROM EventoAuditoria e "
            + "WHERE (:entidad = '' OR e.entidad = :entidad) "
            + "AND (:accion = '' OR e.accion = :accion) "
            + "AND (:filtrarUsuario = false OR e.usuarioId = :usuarioId) "
            + "ORDER BY e.creadoEn DESC")
    List<EventoAuditoria> buscar(@Param("entidad") String entidad,
                                 @Param("accion") String accion,
                                 @Param("filtrarUsuario") boolean filtrarUsuario,
                                 @Param("usuarioId") UUID usuarioId,
                                 Pageable pageable);
}