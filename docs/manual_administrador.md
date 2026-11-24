# Manual del Administrador — TravelGo

**Versión:** 1.0  
**Proyecto:** TravelGo  
**Sprint:** 5

## 1. Introducción

Este manual describe las funcionalidades del panel administrativo de TravelGo. Está dirigido a los usuarios que tienen asignado el rol ADMINISTRADOR.

El administrador es responsable de gestionar los destinos, paquetes turísticos y disponibilidad de los servicios registrados en el sistema.

## 2. Acceso al sistema

1. Iniciar TravelGo desde el navegador.
2. Ingresar a la página de inicio de sesión.
3. Introducir las credenciales de un usuario con rol ADMINISTRADOR.
4. Acceder al panel administrativo.

La ruta principal del panel es:

`/admin`

Al ingresar, el sistema redirige a la sección de destinos.

**Restricción:** las rutas `/admin/**` están protegidas mediante Spring Security y requieren el rol ADMINISTRADOR.

## 3. Gestión de destinos

Ruta: `/admin/destinos`

Desde esta sección se pueden consultar los destinos registrados y agregar nuevos.

### Agregar un destino

1. Ingresar a la sección Destinos.
2. Escribir el nombre del destino.
3. Introducir el país y la ciudad.
4. Agregar una descripción si se desea.
5. Enviar el formulario.

El nombre, país y ciudad son obligatorios.

### Eliminar un destino

1. Localizar el destino.
2. Seleccionar la opción de eliminación.
3. Confirmar la operación según la interfaz.

**Importante:** no se permite eliminar un destino que esté asociado a algún paquete turístico. Primero se debe cambiar o desactivar el paquete que lo utiliza.

## 4. Gestión de paquetes turísticos

Ruta: `/admin/paquetes`

Esta sección permite consultar los paquetes registrados y agregar nuevos.

### Crear un paquete

Se deben proporcionar los siguientes datos:

- Nombre del paquete.
- Descripción.
- Precio.
- Duración en días.
- Destino asociado.
- Estado activo o inactivo.

El sistema valida que el nombre y el destino estén definidos, que el precio sea mayor que cero y que la duración sea de al menos un día.

### Activar o desactivar paquetes

1. Buscar el paquete en el listado.
2. Seleccionar la opción para cambiar su estado.
3. El sistema invierte el estado actual.

Un paquete inactivo deja de aparecer en el catálogo público, pero permanece registrado para conservar las referencias de reservas anteriores.

## 5. Gestión de servicios

Ruta: `/admin/servicios`

Permite consultar los servicios reservables registrados:

- Vuelos.
- Hoteles.
- Transportes.

Se puede filtrar el listado por tipo de servicio.

La información mostrada incluye identificador, tipo, descripción, costo y disponibilidad.

### Cambiar disponibilidad

1. Localizar el servicio.
2. Seleccionar la opción para cambiar su disponibilidad.
3. El sistema invierte el estado entre disponible y no disponible.

Esta operación no elimina el servicio ni modifica su identificador.

## 6. Seguridad y permisos

El panel administrativo está restringido al rol ADMINISTRADOR.

| Funcionalidad | Permiso |
|---|---|
| Consultar destinos | Administrador |
| Agregar y eliminar destinos | Administrador |
| Consultar paquetes | Administrador |
| Crear paquetes | Administrador |
| Activar y desactivar paquetes | Administrador |
| Consultar servicios | Administrador |
| Cambiar disponibilidad de servicios | Administrador |

Los accesos no autorizados son rechazados por Spring Security.

## 7. Recomendaciones de administración

- Verificar los datos antes de registrar destinos y paquetes.
- Mantener precios y duraciones actualizados.
- No eliminar destinos que estén asociados a paquetes.
- Desactivar paquetes que ya no deban aparecer en el catálogo.
- Revisar la disponibilidad de los servicios antes de ofrecerlos a los clientes.
- Proteger las credenciales de acceso administrativo.

## 8. Problemas frecuentes

| Problema | Posible solución |
|---|---|
| Acceso denegado | Verificar que el usuario tenga rol ADMINISTRADOR. |
| No se puede eliminar un destino | Comprobar si está asociado a un paquete. |
| No se puede crear un paquete | Revisar los campos obligatorios, precio, duración y destino. |
| Un paquete no aparece en el catálogo | Comprobar que esté activo. |
| Un servicio no aparece como disponible | Revisar su estado desde la sección Servicios. |

## 9. Consideraciones finales

El administrador es responsable de mantener organizado y actualizado el catálogo de TravelGo. Las operaciones del panel se ejecutan mediante los servicios de negocio y se persisten en PostgreSQL.