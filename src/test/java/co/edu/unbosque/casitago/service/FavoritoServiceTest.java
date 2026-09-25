package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.PublicacionResponse;
import co.edu.unbosque.casitago.entity.*;
import co.edu.unbosque.casitago.repository.FavoritoRepository;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoritoServiceTest {

    @Mock
    private FavoritoRepository favoritoRepository;

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private FavoritoService favoritoService;

    private Usuario huesped;
    private Publicacion publicacion;

    @BeforeEach
    void setUp() {
        huesped = new Usuario("Hugo Huesped", "hugo@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(huesped, "id", UUID.randomUUID());

        publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
        publicacion.setAnfitrion(huesped); // no importa el rol para este test, solo que no sea null
        publicacion.setTitulo("Publicación existente");
        publicacion.setDescripcion("Descripción existente");
        publicacion.setUbicacionTextual("Cartagena, Bolívar");
        publicacion.setCiudad("Cartagena");
        publicacion.setTipo(TipoAlojamiento.APARTAMENTO);
        publicacion.setCapacidad(2);
        publicacion.setPrecioNoche(new BigDecimal("100000"));
        publicacion.setEstado(EstadoPublicacion.ACTIVA);
    }

    // ---------- RF-32: agregar favorito ----------

    @Test
    void agregarFavorito_noEstabaEnFavoritos_deberiaGuardarlo() {
        when(usuarioRepository.findById(huesped.getId())).thenReturn(Optional.of(huesped));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(favoritoRepository.existsByUsuarioIdAndPublicacionId(huesped.getId(), publicacion.getId()))
                .thenReturn(false);

        favoritoService.agregarFavorito(huesped.getId(), publicacion.getId());

        verify(favoritoRepository).save(any(Favorito.class));
    }

    @Test
    void agregarFavorito_yaEstabaEnFavoritos_deberiaLanzarExcepcion() {
        when(usuarioRepository.findById(huesped.getId())).thenReturn(Optional.of(huesped));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(favoritoRepository.existsByUsuarioIdAndPublicacionId(huesped.getId(), publicacion.getId()))
                .thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> favoritoService.agregarFavorito(huesped.getId(), publicacion.getId()));

        assertEquals("Esta publicación ya está en tus favoritos.", ex.getMessage());
        verify(favoritoRepository, never()).save(any());
    }

    @Test
    void agregarFavorito_publicacionInexistente_deberiaLanzarExcepcion() {
        when(usuarioRepository.findById(huesped.getId())).thenReturn(Optional.of(huesped));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> favoritoService.agregarFavorito(huesped.getId(), publicacion.getId()));
    }

    // ---------- RF-32: eliminar favorito ----------

    @Test
    void eliminarFavorito_existente_deberiaEliminarlo() {
        Favorito favorito = new Favorito();
        favorito.setId(UUID.randomUUID());
        favorito.setUsuario(huesped);
        favorito.setPublicacion(publicacion);

        when(favoritoRepository.findByUsuarioIdAndPublicacionId(huesped.getId(), publicacion.getId()))
                .thenReturn(Optional.of(favorito));

        favoritoService.eliminarFavorito(huesped.getId(), publicacion.getId());

        verify(favoritoRepository).delete(favorito);
    }

    @Test
    void eliminarFavorito_inexistente_deberiaLanzarExcepcion() {
        when(favoritoRepository.findByUsuarioIdAndPublicacionId(huesped.getId(), publicacion.getId()))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> favoritoService.eliminarFavorito(huesped.getId(), publicacion.getId()));

        assertEquals("Esta publicación no está en tus favoritos.", ex.getMessage());
    }

    // ---------- RF-32: listar favoritos ----------

    @Test
    void listarFavoritos_deberiaRetornarLasPublicaciones() {
        Favorito favorito = new Favorito();
        favorito.setId(UUID.randomUUID());
        favorito.setUsuario(huesped);
        favorito.setPublicacion(publicacion);

        when(favoritoRepository.findByUsuarioId(huesped.getId())).thenReturn(List.of(favorito));

        List<PublicacionResponse> resultado = favoritoService.listarFavoritos(huesped.getId());

        assertEquals(1, resultado.size());
        assertEquals(publicacion.getId(), resultado.get(0).getId());
    }

    @Test
    void listarFavoritos_sinFavoritos_deberiaRetornarListaVacia() {
        when(favoritoRepository.findByUsuarioId(huesped.getId())).thenReturn(List.of());

        List<PublicacionResponse> resultado = favoritoService.listarFavoritos(huesped.getId());

        assertTrue(resultado.isEmpty());
    }
}