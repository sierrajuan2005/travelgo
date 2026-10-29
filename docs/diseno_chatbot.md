# Diseño de respuestas del chatbot

Solo **diseño**: se construye en el Sprint 4 si el núcleo está estable. Parte de `chatbot_investigacion.md` y lo ajusta al estado real del proyecto.

## 1. Alcance
El chatbot responde con **reglas guiadas** (sin inteligencia artificial): reconoce la intención del mensaje por palabras clave, consulta los services que ya existen y devuelve una respuesta de plantilla. Puede hacer tres cosas: informar sobre el catálogo, informar sobre las reservas y facturas **del propio usuario**, y orientar sobre cómo reservar y pagar. **No** reserva, paga ni cancela por el usuario.

## 2. Ajustes a la investigación inicial
| La investigación decía | Hoy en el proyecto | Decisión |
|---|---|---|
| Spring Boot 3, Java 17 | Spring Boot 4.1, Java 21 | Actualizar el documento |
| Consultar la BD con JPA | Destinos y paquetes usan JPA; reservas, pagos y facturas pasarán a JDBC | El chatbot usa los **services** (`PaqueteService`, `ReservaService`, `FacturaService`), nunca el acceso a datos directamente. Así no le afecta el cambio a JDBC. |
| "Estado de mi reserva 123" | El id de una reserva es un UUID de 36 caracteres, imposible de teclear | El bot lista "mis reservas" con un número corto (1, 2, 3) y el usuario elige por número |
| Cualquier visitante | El catálogo es público, las reservas exigen login | Intenciones de catálogo abiertas; las de reservas, solo con sesión |

## 3. Reglas de seguridad
1. Las respuestas sobre reservas y facturas usan **solo el usuario autenticado** (su correo en la sesión), igual que `obtenerPropia` y `listarPropias`. Si el mensaje trae un id ajeno, se responde como si no existiera.
2. El bot nunca pide ni repite contraseñas, números de tarjeta ni datos de pago.
3. No muestra ids internos (UUID); muestra número de factura (`FAC-xxxxxxxx`) y un número de lista.
4. Sin sesión, las intenciones de reservas responden con la invitación a iniciar sesión y el enlace `/login`.
5. El texto del usuario se escapa al mostrarlo (evita inyectar HTML en la respuesta).

## 4. Cómo interpreta el mensaje
1. Normaliza: minúsculas, sin tildes, sin signos de puntuación y espacios repetidos.
2. Busca la intención cuyas palabras clave aparezcan en el mensaje (gana la de más coincidencias; si hay empate, la más específica).
3. Extrae datos del mensaje cuando la intención los pide: nombre de destino, un número de lista (`#2`, "la 2") o un rango de precio ("menos de 2 millones").
4. Si ninguna intención coincide, usa la respuesta de **no entendí**.

## 5. Intenciones y respuestas

Las partes entre `{llaves}` se llenan con datos reales. Los precios se muestran con separador de miles y el signo `$`.

### 5.1 Generales
| Intención | Se activa con (ejemplos) | Respuesta |
|---|---|---|
| SALUDO | "hola", "buenas", "buenos días" | «¡Hola! Soy el asistente de TravelGo. Puedo ayudarte con **paquetes disponibles**, **tus reservas**, **cómo reservar** y **cómo pagar**. ¿Qué necesitas?» |
| AYUDA | "ayuda", "qué puedes hacer", "opciones" | «Puedes preguntarme: «ver paquetes», «paquetes a {destino}», «mis reservas», «estado de la reserva 2», «mi factura», «cómo reservar» o «cómo pagar».» |
| DESPEDIDA | "gracias", "adiós", "chao" | «¡Con gusto! Que disfrutes tu viaje. Aquí estaré si necesitas algo más.» |
| NO_ENTENDI | cualquier cosa sin intención | «No estoy seguro de haberte entendido. Prueba con «ver paquetes», «mis reservas» o «cómo reservar», o escribe «ayuda».» |

