package co.edu.unbosque.casitago.common.audit;

import co.edu.unbosque.casitago.entity.EventoAuditoria;
import co.edu.unbosque.casitago.repository.EventoAuditoriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private EventoAuditoriaRepository eventoAuditoriaRepository;

    private AuditService auditService;

    @Test
    void registrar_guardaElEventoConLosDatosDados() {
        auditService = new AuditService(eventoAuditoriaRepository);
        UUID usuarioId = UUID.randomUUID();

        auditService.registrar(usuarioId, "usuarios", "LOGIN", "EXITOSO", Map.of("ip", "127.0.0.1"));

        verify(eventoAuditoriaRepository).save(any(EventoAuditoria.class));
    }

    @Test
    void registrar_sobrecargaSinDetalle_usaMapaVacio() {
        auditService = new AuditService(eventoAuditoriaRepository);
        UUID usuarioId = UUID.randomUUID();

        auditService.registrar(usuarioId, "usuarios", "LOGIN", "EXITOSO");

        verify(eventoAuditoriaRepository).save(any(EventoAuditoria.class));
    }

    @Test
    void registrar_siElRepositorioFalla_noPropagaLaExcepcion() {
        auditService = new AuditService(eventoAuditoriaRepository);
        when(eventoAuditoriaRepository.save(any())).thenThrow(new RuntimeException("caída de BD"));

        // La auditoría nunca debe tumbar la operación de negocio que la originó.
        assertThatCode(() -> auditService.registrar(UUID.randomUUID(), "usuarios", "LOGIN", "EXITOSO"))
                .doesNotThrowAnyException();
    }
}