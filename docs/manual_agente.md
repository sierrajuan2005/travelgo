# Manual del Agente — TravelGo

Versión 1.0 (Sprint 5). Autor: Integrante 3.

El **agente de viajes** atiende a los clientes: les arma reservas, ve las reservas de todos los clientes, registra sus
pagos y entrega las facturas. No administra el catálogo (eso es del Administrador).

## 1. Entrar al sistema

1. Abrir TravelGo en el navegador (en la demostración: `http://localhost:8080`).
2. Pulsar **Iniciar sesión** y escribir el correo y la contraseña de agente.
   Usuario de prueba: `agente@travelgo.com` (la contraseña está en `data.sql`).
3. En el menú superior aparecen **Inicio**, **Catálogo**, **Reservas** y **Mis avisos**.

Si la contraseña es incorrecta, el sistema vuelve a pedirla. Para salir, pulsar **Salir** arriba a la derecha.

## 2. Qué puede hacer el agente

| Tarea | Dónde |
|---|---|
| Consultar el catálogo de paquetes y su información real (clima, fotos, atracciones, mapa) | Catálogo |
| Armar una reserva | Catálogo → paquete → Reservar |
| Ver las reservas de todos los clientes | Reservas |
| Registrar el pago de una reserva (con cupón) | Reservas → Pagar |
| Ver e imprimir la factura | Reservas → Ver factura |
| Elegir cómo recibir avisos | Mis avisos |
| Consultar al asistente (chatbot) | Botón redondo abajo a la derecha |

Lo que el agente **no** puede hacer: crear o editar destinos, paquetes, servicios y cupones (Administrador).

## 3. Consultar el catálogo

1. Pulsar **Catálogo**.
2. Filtrar por destino, precio o duración.
3. Abrir un paquete para ver:
   - qué incluye (vuelo y hotel reales con su fuente, fecha de consulta y enlace),
   - el clima de los próximos días, fotos y atracciones del destino,
   - el mapa con hoteles, restaurantes y sitios turísticos.

Los precios de vuelos y hoteles son **de referencia**: siempre se muestran con la fecha en que se consultaron.

## 4. Armar una reserva

1. En el detalle del paquete, pulsar **Reservar**.
2. Elegir al menos un servicio: vuelo, hotel o transporte (solo aparecen los que llegan al destino del paquete).
3. Pulsar **Confirmar reserva**. La reserva queda **PENDIENTE** y aparece en **Reservas**.

Si no se elige ningún servicio, el sistema muestra "Elige al menos un vuelo, un hotel o un transporte".

## 5. Ver las reservas de los clientes

En **Reservas** el agente ve las reservas de **todos** los clientes, con:

- código de la reserva (8 caracteres),
- paquete y nombre del cliente,
- total y estado: **PENDIENTE** (sin pagar), **CONFIRMADA** (pagada) o **CANCELADA**.

## 6. Registrar un pago

1. En **Reservas**, pulsar **Pagar** en una reserva PENDIENTE.
2. Elegir el método: tarjeta de crédito, tarjeta débito, transferencia o efectivo.
3. Si el cliente tiene un cupón, escribirlo (por ejemplo `VERANO10`). El sistema revisa que exista, esté vigente y
   tenga usos disponibles; si no, muestra el motivo y no cobra.
4. Pulsar **Pagar ahora**. El pago es simulado: no se cobra dinero real.

Al pagar, el sistema emite la factura, aplica el descuento, confirma la reserva y, si el cliente aceptó los avisos,
le envía el correo de **pago confirmado** con el resumen de la factura.

Una reserva ya pagada o cancelada no se puede volver a pagar.

## 7. Ver e imprimir la factura

1. En **Reservas**, pulsar **Ver factura** en una reserva CONFIRMADA.
2. Se ve el número de factura, los servicios, el subtotal, el descuento del cupón y el total.
3. Pulsar **Imprimir o guardar en PDF**. La impresión sale limpia (sin menú, botones ni chatbot).

## 8. Avisos (recordatorios)

En **Mis avisos** cada usuario (también el agente) elige si recibe avisos por **correo** y/o por **mensaje de texto**
(para el SMS hay que escribir el celular). Los clientes reciben:

- cuánto falta para su viaje: 7 días, 1 día y unas horas antes,
- un aviso si una reserva sigue sin pagar a los 2 días,
- la confirmación de cada pago.

Cada aviso se envía una sola vez. Si un cliente dice que no recibe avisos, revisar con él que los haya activado en
**Mis avisos**.

## 9. Asistente (chatbot)

El botón redondo de abajo a la derecha abre el asistente. Sirve para responder rápido a un cliente. Ejemplos:

- "Paquetes baratos para Bogotá"
- "Paquetes con hoteles de lujo en Cartagena"
- "¿Qué incluye el paquete Cartagena Esencial?"
- "¿Hay cupones?" · "¿Cómo pago?"

Las preguntas "¿Cómo va mi reserva?" y "¿Tengo pagos pendientes?" responden sobre las reservas **de quien inició
sesión**. Para ver las de un cliente, usar **Reservas**.

## 10. Problemas frecuentes

| Problema | Qué hacer |
|---|---|
| "Acceso denegado" (403) | Esa página es del Administrador. Volver a **Inicio** |
| La sesión se cerró | Volver a iniciar sesión; la reserva guardada no se pierde |
| El cupón no se acepta | Revisar que esté bien escrito y vigente (el chatbot lista los cupones vigentes) |
| Un precio cambió | Los precios reales son de referencia; se muestra la fecha de la consulta |
