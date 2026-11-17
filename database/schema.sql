-- =====================================================================
-- CasitaGO - Esquema completo de la base de datos (PostgreSQL / Supabase)
--
-- Crea desde cero todo lo que necesita el backend: extensiones, tipos ENUM,
-- tablas, llaves, restricciones e índices. Coincide con las entidades JPA
-- (ddl-auto=validate) y con el Diccionario de Datos.
--
-- Cómo usarlo: Supabase > SQL Editor > New query > pegar > Run.
-- Es para una base VACÍA. En una base que ya tiene las tablas, no hace falta.
-- Va en una transacción: si algo falla, no se crea nada.
-- =====================================================================

BEGIN;

-- ---------- Extensiones ----------
-- btree_gist: permite combinar "=" (publicación) y "&&" (rango de fechas)
-- en la restricción de exclusión que impide reservas superpuestas.
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------- Tipos ENUM ----------
CREATE TYPE rol_usuario        AS ENUM ('HUESPED', 'ANFITRION', 'ADMINISTRADOR');
CREATE TYPE tipo_alojamiento   AS ENUM ('APARTAMENTO', 'CASA', 'HABITACION_PRIVADA', 'HABITACION_COMPARTIDA', 'OTRO');
CREATE TYPE estado_publicacion AS ENUM ('BORRADOR', 'PENDIENTE_REVISION', 'ACTIVA', 'PAUSADA', 'BLOQUEADA');
CREATE TYPE estado_periodo     AS ENUM ('DISPONIBLE', 'RESERVADO', 'BLOQUEADO');
CREATE TYPE estado_reserva     AS ENUM ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'COMPLETADA');
CREATE TYPE estado_pago        AS ENUM ('PENDIENTE', 'EXITOSO', 'FALLIDO', 'REEMBOLSADO');

-- =====================================================================
-- Usuarios y seguridad
-- =====================================================================

CREATE TABLE usuario (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre           VARCHAR(150) NOT NULL,
    correo           VARCHAR(150) NOT NULL,
    contrasena_hash  TEXT         NOT NULL,
    rol              rol_usuario  NOT NULL,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    mfa_habilitado   BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_usuario_correo UNIQUE (correo)
);

