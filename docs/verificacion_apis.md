# Verificación de APIs — Sprint 4 (Integrante 4)

Documento único con una fila por API: **pasó / no pasó / pasó con límites**. Lo elabora Integrante 4 con los aportes
de los demás. Última actualización: 2026-11-12. Lo marcado **(por probar)** o **(por leer)** no está confirmado todavía.

## 1. Resumen

| API | Dueño | Para qué | Resultado | Llave | Moneda | Cuota gratuita | Atribución |
|---|---|---|---|---|---|---|---|
| Ignav | Integrante 1 | Vuelos con precio y enlace | **Por probar con la llave real**: el adaptador ya existe (`integracion/IgnavProveedorVuelos`). Falta confirmar que trae Avianca | Sí (variable `IGNAV_API_KEY`) | **Por confirmar:** el adaptador descarta todo lo que no venga en COP; si Ignav responde en USD para Colombia, los resultados salen vacíos | 1.000 consultas **una sola vez** (no se reinicia cada mes); luego US$2 por 1.000 | Por leer en los términos |
| AirLabs | Integrante 1 | Horarios de aerolíneas | **Por probar** | Sí | No aplica (no da precios) | Por confirmar en su cuenta | Por leer |
| Xotelo | Integrante 2 | Hoteles y tarifas (datos de TripAdvisor) | **Pasó con límites**: el adaptador existe y tiene pruebas; falta confirmar términos | No | El adaptador consulta siempre en USD; acepta `currency` (USD, EUR, BRL...) pero **no COP** | Sin límite publicado | No lo dice |
| Otras fuentes de hoteles | Integrante 2 | Respaldo | Por decidir | — | — | — | — |
| Open-Meteo (clima y geocodificación) | Integrante 3 | Clima, ciudad a coordenadas | **Pasó** (2026-11-06) | No | °C, no hay precios | Gratis, uso no comercial | "Weather data by Open-Meteo.com" (CC BY 4.0); GeoNames (CC BY 4.0) |
| Wikipedia + Wikimedia Commons | Integrante 3 | Fotos y atracciones | **Pasó** / atracciones **pasó con límites** | No | No aplica | Sin llave; pide User-Agent | Autor + licencia + enlace a Commons; textos CC BY-SA 4.0 |
| OpenTripMap | Integrante 3 | Atracciones con dirección | **Pendiente** de llave (sin llave responde 401) | Sí (gratis) | No aplica | Por confirmar | © colaboradores de OpenStreetMap (ODbL) |
| Geoapify | Integrante 3 | Hoteles y atracciones de apoyo | **Pendiente** de llave | Sí (gratis) | No aplica | Por confirmar | © OpenStreetMap, "Powered by Geoapify" |
| Overpass API | Integrante 3 | Alternativa sin llave | **No pasó** (no respondió); descartada | No | — | — | — |

Descartadas desde el planning: Amadeus Self-Service (cerrado en julio de 2026) y Duffel (aerolíneas ficticias en
modo de prueba). No se usan scrapers de terceros.

## 2. Términos de uso (criterio: ninguna prohíbe el uso académico)

| API | Qué se encontró | Qué falta |
|---|---|---|
| Ignav | Cobro: 1.000 consultas gratis (solo cuentan las exitosas, HTTP 200) y luego US$2 por 1.000; sin límite de velocidad. Los resultados de búsqueda se pueden reutilizar hasta **30 minutos** y el campo `observed_at` dice cuándo se consultó. El enlace de reserva sale de un endpoint aparte que usa el `ignav_id` de una búsqueda hecha con la misma cuenta. Su página de términos menciona proyectos académicos, caché/almacenamiento, atribución y redistribución, pero **no pude leer el detalle** | Leer https://ignav.com/terms y confirmar: ¿se puede guardar el precio en la base de datos más de 30 minutos? Eso afecta la regla "una copia por paquete". Si no se puede, hay que escribir a gus@ignav.com |
| AirLabs | La página principal no muestra límites ni términos | Leer https://airlabs.co/terms-of-service y las condiciones del plan gratuito |
| Xotelo | La página no publica términos de uso ni límites; el servicio es gratuito y se apoya en donaciones. Es no oficial y sus datos vienen de TripAdvisor | Leer con cuidado: al no haber términos escritos, no se puede confirmar que permita uso académico. Dejarlo anotado como riesgo |
| Open-Meteo, Wikimedia, OpenTripMap, Geoapify | Ver tabla 1 y `docs/destinos_reales.md` | Verificar cuotas de OpenTripMap y Geoapify al crear la cuenta |

## 3. Moneda y conversión a COP

| API | Moneda de respuesta | ¿Convertir a COP? |
|---|---|---|
| Ignav | Por confirmar con **una** consulta real (mercado CO). El adaptador solo acepta COP | Si responde en USD, hay que convertir en el adaptador en vez de descartar |
| Xotelo | USD (el adaptador pide siempre USD); admite USD, GBP, EUR, CAD, CHF, AUD, JPY, CNY, INR, THB, BRL, HKD, RUB, BZD | Sí, siempre (COP no está en la lista) |
| Resto | No devuelven precios | No |

Propuesta para la regla "si una API responde en otra moneda, se convierte y se muestra la moneda original":
guardar `moneda` y `precio_original` junto al precio en COP (columnas ya incluidas en `schema.sql`), con una
tasa fija configurable (por ejemplo `travelgo.moneda.usd-cop`) y la fecha de la tasa. **Decisión pendiente del equipo.**

## 4. Cuotas

| API | Cuota | Cómo se cuida |
|---|---|---|
| Ignav | 1.000 consultas gratis, una sola vez | Caché de 30 minutos como máximo, y no consultar en las pruebas automáticas (usar respuestas simuladas) |
| AirLabs | Por confirmar (el plan dice ~1.000 al mes) | Caché |
| Xotelo | No publicada | Caché y consultar solo al armar el paquete |
| Open-Meteo / Wikimedia | Sin llave | Caché ya hecha por Integrante 3 (clima 1 h, atracciones y fotos 24 h) |

## 5. Cómo se prueba cada fila

1. Hacer la consulta con curl o Postman con la llave en una variable de entorno (nunca en el repo; la cuota de Ignav es compartida y se gasta).
2. Anotar fecha, ruta o ciudad probada y qué devolvió.
3. Cambiar "Por probar" por **Pasó**, **Pasó con límites** o **No pasó**, con el motivo.
4. Lo que no pase se declara como límite del alcance en el README del sprint.
