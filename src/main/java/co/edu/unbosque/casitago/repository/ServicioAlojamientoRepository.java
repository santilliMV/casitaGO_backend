package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.ServicioAlojamiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServicioAlojamientoRepository extends JpaRepository<ServicioAlojamiento, UUID> {
}