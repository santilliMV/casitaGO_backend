package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.PublicacionResponse;
import co.edu.unbosque.casitago.dto.UbicacionResponse;
import co.edu.unbosque.casitago.entity.EstadoPeriodo;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.TipoAlojamiento;
import co.edu.unbosque.casitago.repository.PeriodoDisponibilidadRepository;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import org.springframework.stereotype.Service;
import co.edu.unbosque.casitago.repository.ReservaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BusquedaService {

    private final PublicacionRepository publicacionRepository;
    private final PeriodoDisponibilidadRepository periodoRepository;
    private final GeocodificacionService geocodificacionService;
    private final ReservaRepository reservaRepository;

    public BusquedaService(
            PublicacionRepository publicacionRepository,
            PeriodoDisponibilidadRepository periodoRepository,
            ReservaRepository reservaRepository,
            GeocodificacionService geocodificacionService
    ) {
        this.publicacionRepository = publicacionRepository;
        this.periodoRepository = periodoRepository;
        this.reservaRepository = reservaRepository;
        this.geocodificacionService = geocodificacionService;
    }

    // ---------- RF-15: buscar alojamientos ----------
    public List<PublicacionResponse> buscar(String ciudad, LocalDate desde, LocalDate hasta, Integer capacidad,
                                            String tipoTexto, BigDecimal precioMin, BigDecimal precioMax,
                                            List<String> servicios) {
        TipoAlojamiento tipo = resolverTipo(tipoTexto);
        validarFechas(desde, hasta);
        List<String> serviciosMinuscula = normalizarServicios(servicios);

        List<Publicacion> publicaciones = publicacionRepository.buscarActivas(
                ciudad, tipo, capacidad, precioMin, precioMax, serviciosMinuscula, serviciosMinuscula.size());

        List<PublicacionResponse> respuesta = new ArrayList<>();
        for (Publicacion publicacion : publicaciones) {
            if (desde == null || estaDisponibleEnFechas(publicacion.getId(), desde, hasta)) {
                respuesta.add(PublicacionResponse.desde(publicacion));
            }
        }
        return respuesta;
    }

    private TipoAlojamiento resolverTipo(String tipoTexto) {
        if (tipoTexto == null) {
            return null;
        }
        try {
            return TipoAlojamiento.valueOf(tipoTexto);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("El tipo de alojamiento indicado no es válido.");
        }
    }

    private void validarFechas(LocalDate desde, LocalDate hasta) {
        if ((desde == null) != (hasta == null)) {
            throw new RuntimeException("Debes indicar ambas fechas (desde y hasta) o ninguna.");
        }
        if (desde != null && !hasta.isAfter(desde)) {
            throw new RuntimeException("La fecha de fin debe ser posterior a la fecha de inicio.");
        }
    }

    private List<String> normalizarServicios(List<String> servicios) {
        List<String> serviciosMinuscula = new ArrayList<>();
        if (servicios != null) {
            for (String servicio : servicios) {
                serviciosMinuscula.add(servicio.toLowerCase());
            }
        }
        return serviciosMinuscula;
    }

    private boolean estaDisponibleEnFechas(UUID publicacionId, LocalDate desde, LocalDate hasta) {
        boolean bloqueado = !periodoRepository.buscarSolapados(publicacionId, desde, hasta).isEmpty();
        boolean reservado = !reservaRepository.buscarSolapadas(publicacionId, desde, hasta).isEmpty();
        return !bloqueado && !reservado;
    }

    // ---------- RF-16: consultar detalle de un alojamiento ----------
    public PublicacionResponse consultarDetalle(UUID publicacionId) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new RuntimeException("Publicación no encontrada."));
        return PublicacionResponse.desde(publicacion);
    }

    // ---------- RF-30: obtener ubicación en mapa ----------
    public UbicacionResponse obtenerUbicacion(UUID publicacionId) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new RuntimeException("Publicación no encontrada."));

        if (publicacion.getLatitud() == null || publicacion.getLongitud() == null) {
            double[] coordenadas = geocodificacionService.geocodificar(publicacion.getUbicacionTextual());
            if (coordenadas == null) {
                throw new RuntimeException("No se pudo ubicar esta publicación en el mapa.");
            }
            publicacion.setLatitud(coordenadas[0]);
            publicacion.setLongitud(coordenadas[1]);
            publicacionRepository.save(publicacion);
        }

        UbicacionResponse respuesta = new UbicacionResponse();
        respuesta.setPublicacionId(publicacion.getId());
        respuesta.setLatitud(publicacion.getLatitud());
        respuesta.setLongitud(publicacion.getLongitud());
        return respuesta;
    }
}