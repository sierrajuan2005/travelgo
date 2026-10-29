# Casos de prueba — Facturación, pagos y roles (Sprint 3)

Cubren las historias de usuario 5 a 8 (`historias_usuario.md`) y la matriz de `matriz_permisos.md`.

**Cómo leer la tabla:** "Auto" indica dónde está automatizado el caso (JUnit = `src/test/...`, PM = colección de Postman). "Hoy" dice qué debe pasar con el código actual de `develop` según la revisión (ver `reporte_pruebas_s3.md`): ✅ debería pasar, ❌ falla por un defecto, ⏳ depende de que se migre a JDBC.

Datos de prueba: los de `data.sql`. Cliente `cliente@travelgo.com`, segundo cliente `cliente2@travelgo.com`, agente `agente@travelgo.com`, administrador `admin@travelgo.com`.

## 1. Facturación y pagos

| ID | Caso | Entrada / pasos | Resultado esperado | Auto | Hoy |
|---|---|---|---|---|---|
| CP-FAC-01 | Pagar una reserva pendiente con servicios | `POST /api/pagos` con `reservaId` pendiente y `TARJETA_CREDITO` | 201. Se devuelve la factura; la reserva queda CONFIRMADA; el monto es el total de la reserva | JUnit `PagoServiceTest`, PM 03 | ✅ |
| CP-FAC-02 | El total de la factura es la suma de los servicios | Reserva con vuelo 450.000 + hotel 200.000 × 3 noches | Total facturado = 1.050.000 | JUnit `PagoFacturaTest`, PM 03 | ✅ |
| CP-FAC-03 | Número de factura | Pagar cualquier reserva | Número con formato `FAC-` + 8 caracteres hex en mayúscula; distinto en cada factura | JUnit `PagoFacturaTest` | ✅ |
| CP-FAC-04 | Una reserva se paga una sola vez | Pagar dos veces la misma reserva | 2.º intento: 409; no se crea otro pago ni otra factura | JUnit `PagoServiceTest`, PM 03 | ✅ |
| CP-FAC-05 | No se paga una reserva cancelada | Cancelar y luego pagar | 409; no se guarda pago ni factura | JUnit, PM 03 | ✅ |
| CP-FAC-06 | No se paga una reserva sin servicios | Pagar la reserva `r-vacia` | 409; la reserva sigue PENDIENTE | JUnit | ✅ |
| CP-FAC-07 | Pagar una reserva que no existe | `reservaId` inventado | 404 | JUnit, PM 03 | ✅ |
| CP-FAC-08 | Método de pago inválido o ausente | `"BITCOIN"` o sin método | 400 | PM 03 | ✅ |
| CP-FAC-09 | Reserva ya confirmada por el agente y sin pago | Confirmar con `PUT /confirmar` y luego pagar | 201: se puede pagar | JUnit | ✅ |
| CP-FAC-10 | Orden de guardado del pago | Pagar | Se guarda primero la reserva confirmada, luego el pago y al final la factura | JUnit | ✅ |
| CP-FAC-11 | Consultar pago y factura | `GET /api/pagos/reserva/{id}`, `GET /api/facturas/{id}`, `GET /api/facturas/reserva/{id}` | 200 con los mismos datos; la factura **no** incluye la contraseña del usuario | PM 03 | ✅ |
| CP-FAC-12 | Consultar lo que no existe | Pago o factura de una reserva sin pagar, o id inventado | 404 | JUnit, PM 03 | ✅ |
| CP-FAC-13 | Persistencia | Pagar, reiniciar la app y consultar la factura | La factura sigue existiendo | manual / PM | ⏳ |
| CP-FAC-14 | Restricciones de base de datos | Insertar un segundo pago de una misma reserva; un pago con método inexistente | La base rechaza ambos | SQL (ya probado en PostgreSQL 16) | ✅ |

## 2. Reservas (base de los pagos)

