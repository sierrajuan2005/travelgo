# Aplicación de principios GRASP y SOLID – TravelGo

## 1. Objetivo

Documentar la aplicación de los principios GRASP y SOLID en el módulo de integración de vuelos reales de TravelGo, identificando las clases responsables y su contribución al diseño del sistema.

## 2. Principios GRASP

| Principio | Aplicación en TravelGo | Clases involucradas |
|---|---|---|
| Controller (Controlador) | Centraliza la recepción de solicitudes HTTP y delega las operaciones de búsqueda y consulta de enlaces al proveedor de vuelos. | `VueloRealController` |
| Information Expert (Experto en información) | La implementación del proveedor concentra la información y las operaciones necesarias para consultar Ignav, validar sus respuestas y transformar los itinerarios en objetos `Vuelo`. | `IgnavProveedorVuelos`, `IgnavRespuesta`, `Vuelo` |
| Low Coupling (Bajo acoplamiento) | El controlador depende de la interfaz `ProveedorVuelos` y no directamente de la implementación de Ignav. | `VueloRealController`, `ProveedorVuelos` |
| High Cohesion (Alta cohesión) | Cada componente mantiene responsabilidades relacionadas: el controlador gestiona solicitudes HTTP, el proveedor integra el servicio externo y la configuración construye el cliente HTTP. | `VueloRealController`, `IgnavProveedorVuelos`, `IgnavRestClientConfig` |
| Polymorphism (Polimorfismo) | La interfaz permite definir un contrato común para diferentes proveedores de vuelos, cuyas implementaciones pueden realizar las operaciones de manera diferente. | `ProveedorVuelos`, `IgnavProveedorVuelos` |
| Indirection (Indirección) | La interfaz `ProveedorVuelos` funciona como intermediaria entre el controlador y la implementación concreta del proveedor externo. | `VueloRealController`, `ProveedorVuelos`, `IgnavProveedorVuelos` |
| Protected Variations (Variaciones protegidas) | La abstracción del proveedor reduce el impacto de posibles cambios en la API externa sobre el controlador y las demás capas consumidoras. | `ProveedorVuelos`, `IgnavProveedorVuelos` |

## 3. Principios SOLID

| Principio | Aplicación en TravelGo | Clases involucradas |
|---|---|---|
| SRP – Single Responsibility Principle (Responsabilidad única) | El controlador administra los endpoints REST; el proveedor implementa la integración con Ignav; y la configuración registra el cliente HTTP. Cada clase tiene una responsabilidad principal diferenciada. | `VueloRealController`, `IgnavProveedorVuelos`, `IgnavRestClientConfig` |
| OCP – Open/Closed Principle (Abierto/Cerrado) | La interfaz permite incorporar nuevas implementaciones de proveedores de vuelos sin modificar directamente el controlador, manteniendo el contrato establecido. | `ProveedorVuelos`, `IgnavProveedorVuelos` |
| LSP – Liskov Substitution Principle (Sustitución de Liskov) | Cualquier implementación de `ProveedorVuelos` debe respetar el contrato de sus operaciones y poder utilizarse desde el controlador sin alterar el comportamiento esperado. | `ProveedorVuelos`, `IgnavProveedorVuelos` |
| ISP – Interface Segregation Principle (Segregación de interfaces) | La interfaz expone únicamente las dos operaciones necesarias para la consulta de vuelos y la obtención de enlaces de reserva, evitando métodos ajenos a este contrato. | `ProveedorVuelos` |
| DIP – Dependency Inversion Principle (Inversión de dependencias) | El controlador depende de la abstracción `ProveedorVuelos` y recibe su implementación mediante inyección de dependencias de Spring, en lugar de construir directamente un objeto `IgnavProveedorVuelos`. | `VueloRealController`, `ProveedorVuelos`, `IgnavProveedorVuelos` |

## 4. Patrones de diseño complementarios

| Patrón | Aplicación |
|---|---|
| Adapter (Adaptador) | `IgnavProveedorVuelos` adapta las solicitudes y respuestas del proveedor Ignav al contrato interno de TravelGo y transforma los itinerarios externos en objetos `Vuelo`. |
| Dependency Injection (Inyección de dependencias) | Spring administra e inyecta las dependencias necesarias para el controlador y el cliente HTTP. |
| Cache (Caché) | `IgnavProveedorVuelos` utiliza un `ConcurrentHashMap` para almacenar resultados de búsqueda durante 30 minutos, reduciendo consultas repetidas al proveedor externo. |

## 5. Beneficios obtenidos

La aplicación de estos principios y patrones aporta los siguientes beneficios al módulo:

- **Mantenibilidad:** las responsabilidades están distribuidas entre componentes especializados.
- **Extensibilidad:** la interfaz `ProveedorVuelos` permite incorporar otros proveedores de vuelos.
- **Bajo acoplamiento:** el controlador no depende directamente de la API de Ignav.
- **Reutilización:** la configuración del cliente HTTP se centraliza mediante Spring.
- **Eficiencia:** la caché reduce solicitudes repetidas al proveedor externo.
- **Adaptabilidad:** las respuestas externas se transforman al modelo interno de TravelGo.

## 6. Consideraciones

Los principios descritos corresponden al diseño y a las responsabilidades identificadas en la implementación actual. Algunos, como OCP y LSP, establecen condiciones que deben respetarse al incorporar nuevas implementaciones de proveedores; no implican que actualmente existan varios proveedores integrados.

La integración con Ignav conserva la separación entre el modelo interno de TravelGo y la estructura de datos del servicio externo.