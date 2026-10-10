package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.EstadoPublicacion;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.TipoAlojamiento;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class PublicacionRepositoryBusquedaTest {

    @Test
    void buscarActivas_sinTipo_noFiltraPorTipoYUsaValorDeRelleno() {
        PublicacionRepository repositorio = mock(PublicacionRepository.class, CALLS_REAL_METHODS);
        List<Publicacion> esperado = List.of(new Publicacion());
        doReturn(esperado).when(repositorio).buscarPorEstado(
                eq(EstadoPublicacion.ACTIVA), any(), eq(false), eq(TipoAlojamiento.OTRO),
                any(), any(), any(), anyList(), anyLong());

        List<Publicacion> resultado = repositorio.buscarActivas("Bogotá", null, null, null, null, List.of(), 0L);

        assertThat(resultado).isSameAs(esperado);
    }

    @Test
    void buscarActivas_conTipo_filtraPorEseTipo() {
        PublicacionRepository repositorio = mock(PublicacionRepository.class, CALLS_REAL_METHODS);
        List<Publicacion> esperado = List.of(new Publicacion());
        doReturn(esperado).when(repositorio).buscarPorEstado(
                eq(EstadoPublicacion.ACTIVA), any(), eq(true), eq(TipoAlojamiento.CASA),
                any(), any(), any(), anyList(), anyLong());

        List<Publicacion> resultado = repositorio.buscarActivas(null, TipoAlojamiento.CASA, null, null, null, List.of(), 0L);

        assertThat(resultado).isSameAs(esperado);
    }
}