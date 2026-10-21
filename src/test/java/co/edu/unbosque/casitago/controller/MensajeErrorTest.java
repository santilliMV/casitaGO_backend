package co.edu.unbosque.casitago.controller;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MensajeErrorTest {

    @Test
    void de_errorDeNegocio_devuelveElMensajeTalCual() {
        RuntimeException e = new IllegalStateException("La publicación no está disponible.");

        assertEquals("La publicación no está disponible.", MensajeError.de(e));
    }

    @Test
    void de_errorSinMensaje_devuelveNull() {
        RuntimeException e = new IllegalArgumentException();

        assertNull(MensajeError.de(e));
    }

    @Test
    void de_errorDeAccesoADatos_ocultaElDetalle() {
        RuntimeException e = new DataIntegrityViolationException(
                "could not execute statement; constraint [no_solapamiento]");

        assertEquals(MensajeError.GENERICO, MensajeError.de(e));
    }

    @Test
    void de_errorDePersistencia_ocultaElDetalle() {
        RuntimeException e = new PersistenceException("select * from usuarios");

        assertEquals(MensajeError.GENERICO, MensajeError.de(e));
    }

    @Test
    void de_sqlExceptionComoCausa_ocultaElDetalle() {
        RuntimeException e = new RuntimeException("Fallo al guardar", new SQLException("duplicate key value"));

        assertEquals(MensajeError.GENERICO, MensajeError.de(e));
    }

    @Test
    void de_errorDeBaseDeDatosEnCausaProfunda_ocultaElDetalle() {
        RuntimeException e = new RuntimeException("nivel 1",
                new RuntimeException("nivel 2", new SQLException("relation \"usuarios\" does not exist")));

        assertEquals(MensajeError.GENERICO, MensajeError.de(e));
    }

    @Test
    void de_causaDeBaseDeDatosDemasiadoLejos_noSeBusca() {
        Throwable causa = new SQLException("detalle interno");
        for (int i = 0; i < 12; i++) {
            causa = new RuntimeException("nivel " + i, causa);
        }
        RuntimeException e = new RuntimeException("mensaje de negocio", causa);

        assertEquals("mensaje de negocio", MensajeError.de(e));
    }

    @Test
    void de_errorDeNegocioConCausaQueNoEsDeBaseDeDatos_devuelveElMensaje() {
        RuntimeException e = new RuntimeException("Pago rechazado", new IllegalStateException("tarjeta"));

        assertEquals("Pago rechazado", MensajeError.de(e));
    }
}