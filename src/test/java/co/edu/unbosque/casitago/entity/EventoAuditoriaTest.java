package co.edu.unbosque.casitago.entity;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventoAuditoriaTest {

    @Test
    void constructor_yPrePersist_asignanCreadoEn() {
        UUID usuarioId = UUID.randomUUID();
        EventoAuditoria evento = new EventoAuditoria(usuarioId, "usuarios", "LOGIN", "EXITOSO", Map.of("ip", "127.0.0.1"));

        assertThat(evento.getId()).isNull(); // aún no se ha persistido

        evento.prePersist();

        assertThat(evento.getId()).isNull(); // prePersist no asigna el id, eso lo hace JPA
    }
}