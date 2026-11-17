-- =====================================================================
-- CasitaGO - REVERTIR el renombre (singular -> plural)
-- Ejecutar en Supabase: SQL Editor > New query > pegar > Run.
-- Todo va en una transacción: si una línea falla, no se cambia nada.
-- Las llaves foráneas, índices, restricciones y políticas RLS siguen
-- funcionando después del renombre (Postgres las asocia a la tabla, no al nombre).
-- =====================================================================

BEGIN;

-- Solo usar si hay que volver al backend anterior (tablas en plural).

ALTER TABLE public.usuario RENAME TO usuarios;
ALTER TABLE public.publicacion RENAME TO publicaciones;
ALTER TABLE public.imagen_publicacion RENAME TO imagenes_publicacion;
ALTER TABLE public.regla_alojamiento RENAME TO reglas_alojamiento;
ALTER TABLE public.servicio_alojamiento RENAME TO servicios_alojamiento;
ALTER TABLE public.periodo_disponibilidad RENAME TO periodos_disponibilidad;
ALTER TABLE public.cotizacion RENAME TO cotizaciones;
ALTER TABLE public.reserva RENAME TO reservas;
ALTER TABLE public.cancelacion RENAME TO cancelaciones;
ALTER TABLE public.transaccion_pago RENAME TO transacciones_pago;
ALTER TABLE public.notificacion RENAME TO notificaciones;
ALTER TABLE public.resena RENAME TO resenas;
ALTER TABLE public.mensaje RENAME TO mensajes;
ALTER TABLE public.favorito RENAME TO favoritos;
ALTER TABLE public.restriccion_usuario RENAME TO restricciones_usuario;
ALTER TABLE public.codigo_recuperacion RENAME TO codigos_recuperacion;
ALTER TABLE public.evento_auditoria RENAME TO eventos_auditoria;

COMMIT;

-- Verificación: debe listar las 17 tablas con el nombre nuevo.
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
ORDER BY table_name;
