package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.EstadoReserva;
import co.edu.unbosque.casitago.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    List<Reserva> findByHuespedIdOrderByCreadoEnDesc(UUID huespedId);

    Optional<Reserva> findByIdempotencyKey(String idempotencyKey);

    default List<Reserva> buscarSolapadas(UUID publicacionId, LocalDate inicio, LocalDate fin) {
        return buscarSolapadasInterno(publicacionId, inicio, fin, EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);
    }

    @Query("SELECT r FROM Reserva r "
            + "WHERE r.publicacion.id = :publicacionId "
            + "AND r.estado IN (:pendiente, :confirmada) "
            + "AND r.fechaLlegada < :fin "
            + "AND r.fechaSalida > :inicio")
    List<Reserva> buscarSolapadasInterno(@Param("publicacionId") UUID publicacionId,
                                         @Param("inicio") LocalDate inicio,
                                         @Param("fin") LocalDate fin,
                                         @Param("pendiente") EstadoReserva pendiente,
                                         @Param("confirmada") EstadoReserva confirmada);

    long countByEstado(EstadoReserva estado);

    @Query("SELECT SUM(r.cotizacion.total) FROM Reserva r WHERE r.estado IN :estados")
    BigDecimal sumarIngresos(@Param("estados") List<EstadoReserva> estados);

    @Query("SELECT p.ciudad, COUNT(r) FROM Reserva r JOIN r.publicacion p "
            + "WHERE r.estado <> :excluido GROUP BY p.ciudad ORDER BY COUNT(r) DESC")
    List<Object[]> ciudadesConMasReservas(@Param("excluido") EstadoReserva excluido, Pageable pageable);
}