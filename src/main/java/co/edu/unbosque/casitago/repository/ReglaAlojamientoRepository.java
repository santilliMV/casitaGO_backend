package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.ReglaAlojamiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReglaAlojamientoRepository extends JpaRepository<ReglaAlojamiento, UUID> {
}