-- =====================================================================
-- TravelGo - schema.sql (PostgreSQL)
-- Sprint 3 - Integrante 4. Base para la migración a JDBC (Integrante 2).
--
-- Se sigue docs/Diagramas/ModeloER.md con estos ajustes para que cuadre
-- con el código que hoy está en develop:
--   1) destino, paquete_turistico y paquete_destino usan BIGSERIAL porque
--      sus entidades JPA tienen id Long (el ER decía varchar(36)). Los
--      nombres de tabla y columna son los que genera Hibernate.
--   2) Pago y Factura siguen el ER y las clases actuales del dominio:
--        Factura.reserva -> factura.reserva_id (UNIQUE: una reserva, una factura)
--        Pago.factura    -> pago.factura_id    (el pago pertenece a una factura)
--      La factura se crea ANTES que el pago (el pago la referencia), por eso
--      factura va primero en este archivo.
--   3) Factura guarda subtotal y estado (PENDIENTE, PAGADA o ANULADA).
--      Pago guarda estado (APROBADO o RECHAZADO).
--   4) Pago.fechaPago es DATE (el dominio usa LocalDate).
--   5) El estado de la reserva se valida con CHECK contra el enum EstadoReserva.
--
-- Es idempotente (IF NOT EXISTS): se puede ejecutar varias veces.
-- Para que Spring Boot lo ejecute con PostgreSQL hay que agregar en
-- application.properties:   spring.sql.init.mode=always
-- y, para que Hibernate no cree tablas por su cuenta:
--                           spring.jpa.hibernate.ddl-auto=validate (o none)
-- =====================================================================

-- ---------- Usuarios ----------
CREATE TABLE IF NOT EXISTS usuario (
    id         VARCHAR(36)  PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    correo     VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,           -- hash BCrypt (60 caracteres)
    rol        VARCHAR(20)  NOT NULL
               CHECK (rol IN ('CLIENTE', 'AGENTE', 'ADMINISTRADOR'))
);

-- ---------- Catálogo (entidades JPA, id Long) ----------
CREATE TABLE IF NOT EXISTS destino (
    id          BIGSERIAL    PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    pais        VARCHAR(80)  NOT NULL,
    ciudad      VARCHAR(80)  NOT NULL
);

CREATE TABLE IF NOT EXISTS paquete_turistico (
    id            BIGSERIAL     PRIMARY KEY,
    nombre        VARCHAR(100)  NOT NULL,
    descripcion   VARCHAR(500),
    precio        NUMERIC(12,2) NOT NULL CHECK (precio > 0),
    duracion_dias INTEGER       NOT NULL CHECK (duracion_dias >= 1),
    activo        BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS paquete_destino (
    paquete_id BIGINT NOT NULL REFERENCES paquete_turistico (id) ON DELETE CASCADE,
    destino_id BIGINT NOT NULL REFERENCES destino (id),
    PRIMARY KEY (paquete_id, destino_id)
);

-- ---------- Servicios reservables (herencia: tabla base + una por tipo) ----------
CREATE TABLE IF NOT EXISTS servicio_reservable (
    id         VARCHAR(36)   PRIMARY KEY,
    precio     NUMERIC(10,2) NOT NULL CHECK (precio >= 0),
    disponible BOOLEAN       NOT NULL DEFAULT TRUE,
    tipo       VARCHAR(30)   NOT NULL CHECK (tipo IN ('VUELO', 'HOTEL', 'TRANSPORTE'))
);

CREATE TABLE IF NOT EXISTS vuelo (
    id            VARCHAR(36)  PRIMARY KEY REFERENCES servicio_reservable (id) ON DELETE CASCADE,
    aerolinea     VARCHAR(100) NOT NULL,
    origen        VARCHAR(100) NOT NULL,
    destino       VARCHAR(100) NOT NULL,
    fecha_salida  TIMESTAMP    NOT NULL,
    fecha_llegada TIMESTAMP    NOT NULL,
    CHECK (fecha_llegada > fecha_salida),
    CHECK (lower(origen) <> lower(destino))
);

CREATE TABLE IF NOT EXISTS hotel (
    id              VARCHAR(36)  PRIMARY KEY REFERENCES servicio_reservable (id) ON DELETE CASCADE,
    nombre          VARCHAR(150) NOT NULL,
    direccion       VARCHAR(200) NOT NULL,
    tipo_habitacion VARCHAR(100) NOT NULL,
    noches          INTEGER      NOT NULL CHECK (noches > 0)
);

CREATE TABLE IF NOT EXISTS transporte (
    id      VARCHAR(36)  PRIMARY KEY REFERENCES servicio_reservable (id) ON DELETE CASCADE,
    tipo    VARCHAR(100) NOT NULL,
    origen  VARCHAR(100) NOT NULL,
    destino VARCHAR(100) NOT NULL,
    fecha   TIMESTAMP    NOT NULL,
    CHECK (lower(origen) <> lower(destino))
);

-- ---------- Reservas ----------
CREATE TABLE IF NOT EXISTS reserva (
    id            VARCHAR(36)   PRIMARY KEY,
    fecha_reserva DATE          NOT NULL,
    estado        VARCHAR(30)   NOT NULL
                  CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA')),
    total         NUMERIC(10,2) NOT NULL CHECK (total >= 0),
    usuario_id    VARCHAR(36)   NOT NULL REFERENCES usuario (id),
    paquete_id    BIGINT        REFERENCES paquete_turistico (id)
);

CREATE TABLE IF NOT EXISTS reserva_servicio (
    reserva_id  VARCHAR(36) NOT NULL REFERENCES reserva (id) ON DELETE CASCADE,
    servicio_id VARCHAR(36) NOT NULL REFERENCES servicio_reservable (id),
    PRIMARY KEY (reserva_id, servicio_id)
);

-- ---------- Facturas y pagos ----------
-- La factura va antes que el pago porque el pago la referencia.
CREATE TABLE IF NOT EXISTS factura (
    id            VARCHAR(36)   PRIMARY KEY,
    numero        VARCHAR(50)   NOT NULL UNIQUE,
    fecha_emision DATE          NOT NULL,
    subtotal      NUMERIC(10,2) NOT NULL CHECK (subtotal > 0),
    total         NUMERIC(10,2) NOT NULL CHECK (total > 0),
    estado        VARCHAR(30)   NOT NULL
                  CHECK (estado IN ('PENDIENTE', 'PAGADA', 'ANULADA')),
    reserva_id    VARCHAR(36)   NOT NULL UNIQUE REFERENCES reserva (id)  -- una reserva, una factura
);

CREATE TABLE IF NOT EXISTS pago (
    id         VARCHAR(36)   PRIMARY KEY,
    monto      NUMERIC(10,2) NOT NULL CHECK (monto > 0),
    fecha_pago DATE          NOT NULL,
    metodo     VARCHAR(50)   NOT NULL
               CHECK (metodo IN ('TARJETA_CREDITO', 'TARJETA_DEBITO', 'TRANSFERENCIA', 'EFECTIVO')),
    estado     VARCHAR(30)   NOT NULL
               CHECK (estado IN ('APROBADO', 'RECHAZADO')),
    factura_id VARCHAR(36)   NOT NULL REFERENCES factura (id)
);

-- ---------- Índices para las consultas de los DAO ----------
CREATE INDEX IF NOT EXISTS idx_reserva_usuario       ON reserva (usuario_id);
CREATE INDEX IF NOT EXISTS idx_servicio_disponible   ON servicio_reservable (disponible);
CREATE INDEX IF NOT EXISTS idx_paquete_activo        ON paquete_turistico (activo);
