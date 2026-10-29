-- =====================================================================
-- TravelGo - data.sql (datos de prueba / seeds)
-- Sprint 3 - Integrante 4. Requiere schema.sql ejecutado antes.
--
-- Es idempotente: se puede correr varias veces sin duplicar datos.
--
-- USUARIOS DE PRUEBA (la contraseña va cifrada con BCrypt, igual que el registro):
--   admin@travelgo.com     Admin123*      ADMINISTRADOR
--   agente@travelgo.com    Agente123*     AGENTE
--   cliente@travelgo.com   Cliente123*    CLIENTE
--   cliente2@travelgo.com  Cliente123*    CLIENTE   (sirve para probar "reserva ajena")
--
-- RESERVAS DE EJEMPLO:
--   r-pendiente   cliente   PENDIENTE  vuelo + hotel   total 1.050.000  (para probar el pago)
--   r-pagada      cliente   CONFIRMADA vuelo           total   450.000  (ya tiene pago y factura)
--   r-cancelada   cliente   CANCELADA  hotel           total   600.000  (no se puede pagar)
--   r-vacia       cliente2  PENDIENTE  sin servicios   total         0  (no se puede confirmar ni pagar)
-- =====================================================================

-- ---------- Usuarios ----------
INSERT INTO usuario (id, nombre, correo, password, rol) VALUES
 ('u-admin',    'Administrador TravelGo', 'admin@travelgo.com',    '$2a$10$I1GRg1KtLY5NSA5hdmJxR.cmCn8MKpV73rXSBS5Ldc8oFEuse2IN.', 'ADMINISTRADOR'),
 ('u-agente',   'Agente TravelGo',        'agente@travelgo.com',   '$2a$10$IsYP4Lx0Om1/aisZ0KM6IeEOQpK16ZPBwSSQSA.SGkQqtZt7hyY7u', 'AGENTE'),
 ('u-cliente',  'Cliente de prueba',      'cliente@travelgo.com',  '$2a$10$S3VcVTzl02QNksMLVCt5hO71T5c.2.iuRyAvxCu7OZ5KNi2AZfTgW', 'CLIENTE'),
 ('u-cliente2', 'Segundo cliente',        'cliente2@travelgo.com', '$2a$10$S3VcVTzl02QNksMLVCt5hO71T5c.2.iuRyAvxCu7OZ5KNi2AZfTgW', 'CLIENTE')
ON CONFLICT (id) DO NOTHING;

-- ---------- Destinos ----------
-- Se fijan los ids para poder referenciarlos; si la tabla ya tenía datos, no se tocan.
INSERT INTO destino (id, nombre, descripcion, pais, ciudad) VALUES
 (1, 'Cartagena de Indias', 'Ciudad amurallada y playas del Caribe', 'Colombia', 'Cartagena'),
 (2, 'Bogotá',              'Capital de Colombia, museos y gastronomía', 'Colombia', 'Bogotá'),
 (3, 'Cusco y Machu Picchu','Ruta inca en los Andes',                'Perú',     'Cusco'),
 (4, 'Cancún',              'Playas y arrecife del Caribe mexicano', 'México',   'Cancún')
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('destino', 'id'), GREATEST((SELECT MAX(id) FROM destino), 1));

-- ---------- Paquetes ----------
INSERT INTO paquete_turistico (id, nombre, descripcion, precio, duracion_dias, activo) VALUES
 (1, 'Cartagena Esencial',   'Vuelo, hotel y tour por la ciudad amurallada', 1200000.00, 4, TRUE),
 (2, 'Bogotá Cultural',      'Museos, Monserrate y gastronomía local',        800000.00, 3, TRUE),
 (3, 'Machu Picchu Aventura','Cusco, Valle Sagrado y Machu Picchu',          3500000.00, 7, TRUE),
 (4, 'Cancún Todo Incluido', 'Resort todo incluido frente al mar',           2800000.00, 5, TRUE),
 (5, 'Paquete retirado',     'Paquete desactivado para probar el filtro',     500000.00, 2, FALSE)
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('paquete_turistico', 'id'), GREATEST((SELECT MAX(id) FROM paquete_turistico), 1));

