package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.EventoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, UUID> {
    // RF-26 (consulta de eventos por parte de un ADMINISTRADOR) se implementa
    // en el módulo admin; aquí solo se necesita escritura por ahora.
}
