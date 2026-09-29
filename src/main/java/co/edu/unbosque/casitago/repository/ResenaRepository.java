package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.Resena;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResenaRepository extends JpaRepository<Resena, UUID> {

    boolean existsByReservaId(UUID reservaId);

    List<Resena> findByPublicacionIdAndOcultaFalseOrderByCreadoEnDesc(UUID publicacionId);
}