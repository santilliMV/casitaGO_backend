package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.EstadoReserva;
import co.edu.unbosque.casitago.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}