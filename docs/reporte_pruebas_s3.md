# Reporte de pruebas — Sprint 3

Integrante 4 · Código revisado: `develop` en el commit `5166cf3` (merge de `feature/roles-permisos`).

## 1. Qué se revisó y qué se pudo ejecutar

| Qué | Estado |
|---|---|
| Pruebas unitarias (JUnit 5 + Mockito) de reservas, servicios, pagos, facturas, dominio y login por rol | **Escritas; hay que correr `./mvnw test` en la máquina del equipo.** El entorno donde se prepararon no puede descargar dependencias de Maven. Lo que sí se verificó: `ReservaTest`, `PagoFacturaTest` y `UsuarioDAOMemoriaTest` (33 pruebas) se compilaron contra el código real de `develop` y se ejecutaron con un lanzador propio: **pasan las 33**. Las pruebas con Mockito (`ReservaServiceTest`, `ServicioReservableServiceTest`, `PagoServiceTest`, `FacturaServiceTest`) se comprobaron solo a nivel de compilación contra el código real; `ServicioUsuarioDetallesTest` no se pudo ni compilar aquí (usa Spring Security). Ninguna de esas se ejecutó. |
| Defecto D1 y su arreglo | **Confirmado ejecutando el código real:** con un usuario registrado como lo hace `AuthController`, `crearReserva` lanza "Usuario no encontrado". Con el cambio a `buscarPorCorreo` el mismo escenario pasa. También se ejecutó el flujo reserva → pago → factura con los DAO en memoria reales: el pago confirma la reserva, genera una sola factura y un segundo pago se rechaza. |
| `schema.sql` y `data.sql` | **Ejecutados en PostgreSQL 16**, dos veces seguidas sin errores (son repetibles). Se verificó que los totales de las reservas sembradas coinciden con la regla del dominio y que las restricciones rechazan lo que deben (segundo pago de una reserva, estado inválido, correo repetido, método de pago inexistente, vuelo con origen igual al destino). |
| Colección de Postman | JSON válido (85 peticiones). **No ejecutada contra la app.** |
| Pruebas de integración end-to-end | Pendientes: necesitan la app corriendo con la base de datos y los usuarios de `data.sql`. |

Los defectos de abajo salen de **leer el código de `develop`**, no de ejecutarlo. Cada uno trae la línea exacta para que quien lo corrija lo pueda confirmar en un minuto, y las pruebas de este sprint los dejan en evidencia al correrlas.

## 2. Defectos encontrados

| ID | Gravedad | Dueño | Resumen |
|---|---|---|---|
| D1 | Alta | Integrante 1 | Crear una reserva busca al usuario por correo usando `buscarPorId` |
| D2 | Alta | Integrante 1 | `/api/**` no aplica la matriz de permisos: solo exige estar autenticado |
| D3 | Alta | Integrantes 1 y 2 | Un usuario puede pagar reservas ajenas y ver pagos, facturas y reservas de otros |
| D4 | Media | Integrante 1 | El AGENTE no puede gestionar reservas de clientes |
| D5 | Alta (bloquea pruebas) | Integrantes 1 y 2 | No existe forma de tener un AGENTE o ADMINISTRADOR en el sistema |
| D6 | Media | Integrante 1 | CSRF activo: las peticiones POST/PUT/DELETE a `/api/**` sin token responden 403 |
| D7 | Media | Integrante 2 | El ER y las clases `Pago`/`Factura` no coinciden |
| D8 | Media | Todos | Contraseña de PostgreSQL publicada en el repositorio |
| D9 | Baja | Integrante 1 | `Reserva` no valida el estado al rehidratarla desde la BD |

### D1 — Crear reserva siempre falla con "Usuario no encontrado"
- **Dónde:** `ReservaService.java`, línea 37: `usuarioDAO.buscarPorId(correoAutenticado)`.
- **Por qué falla:** `crearReserva` recibe el **correo** del usuario autenticado, pero lo busca por **id**. Al registrarse, el id es un UUID (`AuthController`), así que nunca coincide con el correo.
- **Resultado:** `POST /api/reservas` responde 404 para cualquier usuario.
- **Arreglo (una línea):** usar `usuarioDAO.buscarPorCorreo(correoAutenticado)`, igual que ya hace `listarPropias`.
- **Cómo se ve:** fallan las 7 pruebas de `crearReserva` en `ReservaServiceTest` y la petición "Crear reserva" de Postman. Esas pruebas ya esperan el comportamiento correcto, así que pasan solas cuando se arregle.

### D2 — La matriz de permisos no se aplica a la API
- **Dónde:** `SecurityConfig.java`, líneas 28-31. Solo protege `/admin/**`, `/agente/**` y `/cliente/**`; todo lo demás es `anyRequest().authenticated()`. Ningún controlador usa `@PreAuthorize`.
- **Efecto:** cualquier usuario con sesión, incluso un CLIENTE, puede crear o borrar destinos, paquetes y servicios, y listar todos los pagos y facturas. La matriz dice que eso es solo del ADMINISTRADOR (catálogo) y del AGENTE (pagos).
- **Arreglo sugerido:** reglas por ruta en `SecurityConfig`, por ejemplo `requestMatchers(HttpMethod.POST, "/api/destinos/**", "/api/paquetes/**", "/api/servicios/**").hasRole("ADMINISTRADOR")` y `requestMatchers("/api/pagos", "/api/facturas").hasAnyRole("AGENTE")`; o `@EnableMethodSecurity` con `@PreAuthorize` en los controladores.
- **Cómo se ve:** carpeta "04 Roles y permisos" de Postman, peticiones marcadas con D2.

