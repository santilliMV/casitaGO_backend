package co.edu.unbosque.casitago.entity;

/**
 * Roles del dominio CasitaGO.
 * Debe coincidir EXACTAMENTE (nombre y orden no importa, pero los valores sí)
 * con el tipo enum de Postgres `rol_usuario` definido en database/schema.sql:
 *   CREATE TYPE rol_usuario AS ENUM ('HUESPED', 'ANFITRION', 'ADMINISTRADOR');
 */
public enum RolUsuario {
    HUESPED,
    ANFITRION,
    ADMINISTRADOR
}
