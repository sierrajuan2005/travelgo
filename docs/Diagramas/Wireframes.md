# Wireframes y Flujo de Pantallas - TravelGo

## 1. Descripción

Este documento presenta los wireframes de las pantallas principales de TravelGo (inicio, login, registro, catálogo, detalle del paquete y reserva) y el flujo de navegación entre ellas.

Cada pantalla usa los atributos definidos en el [Diagrama de Clases](DiagramaDeClases.md) y en el [Modelo Entidad-Relación](ModeloER.md).

## 2. Flujo de pantallas

```mermaid
flowchart LR
    Inicio["Inicio"]
    Login["Login"]
    Registro["Registro"]
    Catalogo["Catálogo"]
    Detalle["Detalle del paquete"]
    Reserva["Reserva"]

    Inicio --> Catalogo
    Inicio --> Login
    Login -->|"¿No tienes cuenta?"| Registro
    Registro -->|"registro correcto"| Login
    Catalogo -->|"Ver detalle"| Detalle
    Detalle -->|"Reservar"| Sesion{"¿Sesión iniciada?"}
    Sesion -->|"sí"| Reserva
    Sesion -->|"no"| Login
    Login -->|"vuelve a la página pedida"| Reserva
    Reserva -->|"Confirmar reserva"| Catalogo
```

- **Pantallas públicas:** inicio, login, registro, catálogo y detalle.
- **Pantalla privada:** reserva. Si el usuario no ha iniciado sesión, va al login y después vuelve a la reserva.

## 3. Wireframes

### 3.1 Login

```text
+------------------------------------------+
| TravelGo        Inicio  Catálogo  Login  |
+------------------------------------------+
|            Iniciar sesión                |
|   Correo      [______________________]   |
|   Contraseña  [______________________]   |
|               [      Ingresar       ]    |
|   ¿No tienes cuenta? Regístrate          |
+------------------------------------------+
```

### 3.2 Registro

```text
+------------------------------------------+
|              Crear cuenta                |
|   Nombre               [______________]  |
|   Correo               [______________]  |
|   Contraseña           [______________]  |
|   Confirmar contraseña [______________]  |
|               [    Registrarme    ]      |
+------------------------------------------+
```

Los campos corresponden a `Usuario` (nombre, correo, password). El rol se asigna como `CLIENTE`.

### 3.3 Catálogo

```text
+------------------------------------------+
|  Paquetes turísticos                     |
|  Destino [______] Mín [___] Máx [___]   |
|  [Filtrar] [X]                           |
|  +------------+ +------------+ +-------+ |
|  | Nombre     | | Nombre     | | ...   | |
|  | Ciudad, País| | Ciudad, País|        | |
|  | N días     | | N días     | |       | |
|  | Descripción| | Descripción| |       | |
|  | USD precio | | USD precio | |       | |
|  | [Ver detalle]| [Ver detalle]|       | |
|  +------------+ +------------+ +-------+ |
+------------------------------------------+
```

Solo se muestran los paquetes con `activo = true`. Se pueden filtrar por destino (ciudad o país) y por rango de precios (Historia 3).

### 3.4 Detalle del paquete

```text
+------------------------------------------+
| Catálogo > Nombre del paquete            |
| +----------------------+ +-------------+ |
| | Nombre               | | Precio      | |
| | Ciudad, País         | | USD precio  | |
| | Descripción          | | N días      | |
| | Sobre el destino:    | | [Reservar]  | |
| | nombre y descripción | +-------------+ |
| +----------------------+                 |
+------------------------------------------+
```

### 3.5 Reserva

```text
+------------------------------------------+
| Arma tu reserva                          |
| +----------------------+ +-------------+ |
| | Vuelo                | | Resumen     | |
| | ( ) aerolínea, origen| | Paquete     | |
| |     → destino, fechas| | Extras      | |
| | ( ) Sin vuelo        | | Total       | |
| | Hotel                | | [Confirmar] | |
| | ( ) nombre, noches   | +-------------+ |
| | ( ) Sin hotel        |                 |
| | Transporte           |                 |
| | ( ) tipo, fecha      |                 |
| | ( ) Sin transporte   |                 |
| +----------------------+                 |
+------------------------------------------+
```

