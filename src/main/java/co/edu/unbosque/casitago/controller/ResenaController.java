package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.dto.ResenaRequest;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.service.ResenaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class ResenaController {

    @Autowired
    private ResenaService resenaService;

    @PostMapping("/api/reservas/{id}/resena")
    public ResponseEntity<Object> crearResena(@PathVariable UUID id,
                                              @Valid @RequestBody ResenaRequest request,
                                              @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(resenaService.crearResena(id, usuario, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api/publicaciones/{id}/resenas")
    public ResponseEntity<Object> listarResenas(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(resenaService.listarPorPublicacion(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}