package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CotizacionRepository extends JpaRepository<Cotizacion, UUID> {
}