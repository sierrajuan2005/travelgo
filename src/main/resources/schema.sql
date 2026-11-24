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
    id                    VARCHAR(36)  PRIMARY KEY,
    nombre                VARCHAR(100) NOT NULL,
    correo                VARCHAR(150) NOT NULL UNIQUE,
    password              VARCHAR(255) NOT NULL, -- hash BCrypt (60 caracteres)
    rol                   VARCHAR(20)  NOT NULL
                          CHECK (rol IN ('CLIENTE', 'AGENTE', 'ADMINISTRADOR')),
    telefono              VARCHAR(30),
    notificaciones_correo BOOLEAN     NOT NULL DEFAULT FALSE,
    notificaciones_sms    BOOLEAN     NOT NULL DEFAULT FALSE
);

-- Compatibilidad con bases de datos existentes
ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS telefono VARCHAR(30),
    ADD COLUMN IF NOT EXISTS notificaciones_correo BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS notificaciones_sms BOOLEAN NOT NULL DEFAULT FALSE;


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


CREATE TABLE IF NOT EXISTS servicio_reservable (
    id              VARCHAR(36)   PRIMARY KEY,
    precio          NUMERIC(10,2) NOT NULL CHECK (precio >= 0),
    disponible      BOOLEAN       NOT NULL DEFAULT TRUE,
    tipo            VARCHAR(30)   NOT NULL CHECK (tipo IN ('VUELO', 'HOTEL', 'TRANSPORTE')),
    fuente          VARCHAR(100),
    id_externo      VARCHAR(255),
    fecha_consulta  TIMESTAMP,
    enlace_reserva  TEXT
);

-- Compatibilidad con bases de datos existentes
ALTER TABLE servicio_reservable
    ADD COLUMN IF NOT EXISTS fuente VARCHAR(100),
    ADD COLUMN IF NOT EXISTS id_externo VARCHAR(255),
    ADD COLUMN IF NOT EXISTS fecha_consulta TIMESTAMP,
    ADD COLUMN IF NOT EXISTS enlace_reserva TEXT;


-- Servicios (vuelos, hoteles, transportes) que componen un paquete.
-- Va después de paquete_turistico y servicio_reservable porque referencia a las dos.
CREATE TABLE IF NOT EXISTS paquete_servicio (
    paquete_id  BIGINT      NOT NULL REFERENCES paquete_turistico (id) ON DELETE CASCADE,
    servicio_id VARCHAR(36) NOT NULL REFERENCES servicio_reservable (id),
    PRIMARY KEY (paquete_id, servicio_id)
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
    noches          INTEGER      NOT NULL CHECK (noches > 0),
    fecha_entrada   DATE
);

ALTER TABLE hotel
    ADD COLUMN IF NOT EXISTS fecha_entrada DATE;

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

-- ---------- Cupones de descuento ----------
-- El cupón va antes que la factura porque la factura lo referencia (cupon_id).
CREATE TABLE IF NOT EXISTS cupon (
    id            VARCHAR(36)   PRIMARY KEY,
    codigo        VARCHAR(30)   NOT NULL UNIQUE,
    tipo          VARCHAR(20)   NOT NULL CHECK (tipo IN ('PORCENTAJE', 'VALOR_FIJO')),
    valor         NUMERIC(10,2) NOT NULL CHECK (valor > 0),
    fecha_inicio  DATE          NOT NULL,
    fecha_fin     DATE          NOT NULL,
    usos_maximos  INTEGER       NOT NULL CHECK (usos_maximos >= 1),
    usos_actuales INTEGER       NOT NULL DEFAULT 0 CHECK (usos_actuales >= 0),
    activo        BOOLEAN       NOT NULL DEFAULT TRUE,
    CHECK (fecha_fin >= fecha_inicio)
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
    reserva_id    VARCHAR(36)   NOT NULL UNIQUE REFERENCES reserva (id),  -- una reserva, una factura
    descuento     NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (descuento >= 0),
    cupon_id      VARCHAR(36)   REFERENCES cupon (id)                     -- cupón aplicado (opcional)
);

-- Compatibilidad con bases de datos existentes (CREATE TABLE IF NOT EXISTS no agrega columnas nuevas)
ALTER TABLE factura ADD COLUMN IF NOT EXISTS descuento NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (descuento >= 0);
ALTER TABLE factura ADD COLUMN IF NOT EXISTS cupon_id  VARCHAR(36) REFERENCES cupon (id);

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

-- =====================================================================
-- Sprint 4 - Integrante 4: índice para buscar servicios por su referencia externa
-- =====================================================================
CREATE INDEX IF NOT EXISTS idx_servicio_fuente_externo ON servicio_reservable (fuente, id_externo);

-- Pendiente de decisión del equipo (moneda): si se guarda la moneda original de la API, agregar
--   ALTER TABLE servicio_reservable ADD COLUMN IF NOT EXISTS moneda VARCHAR(3);
--   ALTER TABLE servicio_reservable ADD COLUMN IF NOT EXISTS precio_original NUMERIC(12,2);
-- y que Integrante 1 las lea/escriba en ServicioReservableDAOJdbc.

-- =====================================================================
-- Sprint 5 - Integrante 4: tablas compartidas
-- Cada integrante implementa su módulo sobre estas estructuras:
--   chat_sesion y mensaje ....... chatbot (Integrante 4)
--   envio_recordatorio .......... recordatorios por correo y SMS (Integrante 3)
--   cache_mapa .................. lugares del mapa (Integrante 2)
-- =====================================================================

