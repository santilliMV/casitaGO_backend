package co.edu.unbosque.casitago.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CanalEventosServiceTest {

    private CanalEventosService canal;

    @BeforeEach
    void setUp() {
        canal = new CanalEventosService();
    }

    private static class EmisorDePrueba extends SseEmitter {
        int envios = 0;
        boolean roto = false;

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (roto) {
                throw new IOException("conexion cerrada");
            }
            envios++;
        }
    }

    private static final Map<String, String> DATOS = Map.of("mensaje", "hola");

    @Test
    void suscribir_devuelveUnEmisorConLaDuracionIndicadaYLoRegistra() {
        UUID usuarioId = UUID.randomUUID();

        SseEmitter emisor = canal.suscribir(usuarioId);

        assertThat(emisor.getTimeout()).isEqualTo(CanalEventosService.DURACION_CONEXION_MS);
        assertThat(canal.conexionesAbiertas(usuarioId)).isEqualTo(1);
    }

    @Test
    void registrar_enviaUnEventoDeConexion() {
        EmisorDePrueba emisor = new EmisorDePrueba();

        canal.registrar(UUID.randomUUID(), emisor);

        assertThat(emisor.envios).isEqualTo(1);
    }

    @Test
    void registrar_siElEmisorYaEstaRoto_noQuedaRegistrado() {
        UUID usuarioId = UUID.randomUUID();
        EmisorDePrueba emisor = new EmisorDePrueba();
        emisor.roto = true;

        canal.registrar(usuarioId, emisor);

        assertThat(canal.conexionesAbiertas(usuarioId)).isZero();
    }

    @Test
    void publicar_soloLlegaAlUsuarioIndicado() {
        UUID destinatario = UUID.randomUUID();
        UUID otro = UUID.randomUUID();
        EmisorDePrueba delDestinatario = new EmisorDePrueba();
        EmisorDePrueba delOtro = new EmisorDePrueba();
        canal.registrar(destinatario, delDestinatario);
        canal.registrar(otro, delOtro);

        canal.publicar(destinatario, "notificacion", DATOS);

        assertThat(delDestinatario.envios).isEqualTo(2);
        assertThat(delOtro.envios).isEqualTo(1);
    }

    @Test
    void publicar_llegaATodasLasConexionesDelMismoUsuario() {
        UUID usuarioId = UUID.randomUUID();
        EmisorDePrueba pestanaUno = new EmisorDePrueba();
        EmisorDePrueba pestanaDos = new EmisorDePrueba();
        canal.registrar(usuarioId, pestanaUno);
        canal.registrar(usuarioId, pestanaDos);

        canal.publicar(usuarioId, "mensaje", DATOS);

        assertThat(pestanaUno.envios).isEqualTo(2);
        assertThat(pestanaDos.envios).isEqualTo(2);
    }

    @Test
    void publicar_siUnaConexionSeRompe_laRetiraYSigueConLasDemas() {
        UUID usuarioId = UUID.randomUUID();
        EmisorDePrueba rota = new EmisorDePrueba();
        EmisorDePrueba sana = new EmisorDePrueba();
        canal.registrar(usuarioId, rota);
        canal.registrar(usuarioId, sana);
        rota.roto = true;

        canal.publicar(usuarioId, "mensaje", DATOS);

        assertThat(canal.conexionesAbiertas(usuarioId)).isEqualTo(1);
        assertThat(sana.envios).isEqualTo(2);
    }

    @Test
    void publicar_usuarioSinConexiones_noFalla() {
        assertThatCode(() -> canal.publicar(UUID.randomUUID(), "notificacion", DATOS))
                .doesNotThrowAnyException();
    }

    @Test
    void publicar_usuarioNulo_noHaceNada() {
        assertThatCode(() -> canal.publicar(null, "notificacion", DATOS))
                .doesNotThrowAnyException();
    }

    @Test
    void publicar_dentroDeUnaTransaccion_enviaSoloDespuesDeConfirmar() {
        UUID usuarioId = UUID.randomUUID();
        EmisorDePrueba emisor = new EmisorDePrueba();
        canal.registrar(usuarioId, emisor);

        TransactionSynchronizationManager.initSynchronization();
        try {
            canal.publicar(usuarioId, "notificacion", DATOS);

            assertThat(emisor.envios).isEqualTo(1);
            List<TransactionSynchronization> pendientes = TransactionSynchronizationManager.getSynchronizations();
            assertThat(pendientes).hasSize(1);

            pendientes.get(0).afterCommit();

            assertThat(emisor.envios).isEqualTo(2);
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void enviarLatido_llegaATodosYRetiraLasConexionesRotas() {
        UUID usuarioId = UUID.randomUUID();
        EmisorDePrueba sana = new EmisorDePrueba();
        EmisorDePrueba rota = new EmisorDePrueba();
        canal.registrar(usuarioId, sana);
        canal.registrar(usuarioId, rota);
        rota.roto = true;

        canal.enviarLatido();

        assertThat(sana.envios).isEqualTo(2);
        assertThat(canal.conexionesAbiertas(usuarioId)).isEqualTo(1);
    }
}