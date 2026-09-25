package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.PublicacionResponse;
import co.edu.unbosque.casitago.entity.Favorito;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.FavoritoRepository;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;

    public FavoritoService(
            FavoritoRepository favoritoRepository,
            PublicacionRepository publicacionRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.favoritoRepository = favoritoRepository;
        this.publicacionRepository = publicacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ---------- RF-32: guardar en favoritos ----------
    @Transactional
    public void agregarFavorito(UUID usuarioId, UUID publicacionId) {
        Usuario usuario = buscarUsuario(usuarioId);
        Publicacion publicacion = buscarPublicacion(publicacionId);

        if (favoritoRepository.existsByUsuarioIdAndPublicacionId(usuarioId, publicacionId)) {
            throw new RuntimeException("Esta publicación ya está en tus favoritos.");
        }

        Favorito favorito = new Favorito();
        favorito.setUsuario(usuario);
        favorito.setPublicacion(publicacion);
        favoritoRepository.save(favorito);
    }

    // ---------- RF-32: quitar de favoritos ----------
    @Transactional
    public void eliminarFavorito(UUID usuarioId, UUID publicacionId) {
        Favorito favorito = favoritoRepository.findByUsuarioIdAndPublicacionId(usuarioId, publicacionId)
                .orElseThrow(() -> new RuntimeException("Esta publicación no está en tus favoritos."));

        favoritoRepository.delete(favorito);
    }

    // ---------- RF-32: listar mis favoritos ----------
    public List<PublicacionResponse> listarFavoritos(UUID usuarioId) {
        List<PublicacionResponse> respuesta = new ArrayList<>();
        for (Favorito favorito : favoritoRepository.findByUsuarioId(usuarioId)) {
            respuesta.add(PublicacionResponse.desde(favorito.getPublicacion()));
        }
        return respuesta;
    }

    private Usuario buscarUsuario(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
    }

    private Publicacion buscarPublicacion(UUID publicacionId) {
        return publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new RuntimeException("Publicación no encontrada."));
    }
}