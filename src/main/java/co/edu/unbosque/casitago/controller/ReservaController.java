package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.dto.*;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    @PostMapping("/cotizacion")
    public ResponseEntity<Object> generarCotizacion(@Valid @RequestBody CotizacionRequest request) {
        try {
            return ResponseEntity.ok(reservaService.generarCotizacion(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<Object> crearReserva(@Valid @RequestBody ReservaRequest request,
                                               @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(reservaService.crearReserva(request, usuario));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/mias")
    public ResponseEntity<Object> listarMisReservas(@AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(reservaService.listarPorHuesped(usuario.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Object> cancelarReserva(@PathVariable UUID id,
                                                  @Valid @RequestBody CancelacionRequest request,
                                                  @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(reservaService.cancelarReserva(id, usuario, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}