| ID | Caso | Resultado esperado | Auto | Hoy |
|---|---|---|---|---|
| CP-RES-01 | Crear reserva con servicios válidos | 201, PENDIENTE, total correcto | JUnit `ReservaServiceTest`, PM 02 | ❌ D1 |
| CP-RES-02 | El dueño sale del usuario autenticado, no del body | Aunque el body traiga otro `usuarioId`, la reserva es del autenticado | PM 02 | ❌ D1 |
| CP-RES-03 | Servicio inexistente / no disponible / lista vacía / paquete inexistente | 404 / 409 / 400 / 404 y no se guarda nada | JUnit, PM 02 | ❌ D1 |
| CP-RES-04 | Cancelar y volver a cancelar | 1.º: 200 CANCELADA; 2.º: 409 | JUnit, PM 02 | ✅ |
| CP-RES-05 | Confirmar una reserva cancelada o sin servicios | 409 | JUnit, PM 02 | ✅ |
| CP-RES-06 | Reconstruir una reserva desde la BD | Conserva id, fecha, estado y total reales | JUnit `ReservaTest` | ✅ |

## 3. Roles y permisos

Lo que dice la matriz de `matriz_permisos.md`, convertido en casos:

| ID | Rol | Acción | Esperado | Auto | Hoy |
|---|---|---|---|---|---|
| CP-ROL-01 | Sin sesión | `GET /api/reservas` | Redirige a `/login` | PM 00 | ✅ |
| CP-ROL-02 | Sin sesión | `GET /catalogo` | 200 (público) | PM 00 | ✅ |
| CP-ROL-03 | Login | Cada rol inicia sesión | Obtiene la autoridad `ROLE_<ROL>` | JUnit `ServicioUsuarioDetallesTest`, PM | ✅ ⏳ |
| CP-ROL-04 | Login | Contraseña incorrecta | Redirige a `/login?error` | PM 00 | ✅ |
| CP-ROL-05 | CLIENTE | Ver / confirmar / cancelar la reserva de otro cliente | 403 | JUnit `ReservaServiceTest`, PM 04 | ✅ (servicio) |
| CP-ROL-06 | CLIENTE | Pagar la reserva de otro cliente | 403 | PM 04 | ❌ D3 |
| CP-ROL-07 | CLIENTE | `GET /api/reservas/usuario/{otro}` y `GET /api/facturas/usuario/{otro}` | 403 | PM 04 | ❌ D3 |
| CP-ROL-08 | CLIENTE | Listar todos los pagos y todas las facturas | 403 | PM 04 | ❌ D2/D3 |
| CP-ROL-09 | CLIENTE | Crear destino, paquete o servicio; borrar un servicio | 403 | PM 04 | ❌ D2 |
| CP-ROL-10 | CLIENTE | `/admin/**` y `/agente/**` | 403 | PM 04 | ✅ |
| CP-ROL-11 | AGENTE | Ver y confirmar las reservas de cualquier cliente | 200 | PM 04 | ❌ D4 |
| CP-ROL-12 | AGENTE | Listar pagos y facturas | 200 | PM 04 | ✅ |
| CP-ROL-13 | AGENTE | Crear destino, paquete o servicio | 403 | PM 04 | ❌ D2 |
| CP-ROL-14 | AGENTE | `/agente/**` pasa la seguridad; `/admin/**` | pasa / 403 | PM 04 | ✅ |
| CP-ROL-15 | ADMINISTRADOR | Crear destino, paquete y servicios; borrar un servicio | 201 / 204 | PM 01, 04 | ✅ |
| CP-ROL-16 | ADMINISTRADOR | `/admin/**` pasa la seguridad | no devuelve 403 | PM 04 | ✅ |
| CP-ROL-17 | Cualquiera | Registrarse | Se crea con rol CLIENTE; correo repetido se rechaza | manual | ✅ |
| CP-ROL-18 | Panel admin (UI) | Entrar como CLIENTE y como ADMINISTRADOR | Solo el ADMINISTRADOR ve el panel | manual (Integrante 3) | ⏳ |

## 4. Usuarios (login y registro)

| ID | Caso | Esperado | Auto |
|---|---|---|---|
| CP-USR-01 | Buscar por correo sin importar mayúsculas | Encuentra al usuario | JUnit `UsuarioDAOMemoriaTest` |
| CP-USR-02 | Buscar por correo no equivale a buscar por id | `buscarPorId("ana@...")` → vacío | JUnit `UsuarioDAOMemoriaTest` |
| CP-USR-03 | Usuario inexistente en el login | `UsernameNotFoundException` | JUnit `ServicioUsuarioDetallesTest` |