CREATE TABLE codigo_recuperacion (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    codigo      VARCHAR(10) NOT NULL,
    expira_en   TIMESTAMPTZ NOT NULL,
    usado       BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE TABLE restriccion_usuario (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID        NOT NULL REFERENCES usuario (id),
    motivo      TEXT        NOT NULL,
    creado_por  UUID        NOT NULL REFERENCES usuario (id),
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    activa      BOOLEAN     NOT NULL DEFAULT TRUE
);

-- =====================================================================
-- Publicaciones
-- =====================================================================

CREATE TABLE publicacion (
    id                 UUID               PRIMARY KEY DEFAULT gen_random_uuid(),
    anfitrion_id       UUID               NOT NULL REFERENCES usuario (id),
    titulo             VARCHAR(200)       NOT NULL,
    descripcion        TEXT               NOT NULL,
    ubicacion_textual  VARCHAR(250)       NOT NULL,
    ciudad             VARCHAR(100)       NOT NULL,
    tipo               tipo_alojamiento   NOT NULL,
    capacidad          INTEGER            NOT NULL,
    precio_noche       NUMERIC(12, 2)     NOT NULL,
    estado             estado_publicacion NOT NULL DEFAULT 'BORRADOR',
    latitud            DOUBLE PRECISION,
    longitud           DOUBLE PRECISION,
    creado_en          TIMESTAMPTZ        NOT NULL DEFAULT now(),
    actualizado_en     TIMESTAMPTZ        NOT NULL DEFAULT now(),
    CONSTRAINT ck_publicacion_capacidad    CHECK (capacidad > 0),
    CONSTRAINT ck_publicacion_precio_noche CHECK (precio_noche > 0)
);

CREATE TABLE servicio_alojamiento (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    publicacion_id  UUID         NOT NULL REFERENCES publicacion (id) ON DELETE CASCADE,
    nombre          VARCHAR(100) NOT NULL
);

CREATE TABLE regla_alojamiento (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    publicacion_id  UUID         NOT NULL REFERENCES publicacion (id) ON DELETE CASCADE,
    descripcion     VARCHAR(300) NOT NULL
);

CREATE TABLE imagen_publicacion (
    id              UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    publicacion_id  UUID    NOT NULL REFERENCES publicacion (id) ON DELETE CASCADE,
    url             TEXT    NOT NULL,
    orden           INTEGER NOT NULL DEFAULT 0
);

-- =====================================================================
-- Disponibilidad, cotización y reservas
-- =====================================================================

CREATE TABLE periodo_disponibilidad (
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    publicacion_id  UUID           NOT NULL REFERENCES publicacion (id),
    fecha_inicio    DATE           NOT NULL,
    fecha_fin       DATE           NOT NULL,
    estado          estado_periodo NOT NULL DEFAULT 'BLOQUEADO',
    CONSTRAINT ck_periodo_fechas CHECK (fecha_fin > fecha_inicio)
);

CREATE TABLE cotizacion (
    id                     UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    publicacion_id         UUID           NOT NULL REFERENCES publicacion (id),
    noches                 INTEGER        NOT NULL,
    precio_base            NUMERIC(12, 2) NOT NULL,
    tarifa_limpieza        NUMERIC(12, 2) NOT NULL DEFAULT 0,
    tarifa_servicio        NUMERIC(12, 2) NOT NULL DEFAULT 0,
    servicios_adicionales  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total                  NUMERIC(12, 2) NOT NULL,
    creado_en              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_cotizacion_noches CHECK (noches > 0)
);

CREATE TABLE reserva (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    huesped_id       UUID           NOT NULL REFERENCES usuario (id),
    publicacion_id   UUID           NOT NULL REFERENCES publicacion (id),
    cotizacion_id    UUID           NOT NULL REFERENCES cotizacion (id),
    fecha_llegada    DATE           NOT NULL,
    fecha_salida     DATE           NOT NULL,
    estado           estado_reserva NOT NULL DEFAULT 'PENDIENTE',
    idempotency_key  VARCHAR(100)   NOT NULL,
    -- Rango [llegada, salida): la salida de una reserva puede ser la llegada de la siguiente.
    rango_fechas     DATERANGE      GENERATED ALWAYS AS (daterange(fecha_llegada, fecha_salida, '[)')) STORED,
    creado_en        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_reserva_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_reserva_fechas CHECK (fecha_salida > fecha_llegada),
    -- Regla de negocio: no puede haber dos reservas activas superpuestas en el mismo
    -- alojamiento. La base de datos la hace cumplir aunque lleguen dos peticiones a la vez.
    CONSTRAINT no_solapamiento EXCLUDE USING gist (
        publicacion_id WITH =,
        rango_fechas   WITH &&
    ) WHERE (estado IN ('PENDIENTE', 'CONFIRMADA'))
);

CREATE TABLE cancelacion (
    id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    reserva_id        UUID           NOT NULL REFERENCES reserva (id),
    usuario_id        UUID           NOT NULL REFERENCES usuario (id),
    motivo            TEXT           NOT NULL,
    valor_devolucion  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    estado_final      estado_reserva NOT NULL,
    creado_en         TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE TABLE transaccion_pago (
    id                  UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    reserva_id          UUID           NOT NULL REFERENCES reserva (id),
    proveedor           VARCHAR(50)    NOT NULL DEFAULT 'stripe_test',
    estado              estado_pago    NOT NULL DEFAULT 'PENDIENTE',
    monto               NUMERIC(12, 2) NOT NULL,
    referencia_externa  VARCHAR(150),
    creado_en           TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- =====================================================================
-- Reseñas, notificaciones, mensajes y favoritos
-- =====================================================================

CREATE TABLE resena (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    huesped_id      UUID        NOT NULL REFERENCES usuario (id),
    reserva_id      UUID        NOT NULL REFERENCES reserva (id),
    publicacion_id  UUID        NOT NULL REFERENCES publicacion (id),
    calificacion    INTEGER     NOT NULL,
    comentario      TEXT,
    oculta          BOOLEAN     NOT NULL DEFAULT FALSE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_resena_calificacion CHECK (calificacion BETWEEN 1 AND 5)
);

CREATE TABLE notificacion (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id   UUID         NOT NULL REFERENCES usuario (id),
    tipo_evento  VARCHAR(100) NOT NULL,
    mensaje      TEXT         NOT NULL,
    leida        BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE mensaje (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    publicacion_id  UUID        NOT NULL REFERENCES publicacion (id),
    huesped_id      UUID        NOT NULL REFERENCES usuario (id),
    emisor_id       UUID        NOT NULL REFERENCES usuario (id),
    contenido       TEXT        NOT NULL,
    leido           BOOLEAN     NOT NULL DEFAULT FALSE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE favorito (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id      UUID        NOT NULL REFERENCES usuario (id),
    publicacion_id  UUID        NOT NULL REFERENCES publicacion (id),
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_favorito_usuario_publicacion UNIQUE (usuario_id, publicacion_id)
);

-- =====================================================================
-- Auditoría
-- =====================================================================

CREATE TABLE evento_auditoria (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID         REFERENCES usuario (id),   -- nulo en eventos del sistema
    entidad     VARCHAR(100) NOT NULL,
    accion      VARCHAR(100) NOT NULL,
    resultado   VARCHAR(50)  NOT NULL,
    detalle     JSONB,
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- =====================================================================
-- Índices para las consultas más frecuentes
-- =====================================================================

CREATE INDEX idx_codigo_recuperacion_usuario ON codigo_recuperacion (usuario_id);
CREATE INDEX idx_restriccion_usuario_usuario ON restriccion_usuario (usuario_id) WHERE activa;
CREATE INDEX idx_publicacion_anfitrion       ON publicacion (anfitrion_id);
CREATE INDEX idx_publicacion_busqueda        ON publicacion (estado, ciudad);
CREATE INDEX idx_servicio_publicacion        ON servicio_alojamiento (publicacion_id);
CREATE INDEX idx_regla_publicacion           ON regla_alojamiento (publicacion_id);
CREATE INDEX idx_imagen_publicacion          ON imagen_publicacion (publicacion_id);
CREATE INDEX idx_periodo_publicacion_fechas  ON periodo_disponibilidad (publicacion_id, fecha_inicio, fecha_fin);
CREATE INDEX idx_reserva_huesped             ON reserva (huesped_id);
CREATE INDEX idx_reserva_estado              ON reserva (estado);
CREATE INDEX idx_cancelacion_reserva         ON cancelacion (reserva_id);
CREATE INDEX idx_transaccion_reserva         ON transaccion_pago (reserva_id);
CREATE INDEX idx_resena_publicacion          ON resena (publicacion_id);
CREATE INDEX idx_resena_reserva              ON resena (reserva_id);
CREATE INDEX idx_notificacion_usuario        ON notificacion (usuario_id, creado_en DESC);
CREATE INDEX idx_mensaje_conversacion        ON mensaje (publicacion_id, huesped_id, creado_en);
CREATE INDEX idx_mensaje_emisor              ON mensaje (emisor_id);
CREATE INDEX idx_favorito_publicacion        ON favorito (publicacion_id);
CREATE INDEX idx_evento_auditoria_filtros    ON evento_auditoria (entidad, accion, creado_en DESC);
CREATE INDEX idx_evento_auditoria_usuario    ON evento_auditoria (usuario_id);

-- =====================================================================
-- Seguridad: Row Level Security
-- Supabase expone las tablas de "public" por su API REST con la llave pública
-- (anon). Activar RLS sin políticas cierra ese acceso: solo el backend las usa.
-- El backend se conecta como el dueño de las tablas (postgres), que no está
-- sujeto a RLS, así que sigue funcionando igual.
-- =====================================================================

ALTER TABLE usuario                ENABLE ROW LEVEL SECURITY;
ALTER TABLE codigo_recuperacion    ENABLE ROW LEVEL SECURITY;
ALTER TABLE restriccion_usuario    ENABLE ROW LEVEL SECURITY;
ALTER TABLE publicacion            ENABLE ROW LEVEL SECURITY;
ALTER TABLE servicio_alojamiento   ENABLE ROW LEVEL SECURITY;
ALTER TABLE regla_alojamiento      ENABLE ROW LEVEL SECURITY;
ALTER TABLE imagen_publicacion     ENABLE ROW LEVEL SECURITY;
ALTER TABLE periodo_disponibilidad ENABLE ROW LEVEL SECURITY;
ALTER TABLE cotizacion             ENABLE ROW LEVEL SECURITY;
ALTER TABLE reserva                ENABLE ROW LEVEL SECURITY;
ALTER TABLE cancelacion            ENABLE ROW LEVEL SECURITY;
ALTER TABLE transaccion_pago       ENABLE ROW LEVEL SECURITY;
ALTER TABLE resena                 ENABLE ROW LEVEL SECURITY;
ALTER TABLE notificacion           ENABLE ROW LEVEL SECURITY;
ALTER TABLE mensaje                ENABLE ROW LEVEL SECURITY;
ALTER TABLE favorito               ENABLE ROW LEVEL SECURITY;
ALTER TABLE evento_auditoria       ENABLE ROW LEVEL SECURITY;

COMMIT;
