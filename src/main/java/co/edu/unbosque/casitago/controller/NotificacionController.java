package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    @Autowired
    private NotificacionService notificacionService;

    @GetMapping("/mias")
    public ResponseEntity<Object> listarMisNotificaciones(@AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(notificacionService.listarMisNotificaciones(usuario.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/leida")
    public ResponseEntity<Object> marcarComoLeida(@PathVariable UUID id,
                                                  @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(notificacionService.marcarComoLeida(id, usuario));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}