package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.service.BusquedaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/publicaciones")
public class BusquedaController {

    private final BusquedaService busquedaService;

    public BusquedaController(BusquedaService busquedaService) {
        this.busquedaService = busquedaService;
    }

    // RF-15
    @GetMapping("/buscar")
    public ResponseEntity<Object> buscar(
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer capacidad,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) List<String> servicios) {
        try {
            return ResponseEntity.ok(busquedaService.buscar(
                    ciudad, desde, hasta, capacidad, tipo, precioMin, precioMax, servicios));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // RF-16
    @GetMapping("/{id}/detalle")
    public ResponseEntity<Object> consultarDetalle(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(busquedaService.consultarDetalle(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // RF-30
    @GetMapping("/{id}/ubicacion")
    public ResponseEntity<Object> obtenerUbicacion(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(busquedaService.obtenerUbicacion(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}