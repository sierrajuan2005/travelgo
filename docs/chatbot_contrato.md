# Chatbot: contrato de integración y tablas compartidas (Sprint 5, Integrante 4)

## 1. Endpoint para el widget (Integrante 3)

`POST /api/chatbot` — requiere sesión iniciada (como el resto de `/api/**`) y el token CSRF
(`X-CSRF-TOKEN`), igual que las demás peticiones POST de la web.

Pedido:

```json
{ "sesionId": null, "mensaje": "Paquetes baratos para Bogotá" }
```

- `sesionId`: `null` en el primer mensaje. Desde el segundo, reenviar el que devolvió el servidor.
- `mensaje`: obligatorio, hasta 500 caracteres.

Respuesta `200`:

```json
{
  "sesionId": "3f0c…",
  "respuesta": "Los paquetes más baratos para Bogotá:\n• Bogotá Cultural — 3 días — $800.000 COP (precio de referencia)",
  "sugerencias": ["Paquetes baratos", "Paquetes de lujo", "¿Hay cupones?", "¿Cómo pago?", "¿Cómo va mi reserva?"]
}
```

- `respuesta` es texto plano con saltos de línea (`\n`); el widget debe mostrarlo respetándolos y sin interpretarlo como HTML.
- `sugerencias` son los botones: al pulsar uno, el widget envía su texto como un mensaje nuevo.
- Errores: `400` (mensaje vacío o demasiado largo), `401/403` (sin sesión), `500` (error de base de datos).

`GET /api/chatbot/{sesionId}/mensajes` — historial para recargar la conversación al abrir el widget.
Devuelve una lista de `{ id, sesionId, emisor ("USUARIO" | "BOT"), texto, intencion, fecha }`. Solo lo ve quien abrió
la sesión; para cualquier otro responde `404`.

El contrato no depende de la web (no usa cookies propias ni vistas): otro canal puede usar el mismo motor llamando a
`ChatbotService.responder(sesionId, mensaje, correo, canal)`.

## 2. Qué puede responder hoy

| El cliente escribe | Estrategia | Estado |
|---|---|---|
| Paquetes baratos para Bogotá | `PaquetesBaratosEstrategia` (tercio más barato del destino) | Listo |
| Paquetes con hoteles de lujo en Cartagena | `PaquetesLujoEstrategia` | Listo: paquetes del destino con al menos un hotel de esa gama (usa `ClasificadorGama` del Integrante 2), del más barato al más caro |
| Algo para 2 personas en diciembre | `PaquetesDestinoEstrategia` | Muestra los paquetes del destino y avisa que fechas y personas aún no filtran (depende del Integrante 1) |
| ¿Qué incluye el paquete X? | `ServiciosPaqueteEstrategia` | Listo, con precio de referencia, fecha de consulta y enlace de reserva |
| ¿Cómo va mi reserva? / ¿Tengo pagos pendientes? | `EstadoReservaEstrategia`, `PagosPendientesEstrategia` | Listo; solo con sesión y solo del propio usuario |
| ¿Hay cupones? | `CuponesEstrategia` | Listo (solo vigentes) |
| ¿Cómo pago? | `ComoPagarEstrategia` | Listo |
| Cualquier otra cosa | `NoEntendidaEstrategia` | Ofrece las opciones que sí maneja |

## 3. Tablas compartidas (en `schema.sql`)

| Tabla | Para quién | Notas |
|---|---|---|
| `chat_sesion`, `mensaje` | Chatbot (Integrante 4) | `usuario_id` es NULL si quien escribió no inició sesión |
| `envio_recordatorio` | Recordatorios (Integrante 3) | `UNIQUE (reserva_id, tipo, canal)` impide repetir el mismo aviso. Tipos: `VIAJE_7_DIAS`, `VIAJE_1_DIA`, `VIAJE_HORAS`, `PAGO_PENDIENTE`, `PAGO_CONFIRMADO`. Canales: `CORREO`, `SMS`. Estados: `ENVIADO`, `FALLIDO` |
| `cache_mapa` | Mapa (Integrante 2) | Una fila por ciudad, categoría y fuente; `respuesta_json` guarda la respuesta de la API y `expira_en` dice hasta cuándo sirve |

Si algún integrante necesita otra columna o otro valor en los `CHECK`, se acuerda y se cambia en `schema.sql` en un PR pequeño.