El total se actualiza al elegir cada opción: precio del paquete + `calcularPrecio()` de cada servicio elegido.

## 4. Relación entre pantallas y clases

| Pantalla | Clases que usa | Atributos / métodos |
|---|---|---|
| Login | Usuario | correo, password |
| Registro | Usuario, Rol | nombre, correo, password, rol = CLIENTE |
| Catálogo | PaqueteTuristico, Destino | nombre, descripcion, precio, duracionDias, activo, destino.ciudad, destino.pais |
| Detalle | PaqueteTuristico, Destino | los del catálogo + destino.nombre, destino.descripcion |
| Reserva | Reserva, Vuelo, Hotel, Transporte | aerolinea, origen, destino, fechaSalida, fechaLlegada; nombre, direccion, tipoHabitacion, noches; tipo, fecha; calcularPrecio() |

## 5. Versión en consola

Para la presentación del primer modelo, el catálogo, el detalle y la reserva también se muestran en consola con la clase `ConsolaMaqueta`. Desde el Sprint 3, el pago, la factura y el panel de administración se muestran solo en consola.

### 5.1 Flujo

```mermaid
flowchart TD
    Rol["Elegir rol"] --> Menu["Menú principal"]
    Menu -->|"1"| Catalogo["Ver catálogo"]
    Menu -->|"2"| Detalle["Ver detalle de un paquete"]
    Menu -->|"3"| Reserva["Armar una reserva"]
    Menu -->|"4"| Buscar["Buscar por destino o precio"]
    Menu -->|"5 (Cliente o Agente)"| Pago["Pagar una reserva"]
    Menu -->|"6 (Administrador)"| Admin["Panel de administración"]
    Buscar --> Menu
    Menu -->|"0"| Salir["Salir"]
    Detalle --> Id["Pedir id del paquete"]
    Reserva --> Id
    Id -->|"reserva"| Servicios["Elegir vuelo, hotel y transporte"]
    Servicios --> Resumen["Resumen con el total"]
    Resumen -->|"se guarda como PENDIENTE"| Menu
    Pago --> Valida{"¿Reserva pendiente?"}
    Valida -->|"no: cancelada o ya pagada"| Menu
    Valida -->|"sí"| Metodo["Elegir método de pago"]
    Metodo --> Factura["Factura"]
    Factura -->|"reserva CONFIRMADA"| Menu
    Admin --> Gestion["Destinos / Paquetes"]
    Gestion -->|"listar, agregar, eliminar"| Admin
    Admin --> ServiciosAdmin["Servicios de un paquete"]
    ServiciosAdmin -->|"cambiar disponibilidad"| Admin
    Admin -->|"0"| Menu
    Catalogo --> Menu
    Detalle --> Menu
```

Mientras Integrante 1 no termine el login por rol, la consola pregunta el rol al iniciar. Ese rol decide qué opciones aparecen en el menú (`RolUsuario`: CLIENTE, AGENTE, ADMINISTRADOR).

### 5.2 Menú principal

```text
=== TravelGo ===
¿Con qué rol entras?
1. Cliente
2. Agente
3. Administrador
Opción: 1

=== TravelGo (CLIENTE) ===
1. Ver catálogo
2. Ver detalle de un paquete
3. Armar una reserva
4. Buscar paquetes por destino o precio
5. Pagar una reserva
0. Salir
Opción: 5
```

El Administrador ve la opción `6. Panel de administración` en lugar de la 5.

### 5.3 Pagar una reserva

```text
--- Reservas ---
1. [5e5eeebd] Machu Picchu mágico - USD 260.00 - PENDIENTE
2. [f294e4ca] Cartagena colonial - USD 0.00 - CANCELADA
3. [f3fc3a6a] Cancún todo incluido - USD 630.00 - CONFIRMADA
Elige una reserva: 1

Total a pagar: USD 260.00
1. Tarjeta de crédito
2. Tarjeta de débito
3. Transferencia
4. Efectivo
Elige el método de pago: 2
Pago registrado.
```

