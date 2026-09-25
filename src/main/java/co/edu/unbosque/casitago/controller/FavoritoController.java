package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.service.FavoritoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class FavoritoController {

    private final FavoritoService favoritoService;

    public FavoritoController(FavoritoService favoritoService) {
        this.favoritoService = favoritoService;
    }

    // RF-32
    @PostMapping("/api/publicaciones/{id}/favoritos")
    public ResponseEntity<Object> agregarFavorito(@AuthenticationPrincipal Usuario usuario, @PathVariable UUID id) {
        try {
            favoritoService.agregarFavorito(usuario.getId(), id);
            return ResponseEntity.ok("Publicación agregada a favoritos.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // RF-32
    @DeleteMapping("/api/publicaciones/{id}/favoritos")
    public ResponseEntity<Object> eliminarFavorito(@AuthenticationPrincipal Usuario usuario, @PathVariable UUID id) {
        try {
            favoritoService.eliminarFavorito(usuario.getId(), id);
            return ResponseEntity.ok("Publicación eliminada de favoritos.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // RF-32
    @GetMapping("/api/favoritos")
    public ResponseEntity<Object> listarFavoritos(@AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(favoritoService.listarFavoritos(usuario.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}