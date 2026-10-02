package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.EstadoPago;
import co.edu.unbosque.casitago.entity.TransaccionPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, UUID> {

    List<TransaccionPago> findByReservaIdOrderByCreadoEnDesc(UUID reservaId);

    Optional<TransaccionPago> findFirstByReservaIdAndEstado(UUID reservaId, EstadoPago estado);
}