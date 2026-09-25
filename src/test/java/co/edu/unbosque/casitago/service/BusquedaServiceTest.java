package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.PublicacionResponse;
import co.edu.unbosque.casitago.dto.UbicacionResponse;
import co.edu.unbosque.casitago.entity.*;
import co.edu.unbosque.casitago.repository.PeriodoDisponibilidadRepository;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusquedaServiceTest {

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private PeriodoDisponibilidadRepository periodoRepository;

    @Mock
    private GeocodificacionService geocodificacionService;

    @InjectMocks
    private BusquedaService busquedaService;

    private final LocalDate desde = LocalDate.of(2026, 12, 1);
    private final LocalDate hasta = LocalDate.of(2026, 12, 5);

    // ---------- RF-15: buscar ----------

    @Test
    void buscar_sinFiltros_deberiaRetornarLoQueDevuelveElRepositorio() {
        Publicacion publicacion = publicacionExistente();
        when(publicacionRepository.buscarActivas(any(), any(), any(), any(), any(), any(), eq(0L)))
                .thenReturn(List.of(publicacion));

        List<PublicacionResponse> resultado = busquedaService.buscar(
                null, null, null, null, null, null, null, null);

        assertEquals(1, resultado.size());
        verify(periodoRepository, never()).buscarSolapados(any(), any(), any());
    }

    @Test
    void buscar_conServicios_deberiaMandarListaEnMinusculaYSuTamano() {
        when(publicacionRepository.buscarActivas(any(), any(), any(), any(), any(),
                eq(List.of("wifi", "piscina")), eq(2L))).thenReturn(List.of());

        busquedaService.buscar(null, null, null, null, null, null, null, List.of("WiFi", "Piscina"));

        verify(publicacionRepository).buscarActivas(any(), any(), any(), any(), any(),
                eq(List.of("wifi", "piscina")), eq(2L));
    }

    @Test
    void buscar_conTipoInvalido_deberiaLanzarExcepcion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> busquedaService.buscar(null, null, null, null, "CASTILLO", null, null, null));

        assertEquals("El tipo de alojamiento indicado no es válido.", ex.getMessage());
        verify(publicacionRepository, never()).buscarActivas(any(), any(), any(), any(), any(), any(), anyLong());
    }

    @Test
    void buscar_conSoloUnaFecha_deberiaLanzarExcepcion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> busquedaService.buscar(null, desde, null, null, null, null, null, null));

        assertEquals("Debes indicar ambas fechas (desde y hasta) o ninguna.", ex.getMessage());
    }

    @Test
    void buscar_conFechaFinAnteriorAInicio_deberiaLanzarExcepcion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> busquedaService.buscar(null, hasta, desde, null, null, null, null, null));

        assertEquals("La fecha de fin debe ser posterior a la fecha de inicio.", ex.getMessage());
    }

    @Test
    void buscar_conFechasYPeriodoSolapado_deberiaExcluirLaPublicacion() {
        Publicacion publicacion = publicacionExistente();
        when(publicacionRepository.buscarActivas(any(), any(), any(), any(), any(), any(), eq(0L)))
                .thenReturn(List.of(publicacion));
        when(periodoRepository.buscarSolapados(publicacion.getId(), desde, hasta))
                .thenReturn(List.of(new PeriodoDisponibilidad()));

        List<PublicacionResponse> resultado = busquedaService.buscar(
                null, desde, hasta, null, null, null, null, null);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void buscar_conFechasSinCruce_deberiaIncluirLaPublicacion() {
        Publicacion publicacion = publicacionExistente();
        when(publicacionRepository.buscarActivas(any(), any(), any(), any(), any(), any(), eq(0L)))
                .thenReturn(List.of(publicacion));
        when(periodoRepository.buscarSolapados(publicacion.getId(), desde, hasta)).thenReturn(List.of());

        List<PublicacionResponse> resultado = busquedaService.buscar(
                null, desde, hasta, null, null, null, null, null);

        assertEquals(1, resultado.size());
    }

    // ---------- RF-16: detalle ----------

    @Test
    void consultarDetalle_publicacionExistente_deberiaRetornarla() {
        Publicacion publicacion = publicacionExistente();
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        PublicacionResponse response = busquedaService.consultarDetalle(publicacion.getId());

        assertEquals(publicacion.getId(), response.getId());
    }

    @Test
    void consultarDetalle_publicacionInexistente_deberiaLanzarExcepcion() {
        UUID publicacionId = UUID.randomUUID();
        when(publicacionRepository.findById(publicacionId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> busquedaService.consultarDetalle(publicacionId));
    }

    // ---------- RF-30: ubicación ----------

    @Test
    void obtenerUbicacion_sinCoordenadasGuardadas_deberiaGeocodificarYGuardar() {
        Publicacion publicacion = publicacionExistente();
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(geocodificacionService.geocodificar(publicacion.getUbicacionTextual()))
                .thenReturn(new double[]{10.4, -75.5});
        when(publicacionRepository.save(publicacion)).thenReturn(publicacion);

        UbicacionResponse response = busquedaService.obtenerUbicacion(publicacion.getId());

        assertEquals(10.4, response.getLatitud());
        assertEquals(-75.5, response.getLongitud());
        verify(publicacionRepository).save(publicacion);
    }

    @Test
    void obtenerUbicacion_conCoordenadasYaGuardadas_noDeberiaGeocodificarDeNuevo() {
        Publicacion publicacion = publicacionExistente();
        publicacion.setLatitud(10.4);
        publicacion.setLongitud(-75.5);
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        UbicacionResponse response = busquedaService.obtenerUbicacion(publicacion.getId());

        assertEquals(10.4, response.getLatitud());
        verify(geocodificacionService, never()).geocodificar(any());
        verify(publicacionRepository, never()).save(any());
    }

    @Test
    void obtenerUbicacion_geocodificacionSinResultado_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionExistente();
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(geocodificacionService.geocodificar(any())).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> busquedaService.obtenerUbicacion(publicacion.getId()));

        assertEquals("No se pudo ubicar esta publicación en el mapa.", ex.getMessage());
    }

    // ---------- helpers ----------

    private Publicacion publicacionExistente() {
        Publicacion publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
        publicacion.setAnfitrion(anfitrionDePrueba());
        publicacion.setTitulo("Publicación existente");
        publicacion.setDescripcion("Descripción existente");
        publicacion.setUbicacionTextual("Cartagena, Bolívar");
        publicacion.setCiudad("Cartagena");
        publicacion.setTipo(TipoAlojamiento.APARTAMENTO);
        publicacion.setCapacidad(2);
        publicacion.setPrecioNoche(new BigDecimal("100000"));
        publicacion.setEstado(EstadoPublicacion.ACTIVA);
        return publicacion;
    }

    private Usuario anfitrionDePrueba() {
        Usuario anfitrion = new Usuario("Ana Anfitriona", "ana@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(anfitrion, "id", UUID.randomUUID());
        return anfitrion;
    }
}