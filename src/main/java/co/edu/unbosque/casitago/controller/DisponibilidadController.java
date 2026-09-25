package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.dto.BloqueoRequest;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.service.DisponibilidadService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/publicaciones")
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    public DisponibilidadController(DisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    // RF-13
    @PostMapping("/{id}/bloqueos")
    public ResponseEntity<Object> crearBloqueo(@AuthenticationPrincipal Usuario usuario,
                                               @PathVariable UUID id,
                                               @RequestBody BloqueoRequest request) {
        try {
            return ResponseEntity.ok(disponibilidadService.crearBloqueo(usuario.getId(), id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // RF-13
    @GetMapping("/{id}/periodos")
    public ResponseEntity<Object> listarPeriodos(@AuthenticationPrincipal Usuario usuario,
                                                 @PathVariable UUID id) {
        try {
            return ResponseEntity.ok(disponibilidadService.listarPeriodos(usuario.getId(), id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // RF-13
    @DeleteMapping("/{id}/bloqueos/{periodoId}")
    public ResponseEntity<Object> eliminarBloqueo(@AuthenticationPrincipal Usuario usuario,
                                                  @PathVariable UUID id,
                                                  @PathVariable UUID periodoId) {
        try {
            disponibilidadService.eliminarBloqueo(usuario.getId(), id, periodoId);
            return ResponseEntity.ok("Bloqueo eliminado correctamente.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // RF-14
    @GetMapping("/{id}/disponibilidad")
    public ResponseEntity<Object> consultarDisponibilidad(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            return ResponseEntity.ok(disponibilidadService.consultarDisponibilidad(id, desde, hasta));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}