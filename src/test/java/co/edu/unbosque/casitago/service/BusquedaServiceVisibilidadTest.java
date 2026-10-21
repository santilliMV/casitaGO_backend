package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.PublicacionResponse;
import co.edu.unbosque.casitago.dto.UbicacionResponse;
import co.edu.unbosque.casitago.entity.EstadoPublicacion;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.TipoAlojamiento;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.PeriodoDisponibilidadRepository;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import co.edu.unbosque.casitago.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusquedaServiceVisibilidadTest {

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private PeriodoDisponibilidadRepository periodoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private GeocodificacionService geocodificacionService;

    @InjectMocks
    private BusquedaService busquedaService;

    // ---------- detalle ----------

    @Test
    void consultarDetalle_publicacionActiva_laVeCualquierUsuario() {
        Publicacion publicacion = publicacion(EstadoPublicacion.ACTIVA);
        Usuario huesped = usuario("Hugo Huesped", RolUsuario.HUESPED);
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        PublicacionResponse response = busquedaService.consultarDetalle(publicacion.getId(), huesped);

        assertEquals(publicacion.getId(), response.getId());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoPublicacion.class, names = "ACTIVA", mode = EnumSource.Mode.EXCLUDE)
    void consultarDetalle_publicacionNoActiva_otroUsuarioNoLaVe(EstadoPublicacion estado) {
        Publicacion publicacion = publicacion(estado);
        UUID id = publicacion.getId();
        Usuario otro = usuario("Hugo Huesped", RolUsuario.HUESPED);
        when(publicacionRepository.findById(id)).thenReturn(Optional.of(publicacion));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> busquedaService.consultarDetalle(id, otro));

        assertEquals("La publicación no está disponible.", ex.getMessage());
    }

    @Test
    void consultarDetalle_publicacionNoActiva_otroAnfitrionNoLaVe() {
        Publicacion publicacion = publicacion(EstadoPublicacion.BORRADOR);
        UUID id = publicacion.getId();
        Usuario otroAnfitrion = usuario("Otro Anfitrion", RolUsuario.ANFITRION);
        when(publicacionRepository.findById(id)).thenReturn(Optional.of(publicacion));

        assertThrows(IllegalStateException.class, () -> busquedaService.consultarDetalle(id, otroAnfitrion));
    }

    @Test
    void consultarDetalle_publicacionNoActiva_sinUsuarioNoLaVe() {
        Publicacion publicacion = publicacion(EstadoPublicacion.PAUSADA);
        UUID id = publicacion.getId();
        when(publicacionRepository.findById(id)).thenReturn(Optional.of(publicacion));

        assertThrows(IllegalStateException.class, () -> busquedaService.consultarDetalle(id, null));
    }

    @Test
    void consultarDetalle_publicacionNoActiva_suAnfitrionLaVe() {
        Publicacion publicacion = publicacion(EstadoPublicacion.PAUSADA);
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        PublicacionResponse response = busquedaService.consultarDetalle(publicacion.getId(), publicacion.getAnfitrion());

        assertEquals(publicacion.getId(), response.getId());
    }

    @Test
    void consultarDetalle_publicacionBloqueada_administradorLaVe() {
        Publicacion publicacion = publicacion(EstadoPublicacion.BLOQUEADA);
        Usuario admin = usuario("Admin", RolUsuario.ADMINISTRADOR);
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        PublicacionResponse response = busquedaService.consultarDetalle(publicacion.getId(), admin);

        assertEquals(publicacion.getId(), response.getId());
    }

    @Test
    void consultarDetalle_publicacionInexistente_lanzaExcepcion() {
        UUID id = UUID.randomUUID();
        Usuario huesped = usuario("Hugo Huesped", RolUsuario.HUESPED);
        when(publicacionRepository.findById(id)).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> busquedaService.consultarDetalle(id, huesped));

        assertEquals("Publicación no encontrada.", ex.getMessage());
    }

    // ---------- ubicación ----------

    @Test
    void obtenerUbicacion_publicacionNoActiva_otroUsuarioNoLaVeYNoGeocodifica() {
        Publicacion publicacion = publicacion(EstadoPublicacion.BLOQUEADA);
        UUID id = publicacion.getId();
        Usuario huesped = usuario("Hugo Huesped", RolUsuario.HUESPED);
        when(publicacionRepository.findById(id)).thenReturn(Optional.of(publicacion));

        assertThrows(IllegalStateException.class, () -> busquedaService.obtenerUbicacion(id, huesped));

        verify(geocodificacionService, never()).geocodificar(any());
        verify(publicacionRepository, never()).save(any());
    }

    @Test
    void obtenerUbicacion_publicacionNoActiva_suAnfitrionLaVe() {
        Publicacion publicacion = publicacion(EstadoPublicacion.BORRADOR);
        publicacion.setLatitud(10.4);
        publicacion.setLongitud(-75.5);
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        UbicacionResponse response = busquedaService.obtenerUbicacion(publicacion.getId(), publicacion.getAnfitrion());

        assertEquals(10.4, response.getLatitud());
        assertEquals(-75.5, response.getLongitud());
    }

    // ---------- helpers ----------

    private Publicacion publicacion(EstadoPublicacion estado) {
        Publicacion publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
        publicacion.setAnfitrion(usuario("Ana Anfitriona", RolUsuario.ANFITRION));
        publicacion.setTitulo("Publicación de prueba");
        publicacion.setDescripcion("Descripción de prueba");
        publicacion.setUbicacionTextual("Cartagena, Bolívar");
        publicacion.setCiudad("Cartagena");
        publicacion.setTipo(TipoAlojamiento.APARTAMENTO);
        publicacion.setCapacidad(2);
        publicacion.setPrecioNoche(new BigDecimal("100000"));
        publicacion.setEstado(estado);
        return publicacion;
    }

    private Usuario usuario(String nombre, RolUsuario rol) {
        Usuario usuario = new Usuario(nombre, nombre.replace(" ", "").toLowerCase() + "@correo.com", "hash", rol);
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        return usuario;
    }
}