-- ---------- Chatbot ----------
CREATE TABLE IF NOT EXISTS chat_sesion (
    id               VARCHAR(36) PRIMARY KEY,
    usuario_id       VARCHAR(36) REFERENCES usuario (id),   -- NULL si quien escribió no inició sesión
    canal            VARCHAR(20) NOT NULL CHECK (canal IN ('WEB', 'TELEGRAM')),
    creada_en        TIMESTAMP   NOT NULL,
    ultima_actividad TIMESTAMP   NOT NULL
);

CREATE TABLE IF NOT EXISTS mensaje (
    id        VARCHAR(36) PRIMARY KEY,
    sesion_id VARCHAR(36) NOT NULL REFERENCES chat_sesion (id) ON DELETE CASCADE,
    emisor    VARCHAR(10) NOT NULL CHECK (emisor IN ('USUARIO', 'BOT')),
    texto     TEXT        NOT NULL,
    intencion VARCHAR(40),
    fecha     TIMESTAMP   NOT NULL
);

-- ---------- Recordatorios ----------
-- Un registro por aviso enviado; la restricción UNIQUE evita repetir el mismo aviso de una reserva.
CREATE TABLE IF NOT EXISTS envio_recordatorio (
    id          VARCHAR(36) PRIMARY KEY,
    usuario_id  VARCHAR(36) NOT NULL REFERENCES usuario (id),
    reserva_id  VARCHAR(36) NOT NULL REFERENCES reserva (id),
    tipo        VARCHAR(30) NOT NULL
                CHECK (tipo IN ('VIAJE_7_DIAS', 'VIAJE_1_DIA', 'VIAJE_HORAS', 'PAGO_PENDIENTE', 'PAGO_CONFIRMADO')),
    canal       VARCHAR(10) NOT NULL CHECK (canal IN ('CORREO', 'SMS')),
    estado      VARCHAR(10) NOT NULL CHECK (estado IN ('ENVIADO', 'FALLIDO')),
    fecha_envio TIMESTAMP   NOT NULL,
    UNIQUE (reserva_id, tipo, canal)
);

-- ---------- Mapa ----------
-- Caché de lugares por ciudad, categoría y fuente (Geoapify, Xotelo...). Se reutiliza hasta que expira.
CREATE TABLE IF NOT EXISTS cache_mapa (
    id             VARCHAR(36)  PRIMARY KEY,
    ciudad         VARCHAR(100) NOT NULL,
    categoria      VARCHAR(50)  NOT NULL,
    fuente         VARCHAR(50)  NOT NULL,
    respuesta_json TEXT         NOT NULL,
    fecha_consulta TIMESTAMP    NOT NULL,
    expira_en      TIMESTAMP    NOT NULL,
    UNIQUE (ciudad, categoria, fuente)
);

CREATE INDEX IF NOT EXISTS idx_chat_sesion_usuario ON chat_sesion (usuario_id);
CREATE INDEX IF NOT EXISTS idx_mensaje_sesion      ON mensaje (sesion_id, fecha);
CREATE INDEX IF NOT EXISTS idx_envio_reserva       ON envio_recordatorio (reserva_id);

-- =====================================================================
-- ExpoSoftware - Integrante 3: "Lanza tu viaje"
-- El visitante escribe su nombre en el celular, elige un destino y su avión sale de Valledupar en la
-- pantalla del stand. destino_id es la clave de CatalogoDestinosSonados (lista fija en Java con
-- coordenadas), no de la tabla destino: se puede soñar con Tokio aunque no haya paquetes allí.
-- oculto = TRUE cuando un administrador lo quita de la pantalla (/admin/expo).
-- =====================================================================
CREATE TABLE IF NOT EXISTS lanzamiento_expo (
    id         BIGSERIAL   PRIMARY KEY,
    nombre     VARCHAR(20) NOT NULL,
    destino_id VARCHAR(30) NOT NULL,
    fecha      TIMESTAMP   NOT NULL,
    oculto     BOOLEAN     NOT NULL DEFAULT FALSE
);

-- Recorrido del visitante después de lanzar (embudo del stand en /admin/expo):
--   telefono/fecha_contacto: celular que dejó CON autorización (Ley 1581 de 2012) para su postal y ofertas.
--   vio_destino: abrió la página de su destino. abrio_paquete: abrió un paquete desde esa página.
-- ADD COLUMN IF NOT EXISTS para las bases que ya tenían la tabla.
ALTER TABLE lanzamiento_expo ADD COLUMN IF NOT EXISTS telefono       VARCHAR(16);
ALTER TABLE lanzamiento_expo ADD COLUMN IF NOT EXISTS fecha_contacto TIMESTAMP;
ALTER TABLE lanzamiento_expo ADD COLUMN IF NOT EXISTS vio_destino    BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE lanzamiento_expo ADD COLUMN IF NOT EXISTS abrio_paquete  BOOLEAN NOT NULL DEFAULT FALSE;
-- WhatsApp enviado al visitante: id del mensaje en Twilio, estado (ENVIANDO, ENTREGADO, LEIDO, SIN_ACTIVAR, FALLIDO).
ALTER TABLE lanzamiento_expo ADD COLUMN IF NOT EXISTS whatsapp_id     VARCHAR(64);
ALTER TABLE lanzamiento_expo ADD COLUMN IF NOT EXISTS whatsapp_estado VARCHAR(20);
ALTER TABLE lanzamiento_expo ADD COLUMN IF NOT EXISTS whatsapp_fecha  TIMESTAMP;
-- El visitante compartió o descargó su postal (embudo del stand).
ALTER TABLE lanzamiento_expo ADD COLUMN IF NOT EXISTS compartio_postal BOOLEAN NOT NULL DEFAULT FALSE;
-- Viaje de quien le compartió la postal ("se unió al viaje de Jaime"); NULL si llegó por el QR del stand.
ALTER TABLE lanzamiento_expo ADD COLUMN IF NOT EXISTS invitado_por BIGINT REFERENCES lanzamiento_expo (id);
