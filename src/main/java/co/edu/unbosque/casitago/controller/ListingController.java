package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.dto.ActualizarPublicacionRequest;
import co.edu.unbosque.casitago.dto.BloquearPublicacionRequest;
import co.edu.unbosque.casitago.dto.CrearPublicacionRequest;
import co.edu.unbosque.casitago.dto.PublicacionResponse;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.service.ListingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/publicaciones")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    // ---------- RF-08: crear publicación ----------
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody CrearPublicacionRequest request,
                                   @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(listingService.crearPublicacion(usuario.getId(), request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---------- RF-09: editar publicación ----------
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable UUID id,
                                    @Valid @RequestBody ActualizarPublicacionRequest request,
                                    @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(listingService.editarPublicacion(usuario.getId(), id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---------- RF-10: activar publicación ----------
    @PatchMapping("/{id}/activar")
    public ResponseEntity<?> activar(@PathVariable UUID id, @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(listingService.activarPublicacion(usuario.getId(), id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---------- RF-11: pausar publicación ----------
    @PatchMapping("/{id}/pausar")
    public ResponseEntity<?> pausar(@PathVariable UUID id, @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(listingService.pausarPublicacion(usuario.getId(), id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---------- RF-12: consultar publicaciones del ANFITRIÓN autenticado ----------
    @GetMapping("/mias")
    public ResponseEntity<?> misPublicaciones(@AuthenticationPrincipal Usuario usuario) {
        try {
            List<PublicacionResponse> publicaciones = listingService.consultarPublicacionesDeAnfitrion(usuario.getId());
            return ResponseEntity.ok(publicaciones);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---------- RF-24: ADMINISTRADOR bloquea publicación ----------
    @PatchMapping("/{id}/bloquear")
    public ResponseEntity<?> bloquear(@PathVariable UUID id,
                                      @Valid @RequestBody BloquearPublicacionRequest request,
                                      @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(listingService.bloquearPublicacion(usuario.getId(), id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---------- RF-08 (imágenes): subir imagen a una publicación ----------
    @PostMapping(value = "/{id}/imagenes", consumes = "multipart/form-data")
    public ResponseEntity<?> agregarImagen(@PathVariable UUID id,
                                           @RequestParam("archivo") MultipartFile archivo,
                                           @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(listingService.agregarImagen(usuario.getId(), id, archivo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}