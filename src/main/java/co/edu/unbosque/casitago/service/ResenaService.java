package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.ResenaRequest;
import co.edu.unbosque.casitago.dto.ResenaResponse;
import co.edu.unbosque.casitago.dto.ResenasPublicacionResponse;
import co.edu.unbosque.casitago.entity.EstadoReserva;
import co.edu.unbosque.casitago.entity.Resena;
import co.edu.unbosque.casitago.entity.Reserva;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import co.edu.unbosque.casitago.repository.ReservaRepository;
import co.edu.unbosque.casitago.repository.ResenaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ResenaService {

    @Autowired
    private ResenaRepository resenaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private PublicacionRepository publicacionRepository;

    public ResenaResponse crearResena(UUID reservaId, Usuario huesped, ResenaRequest request) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RuntimeException("La reserva no existe"));

        if (!reserva.getHuesped().getId().equals(huesped.getId())) {
            throw new RuntimeException("No tienes permiso para reseñar esta reserva");
        }

        if (!estanciaCompletada(reserva)) {
            throw new RuntimeException("Solo puedes reseñar una estancia completada");
        }

        if (resenaRepository.existsByReservaId(reservaId)) {
            throw new RuntimeException("Ya reseñaste esta reserva");
        }

        Resena resena = new Resena();
        resena.setHuesped(huesped);
        resena.setReserva(reserva);
        resena.setPublicacion(reserva.getPublicacion());
        resena.setCalificacion(request.getCalificacion());
        resena.setComentario(request.getComentario());

        resena = resenaRepository.save(resena);
        return ResenaResponse.desde(resena);
    }

    public ResenasPublicacionResponse listarPorPublicacion(UUID publicacionId) {
        if (!publicacionRepository.existsById(publicacionId)) {
            throw new RuntimeException("La publicación no existe");
        }

        List<Resena> resenas = resenaRepository.findByPublicacionIdAndOcultaFalseOrderByCreadoEnDesc(publicacionId);

        List<ResenaResponse> respuestas = new ArrayList<>();
        int suma = 0;
        for (Resena resena : resenas) {
            respuestas.add(ResenaResponse.desde(resena));
            suma += resena.getCalificacion();
        }

        double promedio = 0;
        if (!resenas.isEmpty()) {
            promedio = Math.round((double) suma / resenas.size() * 10) / 10.0;
        }

        ResenasPublicacionResponse response = new ResenasPublicacionResponse();
        response.setPromedio(promedio);
        response.setTotal(resenas.size());
        response.setResenas(respuestas);
        return response;
    }

    private boolean estanciaCompletada(Reserva reserva) {
        if (reserva.getEstado() == EstadoReserva.COMPLETADA) {
            return true;
        }
        return reserva.getEstado() == EstadoReserva.CONFIRMADA
                && !LocalDate.now().isBefore(reserva.getFechaSalida());
    }
}