- La consola usa `PagoService` y `FacturaService` de Integrante 2 (las mismas reglas que `/api/pagos` y `/api/facturas`), armados sin Spring en `BackendConsola`.
- El id de la reserva es un UUID; la consola muestra sus primeros 8 caracteres y se elige la reserva por número.
- El monto es el total de la reserva (`Reserva.calcularTotal()`); el usuario no lo escribe. Por ahora ese total solo suma los servicios, no el precio del paquete.
- Si la reserva ya está pagada, se muestra su factura. Si está CANCELADA o no tiene servicios, se muestra el motivo que da `PagoService` y se vuelve al menú:

```text
Elige una reserva: 2
...
No se pudo pagar: No se puede confirmar una reserva cancelada.
```

### 5.4 Factura

```text
--- Factura FAC-93F951B7 ---
Fecha de emisión: 28/10/2026
Reserva:          5e5eeebd (CONFIRMADA)
Cliente:          Cliente de prueba - cliente@travelgo.com
Paquete:          Machu Picchu mágico
Servicios:
  Vuelo LATAM, Bogotá -> Cusco           USD     260.00
Total:                                   USD     260.00
Pago: Tarjeta de débito - 28/10/2026 21:07
```

La factura se genera sola después del pago (`new Factura(pago)`), y la reserva pasa a CONFIRMADA (`confirmarReserva()`).

### 5.5 Panel de administración

```text
=== Panel de administración ===
1. Destinos
2. Paquetes
3. Servicios
0. Volver
Opción: 1

--- Destinos ---
1. Listar
2. Agregar
3. Eliminar
0. Volver
Opción: 2
Nombre: Arequipa
Descripción: La ciudad blanca del sur del Perú.
País: Perú
Ciudad: Arequipa
Destino agregado con id d7.
```

| Opción | Listar | Agregar (datos que pide) | Eliminar |
|---|---|---|---|
| Destinos | id, nombre, ciudad, país | nombre, descripcion, pais, ciudad | por id, pide confirmación (s/n) |
| Paquetes | id, nombre, destino, precio, días, activo | nombre, descripcion, precio, duracionDias, id del destino | por id, pide confirmación (s/n) |

**Servicios** es la versión básica: el Administrador elige un paquete, ve sus vuelos, hoteles y transportes, y cambia la disponibilidad por id. Los servicios no disponibles no aparecen al armar una reserva. Agregar y eliminar servicios se hará con el CRUD de `/api/servicios` de Integrante 2.

```text
--- Servicios de Machu Picchu mágico ---
[v1] Vuelo Avianca, Bogotá -> Cusco - USD 320.00 - disponible
[v2] Vuelo LATAM, Bogotá -> Cusco - USD 260.00 - disponible
[h1] Hotel Centro Cusco, 5 noches - USD 450.00 - disponible
...
Id del servicio para cambiar su disponibilidad (Enter = volver): v1
El servicio v1 ahora está no disponible.
```

### 5.6 Relación entre opciones y clases

| Opción | Clases que usa | Atributos / métodos |
|---|---|---|
| Elegir rol | Usuario, Rol | rol |
| Pagar una reserva | Pago, MetodoPago, Reserva, PagoService | metodo, monto = reserva.total, fechaPago; pagar(), confirmarReserva() |
| Factura | Factura, Pago, Reserva, Usuario, FacturaService | numero, fechaEmision, total; usuario.nombre, usuario.correo; obtenerPorReserva() |
| Panel de administración | Destino, PaqueteTuristico, Vuelo, Hotel, Transporte | atributos de Destino y PaqueteTuristico; disponible de cada servicio |

## 6. Consideraciones técnicas

- Las pantallas web se construyen con Thymeleaf y Bootstrap 5.
- El pago, la factura y el panel de administración se muestran en consola (`ConsolaMaqueta`), sin Spring ni base de datos.
- Las reservas, los pagos y las facturas de la consola usan las clases y los services reales, con los DAO en memoria (`BackendConsola`).
- El catálogo y el panel de administración todavía usan datos de ejemplo del paquete `maqueta/`, con los mismos atributos que el diagrama de clases.
