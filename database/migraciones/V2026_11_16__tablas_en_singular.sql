-- =====================================================================
-- CasitaGO - Renombrar tablas de plural a singular
-- Ejecutar en Supabase: SQL Editor > New query > pegar > Run.
-- Todo va en una transacción: si una línea falla, no se cambia nada.
-- Las llaves foráneas, índices, restricciones y políticas RLS siguen
-- funcionando después del renombre (Postgres las asocia a la tabla, no al nombre).
-- =====================================================================

BEGIN;

-- IMPORTANTE: ejecutar justo cuando se despliegue el backend con las entidades
-- en singular. Con ddl-auto=validate, el backend viejo no arranca con estas
-- tablas y el nuevo no arranca sin ellas.

ALTER TABLE public.usuarios RENAME TO usuario;
ALTER TABLE public.publicaciones RENAME TO publicacion;
ALTER TABLE public.imagenes_publicacion RENAME TO imagen_publicacion;
ALTER TABLE public.reglas_alojamiento RENAME TO regla_alojamiento;
ALTER TABLE public.servicios_alojamiento RENAME TO servicio_alojamiento;
ALTER TABLE public.periodos_disponibilidad RENAME TO periodo_disponibilidad;
ALTER TABLE public.cotizaciones RENAME TO cotizacion;
ALTER TABLE public.reservas RENAME TO reserva;
ALTER TABLE public.cancelaciones RENAME TO cancelacion;
ALTER TABLE public.transacciones_pago RENAME TO transaccion_pago;
ALTER TABLE public.notificaciones RENAME TO notificacion;
ALTER TABLE public.resenas RENAME TO resena;
ALTER TABLE public.mensajes RENAME TO mensaje;
ALTER TABLE public.favoritos RENAME TO favorito;
ALTER TABLE public.restricciones_usuario RENAME TO restriccion_usuario;
ALTER TABLE public.codigos_recuperacion RENAME TO codigo_recuperacion;
ALTER TABLE public.eventos_auditoria RENAME TO evento_auditoria;

COMMIT;

-- Verificación: debe listar las 17 tablas con el nombre nuevo.
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
ORDER BY table_name;
