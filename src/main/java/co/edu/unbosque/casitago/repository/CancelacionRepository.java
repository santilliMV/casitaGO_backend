package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.Cancelacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CancelacionRepository extends JpaRepository<Cancelacion, UUID> {
}