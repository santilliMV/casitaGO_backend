package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.dto.BloquearPublicacionRequest;
import co.edu.unbosque.casitago.dto.PublicacionRequest;
import co.edu.unbosque.casitago.dto.PublicacionResponse;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.service.PublicacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/publicaciones")
public class PublicacionController {

    private final PublicacionService publicacionService;

    public PublicacionController(PublicacionService publicacionService) {
        this.publicacionService = publicacionService;
    }

    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody PublicacionRequest request,
                                        @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(publicacionService.crearPublicacion(usuario.getId(), request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> editar(@PathVariable UUID id,
                                         @Valid @RequestBody PublicacionRequest request,
                                         @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(publicacionService.editarPublicacion(usuario.getId(), id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<Object> activar(@PathVariable UUID id, @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(publicacionService.activarPublicacion(usuario.getId(), id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/pausar")
    public ResponseEntity<Object> pausar(@PathVariable UUID id, @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(publicacionService.pausarPublicacion(usuario.getId(), id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/mias")
    public ResponseEntity<Object> misPublicaciones(@AuthenticationPrincipal Usuario usuario) {
        try {
            List<PublicacionResponse> publicaciones = publicacionService.consultarPublicacionesDeAnfitrion(usuario.getId());
            return ResponseEntity.ok(publicaciones);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/bloquear")
    public ResponseEntity<Object> bloquear(@PathVariable UUID id,
                                           @Valid @RequestBody BloquearPublicacionRequest request,
                                           @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(publicacionService.bloquearPublicacion(usuario.getId(), id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "/{id}/imagenes", consumes = "multipart/form-data")
    public ResponseEntity<Object> agregarImagen(@PathVariable UUID id,
                                                @RequestParam("archivo") MultipartFile archivo,
                                                @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(publicacionService.agregarImagen(usuario.getId(), id, archivo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}