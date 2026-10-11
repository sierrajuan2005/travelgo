# Modelo Entidad-Relación (MER) - TravelGo

## 1. Descripción

El modelo entidad-relación de TravelGo representa la estructura de la base de datos del sistema de gestión de una agencia de viajes. Define las tablas, sus atributos, claves primarias, claves foráneas y relaciones.

El modelo contempla la gestión de usuarios, destinos turísticos, paquetes, reservas, servicios reservables, facturas y pagos.

## 2. Modelo de datos


```dbml
Table Usuario {
  id varchar(36) [pk]
  nombre varchar(100) [not null]
  correo varchar(150) [not null, unique]
  password varchar(255) [not null]
  rol varchar(20) [not null, note: 'CLIENTE, AGENTE, ADMINISTRADOR']
}

Table Destino {
  id varchar(36) [pk]
  nombre varchar(100) [not null]
  descripcion text
  pais varchar(100) [not null]
  ciudad varchar(100) [not null]
}

Table PaqueteTuristico {
  id varchar(36) [pk]
  nombre varchar(150) [not null]
  descripcion text
  precio decimal(10,2) [not null]
  duracionDias int [not null]
  activo boolean [not null, default: true]
  destino_id varchar(36) [not null]
}

Table Reserva {
  id varchar(36) [pk]
  fechaReserva date [not null]
  estado varchar(30) [not null]
  total decimal(10,2) [not null]
  usuario_id varchar(36) [not null]
  paquete_id varchar(36)
}

Table ServicioReservable {
  id varchar(36) [pk]
  precio decimal(10,2) [not null]
  disponible boolean [not null, default: true]
  tipo varchar(30) [not null, note: 'VUELO, HOTEL, TRANSPORTE']
}

Table Vuelo {
  id varchar(36) [pk]
  aerolinea varchar(100) [not null]
  origen varchar(100) [not null]
  destino varchar(100) [not null]
  fechaSalida datetime [not null]
  fechaLlegada datetime [not null]
}

Table Hotel {
  id varchar(36) [pk]
  nombre varchar(150) [not null]
  direccion varchar(200) [not null]
  tipoHabitacion varchar(100) [not null]
  noches int [not null]
}

Table Transporte {
  id varchar(36) [pk]
  tipo varchar(100) [not null]
  origen varchar(100) [not null]
  destino varchar(100) [not null]
  fecha datetime [not null]
}

Table ReservaServicio {
  reserva_id varchar(36) [not null]
  servicio_id varchar(36) [not null]

  indexes {
    (reserva_id, servicio_id) [pk]
  }
}

Table Factura {
  id varchar(36) [pk]
  numero varchar(50) [not null, unique]
  fechaEmision date [not null]
  subtotal decimal(10,2) [not null]
  total decimal(10,2) [not null]
  estado varchar(30) [not null]
  reserva_id varchar(36) [not null, unique]
}

Table Pago {
  id varchar(36) [pk]
  monto decimal(10,2) [not null]
  fechaPago date [not null]
  metodo varchar(50) [not null]
  estado varchar(30) [not null]
  factura_id varchar(36) [not null]
}

// Relaciones principales

Ref: PaqueteTuristico.destino_id > Destino.id

Ref: Reserva.usuario_id > Usuario.id

Ref: Reserva.paquete_id > PaqueteTuristico.id

Ref: ReservaServicio.reserva_id > Reserva.id

Ref: ReservaServicio.servicio_id > ServicioReservable.id

Ref: Vuelo.id - ServicioReservable.id

Ref: Hotel.id - ServicioReservable.id

Ref: Transporte.id - ServicioReservable.id

Ref: Factura.reserva_id - Reserva.id

Ref: Pago.factura_id > Factura.id
```

## 3. Descripción de las entidades

| Entidad | Responsabilidad |
|---|---|
| Usuario | Almacenar los datos de los usuarios y su rol dentro del sistema. |
| Destino | Registrar los destinos turísticos disponibles. |
| PaqueteTuristico | Almacenar los paquetes turísticos, sus precios y duración. |
| Reserva | Registrar las reservas realizadas por los usuarios. |
| ServicioReservable | Almacenar los datos comunes de los servicios reservables. |
| Vuelo | Registrar los datos específicos de los servicios de vuelo. |
| Hotel | Registrar la información de los servicios de alojamiento. |
| Transporte | Registrar los servicios de transporte. |
| ReservaServicio | Relacionar las reservas con los servicios incluidos. |
| Factura | Almacenar la información de facturación de una reserva. |
| Pago | Registrar los pagos asociados a una factura. |

## 4. Relaciones entre las entidades

- **Destino y PaqueteTuristico:** cada paquete referencia un destino mediante `destino_id`.
- **Usuario y Reserva:** cada reserva pertenece a un usuario mediante `usuario_id`.
- **PaqueteTuristico y Reserva:** una reserva puede estar asociada a un paquete mediante `paquete_id`.
- **Reserva y ServicioReservable:** se relacionan mediante la tabla intermedia `ReservaServicio`.
- **ServicioReservable y Vuelo, Hotel y Transporte:** estas tablas comparten el identificador del servicio para representar los distintos tipos de servicio reservable.
- **Reserva y Factura:** cada factura referencia una reserva y el campo `reserva_id` es único.
- **Factura y Pago:** cada pago referencia una factura mediante `factura_id`.

## 5. Claves y restricciones

- **Claves primarias (PK):** identifican de manera única cada registro.
- **Claves foráneas (FK):** establecen relaciones entre las tablas.
- **Restricción `not null`:** exige que el atributo tenga un valor.
- **Restricción `unique`:** impide valores duplicados en el atributo correspondiente.
- **Tabla intermedia:** `ReservaServicio` utiliza una clave primaria compuesta por `reserva_id` y `servicio_id`.

## 6. Consideraciones técnicas

El modelo está diseñado como base para la implementación de la persistencia relacional de TravelGo. Su implementación deberá mantener la correspondencia entre las entidades de la base de datos y las clases del dominio desarrolladas en Java.

