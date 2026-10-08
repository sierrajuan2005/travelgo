# Historias de Usuario — Núcleo del Sistema de Agencia de Viajes

## 1. Gestión de Usuarios

### Historia 1: Registro de usuario
**Como** cliente  
**quiero** registrarme en el sistema  
**para** poder realizar reservas de paquetes turísticos.

**Criterios de aceptación:**
- El sistema valida que el correo no esté registrado.
- Se guarda el usuario con rol CLIENTE.
- Se muestra mensaje de confirmación.

---

### Historia 2: Inicio de sesión
**Como** usuario registrado  
**quiero** iniciar sesión en el sistema  
**para** acceder a mis funcionalidades según mi rol.

**Criterios de aceptación:**
- El sistema valida credenciales.
- Se asignan permisos según rol (CLIENTE, AGENTE, ADMINISTRADOR).
- Se muestra el panel correspondiente al rol.

---

## 2. Catálogo de Destinos y Paquetes

### Historia 3: Consultar catálogo
**Como** cliente  
**quiero** ver los paquetes turísticos disponibles  
**para** elegir el que más me interese.

**Criterios de aceptación:**
- Se muestran nombre, descripción, precio y duración.
- Solo se listan paquetes activos.
- Se puede filtrar por destino o rango de precios.

---

### Historia 4: Gestión de paquetes
**Como** agente  
**quiero** crear, editar o eliminar paquetes turísticos  
**para** mantener actualizado el catálogo.

**Criterios de aceptación:**
- Se valida que el destino exista.
- Se actualiza la información en la base de datos.
- Se muestra mensaje de confirmación tras cada acción.

---

## 3. Reservas

### Historia 5: Crear reserva
**Como** cliente  
**quiero** reservar un paquete turístico  
**para** asegurar mi viaje.

**Criterios de aceptación:**
- Se registra la reserva con estado “pendiente”.
- Se calcula el total según el paquete.
- Se vincula la reserva al usuario y al paquete elegido.

---

### Historia 6: Confirmar reserva
**Como** agente  
**quiero** confirmar una reserva  
**para** garantizar la disponibilidad del paquete.

**Criterios de aceptación:**
- El estado cambia a “confirmada”.
- Se genera factura vinculada.
- Se notifica al cliente la confirmación.

---

## 4. Facturación y Pagos

### Historia 7: Generar factura
**Como** sistema  
**quiero** emitir una factura al confirmar una reserva  
**para** registrar el cobro al cliente.

**Criterios de aceptación:**
- La factura incluye subtotal, total y estado inicial “pendiente”.
- Se vincula con la reserva correspondiente.
- Se almacena en la base de datos.

---

### Historia 8: Registrar pago
**Como** cliente  
**quiero** pagar mi factura  
**para** completar el proceso de reserva.

**Criterios de aceptación:**
- Se registra el pago con método y estado.
- La factura cambia a “pagada” si el pago es aprobado.
- Se muestra mensaje de confirmación al cliente.

---

## 5. Observaciones
Estas historias de usuario definen el comportamiento esperado del núcleo del sistema y servirán como base para los sprints siguientes (pruebas, seeds y chatbot).