INSERT INTO paquete_destino (paquete_id, destino_id) VALUES
 (1, 1), (2, 2), (3, 3), (4, 4), (5, 2)
ON CONFLICT DO NOTHING;

-- ---------- Servicios reservables ----------
INSERT INTO servicio_reservable (id, precio, disponible, tipo) VALUES
 ('s-vuelo-1',  450000.00, TRUE,  'VUELO'),
 ('s-vuelo-2',  520000.00, TRUE,  'VUELO'),
 ('s-vuelo-3',  300000.00, FALSE, 'VUELO'),       -- no disponible
 ('s-hotel-1',  200000.00, TRUE,  'HOTEL'),       -- por noche
 ('s-hotel-2',  300000.00, TRUE,  'HOTEL'),
 ('s-trans-1',   80000.00, TRUE,  'TRANSPORTE'),
 ('s-trans-2',   60000.00, FALSE, 'TRANSPORTE')   -- no disponible
ON CONFLICT (id) DO NOTHING;

INSERT INTO vuelo (id, aerolinea, origen, destino, fecha_salida, fecha_llegada) VALUES
 ('s-vuelo-1', 'Avianca', 'Bogotá',   'Cartagena', '2026-12-10 08:00', '2026-12-10 09:35'),
 ('s-vuelo-2', 'LATAM',   'Bogotá',   'Cusco',     '2026-12-15 06:30', '2026-12-15 10:10'),
 ('s-vuelo-3', 'Viva Air','Medellín', 'Cartagena', '2026-12-11 14:00', '2026-12-11 15:25')
ON CONFLICT (id) DO NOTHING;

INSERT INTO hotel (id, nombre, direccion, tipo_habitacion, noches) VALUES
 ('s-hotel-1', 'Hotel Caribe',    'Bocagrande, Cartagena',  'Doble',  3),
 ('s-hotel-2', 'Hotel Andino',    'Centro histórico, Cusco', 'Suite', 2)
ON CONFLICT (id) DO NOTHING;

INSERT INTO transporte (id, tipo, origen, destino, fecha) VALUES
 ('s-trans-1', 'Bus',    'Cartagena', 'Barranquilla', '2026-12-12 07:00'),
 ('s-trans-2', 'Tren',   'Cusco',     'Machu Picchu',  '2026-12-16 05:45')
ON CONFLICT (id) DO NOTHING;

-- ---------- Reservas de ejemplo ----------
-- Total = suma de calcularCosto(): vuelo = precio, hotel = precio x noches, transporte = precio.
INSERT INTO reserva (id, fecha_reserva, estado, total, usuario_id, paquete_id) VALUES
 ('r-pendiente', '2026-10-20', 'PENDIENTE',  1050000.00, 'u-cliente', 1),   -- vuelo 450.000 + hotel 200.000 x 3
 ('r-pagada',    '2026-10-10', 'CONFIRMADA',  450000.00, 'u-cliente', 1),   -- vuelo 450.000
 ('r-cancelada', '2026-10-05', 'CANCELADA',   600000.00, 'u-cliente', NULL), -- hotel 300.000 x 2
 ('r-vacia',     '2026-10-25', 'PENDIENTE',        0.00, 'u-cliente2', NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO reserva_servicio (reserva_id, servicio_id) VALUES
 ('r-pendiente', 's-vuelo-1'),
 ('r-pendiente', 's-hotel-1'),
 ('r-pagada',    's-vuelo-1'),
 ('r-cancelada', 's-hotel-2')
ON CONFLICT DO NOTHING;

-- ---------- Pago y factura de la reserva ya pagada ----------
INSERT INTO pago (id, reserva_id, monto, metodo, fecha_pago) VALUES
 ('p-1', 'r-pagada', 450000.00, 'TARJETA_CREDITO', '2026-10-10 15:30:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO factura (id, numero, pago_id, fecha_emision, total) VALUES
 ('f-1', 'FAC-00000001', 'p-1', '2026-10-10', 450000.00)
ON CONFLICT (id) DO NOTHING;
