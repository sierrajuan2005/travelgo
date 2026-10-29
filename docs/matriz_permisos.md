
# Matriz de permisos por rol - TravelGo

## Objetivo

Definir los permisos de los roles CLIENTE, AGENTE y ADMINISTRADOR
para establecer las reglas de acceso del sistema.

## Matriz de permisos

| Funcionalidad | CLIENTE | AGENTE | ADMINISTRADOR |
|---|---|---|---|
| Consultar catálogo | Sí | Sí | Sí |
| Crear reservas propias | Sí | Sí | No aplica |
| Consultar sus reservas | Sí | Sí | No aplica |
| Gestionar todas las reservas | No | Sí | No aplica |
| Realizar pagos propios | Sí | Sí | No aplica |
| Gestionar pagos | No | Sí | No aplica |
| Gestionar destinos | No | No | Sí |
| Gestionar paquetes turísticos | No | No | Sí |
| Gestionar servicios | No | No | Sí |

## Reglas de seguridad

- El usuario debe estar autenticado para acceder a las funcionalidades protegidas.
- El CLIENTE solo puede consultar y pagar sus propias reservas.
- El AGENTE puede gestionar las reservas y los pagos de los clientes.
- El ADMINISTRADOR puede gestionar el catálogo de destinos, paquetes y servicios.
- El identificador del propietario de una reserva debe obtenerse del usuario autenticado, no confiarse al enviado en la petición.

## Consideraciones

Esta matriz es una propuesta para validar con el equipo durante el Sprint Planning.
Los permisos definitivos deben reflejarse en la configuración de Spring Security
y verificarse mediante pruebas.