### 5.2 Catálogo (público)
| Intención | Se activa con | Datos | Respuesta |
|---|---|---|---|
| VER_PAQUETES | "ver paquetes", "qué paquetes hay", "catálogo" | `PaqueteService.listar(true)` (solo activos) | «Estos son nuestros paquetes disponibles:\n1. {nombre} — ${precio} ({duracionDias} días)\n2. …\nEscribe «detalle 1» para saber más.» Si hay más de 5, muestra 5 y agrega «Hay {n} más. Escribe «ver más».» |
| VER_PAQUETES_VACIO | igual, sin resultados | lista vacía | «Por ahora no hay paquetes disponibles. Vuelve a consultarnos pronto.» |
| DETALLE_PAQUETE | "detalle 2", "más información del paquete 2" | paquete elegido de la última lista | «{nombre}\n{descripcion}\nDuración: {duracionDias} días · Precio: ${precio}\nDestinos: {destinos}\nPara reservar entra a {enlace al catálogo}.» |
| PAQUETES_POR_DESTINO | "paquetes a Cartagena", "viajes a Cusco" | paquetes activos cuyo destino contenga el texto | «Encontré {n} paquete(s) para {destino}:\n1. …» o, sin resultados, «No tengo paquetes para «{destino}». Estos son los destinos disponibles: {lista de destinos}.» |
| PAQUETES_POR_PRECIO | "paquetes menores a 2 millones", "más baratos" | paquetes activos filtrados por precio | «Paquetes hasta ${tope}:\n1. …» o «No encontré paquetes por ese valor; el más económico cuesta ${minimo}.» |

### 5.3 Reservas y facturas (requieren sesión)
| Intención | Se activa con | Datos | Respuesta |
|---|---|---|---|
| MIS_RESERVAS | "mis reservas", "ver mis viajes" | `ReservaService.listarPropias(correo)` | «Tienes {n} reserva(s):\n1. {fecha} — {estado} — ${total}\n2. …\nEscribe «estado de la reserva 1» para ver el detalle.» Sin reservas: «Aún no tienes reservas. Escribe «ver paquetes» para empezar.» |
| ESTADO_RESERVA | "estado de la reserva 2", "cómo va mi reserva 1" | la reserva elegida de la lista (si no hay lista previa, se pide «mis reservas» primero) | «Reserva {n}: {estado}.\nFecha: {fecha} · Total: ${total}\nServicios: {lista}.» + una **sugerencia según el estado** (ver 5.4) |
| RESERVA_INEXISTENTE | número fuera de la lista | — | «No encontré esa reserva. Escribe «mis reservas» para ver la lista.» |
| MI_FACTURA | "mi factura", "factura de la reserva 1" | `FacturaService.obtenerPorReserva(...)` de una reserva propia | «Factura {numero}, emitida el {fecha}.\nTotal: ${total} · Método de pago: {metodo}.» Si no tiene: «Esa reserva aún no tiene factura: se genera cuando pagas.» |
| SIN_SESION | cualquiera de las tres anteriores sin login | — | «Para ver tus reservas necesito que inicies sesión: {enlace /login}.» |

### 5.4 Sugerencia según el estado de la reserva
| Estado | Texto que se agrega |
|---|---|
| PENDIENTE | «Está pendiente de pago. Puedes pagarla desde la sección de reservas.» |
| CONFIRMADA | «Está confirmada y pagada. ¡Buen viaje!» |
| CANCELADA | «Está cancelada y ya no se puede pagar. Si quieres viajar, crea una nueva reserva.» |

### 5.5 Preguntas frecuentes (respuestas fijas)
| Intención | Se activa con | Respuesta |
|---|---|---|
| COMO_RESERVAR | "cómo reservo", "quiero reservar" | «Para reservar: 1) Inicia sesión. 2) Elige un paquete en el catálogo. 3) Selecciona los servicios (vuelo, hotel, transporte). 4) Confirma. Tu reserva queda **pendiente** hasta que la pagues.» |
| COMO_PAGAR | "cómo pago", "métodos de pago" | «Puedes pagar con tarjeta de crédito, tarjeta de débito, transferencia o efectivo. Entra a tu reserva pendiente y elige «Pagar». Al pagar, tu reserva se confirma y recibes la factura.» |
| COMO_CANCELAR | "cancelar mi reserva" | «Puedes cancelar una reserva desde la sección de reservas. Una reserva cancelada no se puede pagar ni volver a activar.» |
| PAGO_DOBLE | "pagué dos veces", "me cobraron doble" | «Cada reserva se paga una sola vez: el sistema no permite un segundo pago. Si ves un cobro repetido en tu banco, comunícate con un agente.» |
| CONTACTO_AGENTE | "hablar con un agente", "ayuda humana" | «Un agente puede ayudarte con casos especiales. Escríbenos a {correo de contacto de la agencia}.» |

