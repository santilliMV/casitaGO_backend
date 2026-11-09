package co.edu.unbosque.casitago.service;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Canal de eventos en tiempo real (SSE) por usuario: avisa al navegador cuando llega una
 * notificación o un mensaje. Las conexiones viven en memoria, así que sirve para una sola
 * instancia del servicio. La conexión se cierra antes de que venza el token (30 minutos) y
 * el front vuelve a conectarse.
 */
@Service
public class CanalEventosService {

    static final long DURACION_CONEXION_MS = 25L * 60 * 1000;
    private static final long LATIDO_MS = 20_000;

    private final Map<UUID, List<SseEmitter>> conexiones = new ConcurrentHashMap<>();

    public SseEmitter suscribir(UUID usuarioId) {
        SseEmitter emitter = new SseEmitter(DURACION_CONEXION_MS);
        registrar(usuarioId, emitter);
        return emitter;
    }

    void registrar(UUID usuarioId, SseEmitter emitter) {
        conexiones.computeIfAbsent(usuarioId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> retirar(usuarioId, emitter));
        emitter.onTimeout(() -> retirar(usuarioId, emitter));
        emitter.onError(error -> retirar(usuarioId, emitter));
        enviar(usuarioId, emitter, SseEmitter.event().name("conectado").data("ok"));
    }

    /**
     * Avisa al usuario. Si hay una transacción en curso, espera a que se confirme para que el
     * navegador encuentre ya guardado lo que va a pedir. Nunca lanza errores hacia quien llama.
     */
    public void publicar(UUID usuarioId, String tipo, Map<String, String> datos) {
        if (usuarioId == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviarATodos(usuarioId, tipo, datos);
                }
            });
        } else {
            enviarATodos(usuarioId, tipo, datos);
        }
    }

    // Los proxies cierran las conexiones que se quedan calladas: se envía un comentario vacío.
    @Scheduled(fixedRate = LATIDO_MS)
    public void enviarLatido() {
        conexiones.forEach((usuarioId, abiertas) -> {
            for (SseEmitter emitter : abiertas) {
                enviar(usuarioId, emitter, SseEmitter.event().comment("latido"));
            }
        });
    }

    int conexionesAbiertas(UUID usuarioId) {
        return conexiones.getOrDefault(usuarioId, List.of()).size();
    }

    private void enviarATodos(UUID usuarioId, String tipo, Map<String, String> datos) {
        for (SseEmitter emitter : conexiones.getOrDefault(usuarioId, List.of())) {
            enviar(usuarioId, emitter, SseEmitter.event().name(tipo).data(datos, MediaType.APPLICATION_JSON));
        }
    }

    private void enviar(UUID usuarioId, SseEmitter emitter, SseEmitter.SseEventBuilder evento) {
        try {
            emitter.send(evento);
        } catch (IOException | IllegalStateException e) {
            retirar(usuarioId, emitter);
        }
    }

    private void retirar(UUID usuarioId, SseEmitter emitter) {
        conexiones.computeIfPresent(usuarioId, (id, abiertas) -> {
            abiertas.remove(emitter);
            return abiertas.isEmpty() ? null : abiertas;
        });
    }
}