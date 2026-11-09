package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.service.CanalEventosService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class EventosController {

    private final CanalEventosService canalEventosService;

    public EventosController(CanalEventosService canalEventosService) {
        this.canalEventosService = canalEventosService;
    }

    @GetMapping(value = "/api/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribirse(@AuthenticationPrincipal Usuario usuario) {
        return canalEventosService.suscribir(usuario.getId());
    }
}