### D3 — Acceso a datos de otros usuarios
- `POST /api/pagos` (`PagoController`, línea 27) no comprueba que la reserva sea del usuario autenticado: un CLIENTE puede **pagar la reserva de otro**.
- `GET /api/reservas/usuario/{usuarioId}` (`ReservaController`, línea 49) y `GET /api/facturas/usuario/{usuarioId}` (`FacturaController`, línea 36) devuelven los datos de **cualquier** usuario con solo cambiar el id.
- `GET /api/pagos` y `GET /api/facturas` listan los de **todos**.
- **Arreglo sugerido:** CLIENTE solo opera sobre lo suyo (como ya se hizo con `obtenerPropia`); los listados completos y la consulta por usuario, solo para AGENTE.

### D4 — El AGENTE no puede gestionar reservas de clientes
- **Dónde:** `ReservaController` usa solo `listarPropias`, `obtenerPropia`, `confirmarPropia` y `cancelarPropia`, que exigen ser el dueño.
- **Efecto:** el AGENTE solo ve y confirma sus propias reservas. La matriz dice que "gestiona todas las reservas" y la historia de usuario 6 dice que el agente confirma reservas de clientes.
- **Arreglo sugerido:** si el rol autenticado es AGENTE, usar los métodos sin dueño que ya existen (`listar`, `obtener`, `confirmarReserva`, `cancelarReserva`).
- **Por verificar:** que `AccessDeniedException` salga como 403 y no como 500 (no hay un `@ExceptionHandler` para ella en `GlobalExceptionHandler`).

### D5 — Con los DAO en memoria no se pueden probar los roles
- `AuthController` solo crea usuarios con rol CLIENTE y `UsuarioDAOMemoria` arranca vacío.
- La lista de DAO JDBC de la Fase B (reservas, servicios, pagos, facturas) **no incluye** usuarios. Sin un `UsuarioDAOJdbc` que cargue `data.sql`, no habrá AGENTE ni ADMINISTRADOR y no se podrá probar nada de roles.
- **Propuesta:** agregar `UsuarioDAOJdbc` a la Fase B, o mientras tanto sembrar los 4 usuarios de `data.sql` en el constructor de `UsuarioDAOMemoria`.

### D6 — CSRF activo en la API REST
- `SecurityConfig` no desactiva CSRF. Cualquier POST/PUT/DELETE desde Postman, `fetch` o un script sin token responde 403.
- La colección de Postman obtiene el token sola. Si el equipo prefiere simplificar, puede ignorar CSRF solo para `/api/**` con `csrf.ignoringRequestMatchers("/api/**")`, sabiendo que la autenticación sigue siendo por cookie de sesión (decisión de seguridad que le toca a Integrante 1).

### D7 — ER y clases no coinciden
| | ER (`ModeloER.md`) | Código |
|---|---|---|
| Relación Pago–Factura | `Pago.factura_id` (el pago apunta a la factura) | `Factura` contiene al `Pago` |
| Relación Factura–Reserva | `Factura.reserva_id` | la reserva se obtiene por el pago |
| Campos de Factura | `subtotal`, `estado` | no existen |
| `Pago.estado` | existe | no existe |
| ID de destino y paquete | `varchar(36)` | `Long` (JPA) |

`schema.sql` sigue el **código** (así Integrante 2 puede reconstruir los objetos sin cambiar el dominio). Si el equipo prefiere el ER original, hay que cambiar las clases. Falta decidir y actualizar `ModeloER.md`.

### D8 — Contraseña publicada
`application.properties` (línea 6) tiene la contraseña de PostgreSQL escrita y subida al repositorio. Conviene cambiar esa contraseña y leerla de una variable de entorno (`${DB_PASSWORD}`). También conviene quitar `logging.level.org.springframework.security=TRACE` antes de la entrega.

### D9 — Estado nulo al rehidratar
`new Reserva(id, usuario, paquete, servicios, fecha, null, total)` se acepta sin error y luego `getEstado()` falla con `NullPointerException`. Falta validar que el estado no sea nulo en ese constructor.

## 3. Observaciones menores
- `ReservaService.crearReserva` ya no usa el campo `usuarioId` del body (correcto), pero `ReservaRespuestaDTO` sí devuelve el `usuarioId` interno.
- `Reserva.getEstado()` devuelve `String` y `getEstadoEnum()` el enum; conviene dejar uno solo cuando se migre a JDBC.
- Con `spring.jpa.hibernate.ddl-auto=update` y un `schema.sql` propio, hay que decidir quién crea las tablas (ver cabecera de `schema.sql`).

## 4. Qué debe hacer cada quien

| Quién | Acción |
|---|---|
| Integrante 1 | D1 (una línea), D2, D3, D4 y D6; decidir con Integrante 2 el D5 |
| Integrante 2 | D3 (parte de pagos), D5, decidir D7 al migrar a JDBC |
| Integrante 3 | Confirmar que el panel admin se oculta a no administradores (las rutas `/admin/**` todavía no existen) |
| Todos | D8 antes de la entrega |
| Integrante 4 | Ejecutar `./mvnw test` y la colección de Postman y actualizar este reporte con los resultados |
