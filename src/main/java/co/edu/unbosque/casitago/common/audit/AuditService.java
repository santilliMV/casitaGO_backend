package co.edu.unbosque.casitago.common.audit;

import co.edu.unbosque.casitago.entity.EventoAuditoria;
import co.edu.unbosque.casitago.repository.EventoAuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Punto único para registrar eventos de auditoría (RNF-07: "toda acción de
 * seguridad o cambio de estado relevante debe dejar un registro trazable").
 * Cualquier service de cualquier módulo debe inyectar esto en vez de escribir
 * directamente a EventoAuditoriaRepository.
 */
@Component
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final EventoAuditoriaRepository eventoAuditoriaRepository;

    public AuditService(EventoAuditoriaRepository eventoAuditoriaRepository) {
        this.eventoAuditoriaRepository = eventoAuditoriaRepository;
    }

    public void registrar(UUID usuarioId, String entidad, String accion, String resultado, Map<String, Object> detalle) {
        try {
            eventoAuditoriaRepository.save(new EventoAuditoria(usuarioId, entidad, accion, resultado, detalle));
        } catch (Exception e) {
            // La auditoría nunca debe tumbar la operación de negocio que la originó.
            log.error("No se pudo registrar evento de auditoría [{}::{}={}]: {}", entidad, accion, resultado, e.getMessage());
        }
    }

    public void registrar(UUID usuarioId, String entidad, String accion, String resultado) {
        registrar(usuarioId, entidad, accion, resultado, Map.of());
    }
}
