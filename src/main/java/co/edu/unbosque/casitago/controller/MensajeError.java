package co.edu.unbosque.casitago.controller;

import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;

import java.sql.SQLException;

final class MensajeError {

    static final String GENERICO = "No se pudo completar la operación.";

    private static final Logger LOG = LoggerFactory.getLogger(MensajeError.class);
    private static final int PROFUNDIDAD_MAXIMA = 10;

    private MensajeError() {
    }

    // Los errores de negocio se devuelven tal cual; los de base de datos se
    // ocultan al cliente (nombres de tablas, restricciones, SQL) y se registran.
    static String de(RuntimeException e) {
        if (e == null) {
            return GENERICO;
        }
        Throwable actual = e;
        for (int i = 0; i < PROFUNDIDAD_MAXIMA && actual != null; i++) {
            if (actual instanceof DataAccessException
                    || actual instanceof SQLException
                    || actual instanceof PersistenceException) {
                LOG.error("Error de base de datos", e);
                return GENERICO;
            }
            actual = actual.getCause();
        }
        return e.getMessage();
    }
}