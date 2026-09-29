package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.dto.ConversacionResponse;
import co.edu.unbosque.casitago.dto.MensajeRequest;
import co.edu.unbosque.casitago.dto.MensajeResponse;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.service.MensajeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class MensajeController {

    @Autowired
    private MensajeService mensajeService;

    @PostMapping("/api/publicaciones/{id}/mensajes")
    public ResponseEntity<Object> enviarMensaje(@PathVariable UUID id,
                                                @Valid @RequestBody MensajeRequest request,
                                                @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(mensajeService.enviarMensaje(id, usuario, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api/publicaciones/{id}/mensajes/{huespedId}")
    public ResponseEntity<Object> listarConversacion(@PathVariable UUID id,
                                                     @PathVariable UUID huespedId,
                                                     @AuthenticationPrincipal Usuario usuario) {
        try {
            List<MensajeResponse> mensajes = mensajeService.listarConversacion(id, huespedId, usuario);
            return ResponseEntity.ok(mensajes);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api/mensajes/mias")
    public ResponseEntity<Object> listarMisConversaciones(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(mensajeService.listarMisConversaciones(usuario.getId()));
    }
}