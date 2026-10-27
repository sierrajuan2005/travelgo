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
    Admin --> Gestion["Destinos / Paquetes / Servicios"]
    Gestion -->|"listar, agregar, eliminar"| Admin
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
[R-001] Machu Picchu mágico - USD 1,450.00 - PENDIENTE
[R-002] Cartagena colonial - USD 980.00 - CANCELADA
[R-003] Cancún todo incluido - USD 2,100.00 - CONFIRMADA
Id de la reserva: R-001

Total a pagar: USD 1,450.00
1. Tarjeta
2. Transferencia
Elige el método de pago: 1
Pago registrado.
```

- El monto es el `total` de la reserva; el usuario no lo escribe.
- Solo se paga una reserva PENDIENTE. Si está CANCELADA o ya pagada, se muestra el motivo y se vuelve al menú:

```text
Id de la reserva: R-002
No se puede pagar una reserva cancelada.
```

### 5.4 Factura

```text
--- Factura F-0001 ---
Fecha de emisión: 27/10/2026
Reserva:          R-001 (CONFIRMADA)
Cliente:          nombre - correo
Servicios:
  Paquete Machu Picchu mágico   USD 1,200.00
  Vuelo LATAM, Lima -> Cusco    USD   250.00
Subtotal:                       USD 1,450.00
Total:                          USD 1,450.00
Pago: Tarjeta - 27/10/2026 - APROBADO
```

La factura se genera sola después del pago, y la reserva pasa a CONFIRMADA (`confirmarReserva()`).

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
Destino agregado con id d6.
```

| Opción | Listar | Agregar (datos que pide) | Eliminar |
|---|---|---|---|
| Destinos | id, nombre, ciudad, país | nombre, descripcion, pais, ciudad | por id, pide confirmación (s/n) |
| Paquetes | id, nombre, destino, precio, días, activo | nombre, descripcion, precio, duracionDias, id del destino | por id, pide confirmación (s/n) |
| Servicios | id, tipo, precio, disponible | tipo (Vuelo, Hotel o Transporte), precio y los datos de ese tipo | por id, pide confirmación (s/n) |

Datos de cada tipo de servicio: Vuelo (aerolinea, origen, destino, fechaSalida, fechaLlegada), Hotel (nombre, direccion, tipoHabitacion, noches) y Transporte (tipo, origen, destino, fecha).

### 5.6 Relación entre opciones y clases

| Opción | Clases que usa | Atributos / métodos |
|---|---|---|
| Elegir rol | Usuario, Rol | rol |
| Pagar una reserva | Pago, Reserva | metodo, monto = reserva.total, fechaPago, estado; confirmarReserva() |
| Factura | Factura, Pago, Reserva, Usuario | numero, fechaEmision, subtotal, total, estado; usuario.nombre, usuario.correo |
| Panel de administración | Destino, PaqueteTuristico, Vuelo, Hotel, Transporte | los atributos de cada clase |

## 6. Consideraciones técnicas

- Las pantallas web se construyen con Thymeleaf y Bootstrap 5.
- El pago, la factura y el panel de administración se muestran en consola (`ConsolaMaqueta`), sin Spring ni base de datos.
- Mientras no existan las entidades reales, las pantallas usan datos de ejemplo del paquete `maqueta/`, con los mismos atributos que el diagrama de clases.