## 6. Ejemplos de conversación

**Consultar el catálogo**
> Usuario: hola
> Bot: ¡Hola! Soy el asistente de TravelGo. Puedo ayudarte con paquetes disponibles, tus reservas, cómo reservar y cómo pagar. ¿Qué necesitas?
> Usuario: paquetes a cartagena
> Bot: Encontré 1 paquete para Cartagena:
> 1. Cartagena Esencial — $1.200.000 (4 días)
> Escribe «detalle 1» para saber más.

**Estado de una reserva**
> Usuario: mis reservas
> Bot: Tienes 2 reservas:
> 1. 2026-10-20 — PENDIENTE — $1.050.000
> 2. 2026-10-10 — CONFIRMADA — $450.000
> Usuario: estado de la reserva 1
> Bot: Reserva 1: PENDIENTE. Fecha: 2026-10-20 · Total: $1.050.000. Servicios: vuelo Avianca Bogotá–Cartagena; hotel Hotel Caribe (3 noches). Está pendiente de pago. Puedes pagarla desde la sección de reservas.

**Sin sesión**
> Usuario: mi factura
> Bot: Para ver tus reservas necesito que inicies sesión: /login.

## 7. Contrato propuesto para el Sprint 4
| | |
|---|---|
| Endpoint | `POST /api/chatbot/mensajes` (requiere sesión; para visitantes solo responde intenciones de catálogo y preguntas frecuentes) |
| Petición | `{ "mensaje": "paquetes a cartagena" }` (máx. 300 caracteres, no vacío) |
| Respuesta 200 | `{ "intencion": "PAQUETES_POR_DESTINO", "respuesta": "…", "sugerencias": ["detalle 1", "mis reservas"] }` |
| Errores | 400 si el mensaje está vacío o supera 300 caracteres |
| Clases | `ChatbotController`, `ChatbotService`, `ResolverIntencion` (palabras clave → intención), `PlantillasRespuesta` (los textos de este documento) |
| Estado de la conversación | La última lista mostrada (paquetes o reservas) se guarda en la sesión HTTP para resolver «detalle 2» o «estado de la reserva 1» |

## 8. Casos de prueba del chatbot
| ID | Mensaje | Condición | Resultado esperado |
|---|---|---|---|
| CP-BOT-01 | "Hola" | — | Intención SALUDO |
| CP-BOT-02 | "HOLA!!!  " | mayúsculas, signos y espacios | Intención SALUDO (normalización) |
| CP-BOT-03 | "ver paquetes" | hay paquetes activos y uno inactivo | Lista solo los activos |
| CP-BOT-04 | "ver paquetes" | catálogo vacío | Mensaje de VER_PAQUETES_VACIO |
| CP-BOT-05 | "paquetes a Cusco" | existe el destino | Lista paquetes con ese destino |
| CP-BOT-06 | "paquetes a Narnia" | no existe | Mensaje con los destinos disponibles |
| CP-BOT-07 | "mis reservas" | usuario con 2 reservas | Lista las 2, solo las suyas |
| CP-BOT-08 | "mis reservas" | sin sesión | Invitación a iniciar sesión |
| CP-BOT-09 | "estado de la reserva 3" | solo tiene 2 | RESERVA_INEXISTENTE |
| CP-BOT-10 | "estado de la reserva {id de otro usuario}" | id ajeno | Responde como inexistente; no revela nada |
| CP-BOT-11 | "mi factura" | reserva pagada | Número de factura, total y método |
| CP-BOT-12 | "mi factura" | reserva pendiente | «aún no tiene factura» |
| CP-BOT-13 | "asdfgh" | — | NO_ENTENDI |
| CP-BOT-14 | "" o 301 caracteres | — | 400 |
| CP-BOT-15 | `<script>alert(1)</script>` | — | Se muestra como texto, nunca como HTML |
| CP-BOT-16 | "mi tarjeta es 4111 1111 1111 1111" | — | NO_ENTENDI; el número no se guarda ni se repite |
