# Investigación de transporte terrestre — TravelGo

**Responsable:** Integrante 1  
**Sprint:** 5  
**Resultado:** No se integra proveedor externo de transporte terrestre.

## 1. Objetivo

Evaluar fuentes de datos de transporte terrestre que permitan consultar rutas colombianas, horarios y precios reales, con condiciones adecuadas para un proyecto académico.

## 2. Criterios de aceptación

Una fuente se acepta si:

- Proporciona datos reales de rutas colombianas.
- Permite consultar precios y horarios.
- Permite el uso académico.
- No exige un contrato comercial.
- Tiene un costo cero o muy bajo.

## 3. Fuentes investigadas

| Fuente | Resultado |
|---|---|
| TMLand / TM Solutions | Se identificó como candidato con documentación técnica de API. Se solicitó información sobre acceso académico, pero no se recibió respuesta. No fue posible confirmar credenciales, costos ni condiciones de uso. |
| Busbud | No se confirmó acceso público autoservicio a una API para el proyecto. |
| CheckMyBus | No se confirmó acceso técnico disponible para integración académica. |
| OpenRouteService | Permite consultar distancias y duración de rutas, pero no proporciona precios ni horarios de empresas de buses. |

## 4. Decisión

No se integra un proveedor externo de transporte terrestre durante el Sprint 5.

La falta de respuesta de TMLand impide verificar los requisitos de acceso, costo y autorización académica. Por tanto, no se considera una fuente aprobada.

No se incorporan precios, horarios ni disponibilidad de buses inventados.

## 5. Alcance resultante

El transporte terrestre queda como una limitación documentada del proyecto. OpenRouteService puede utilizarse como referencia para distancia y duración de rutas, sin presentarlo como proveedor de precios o disponibilidad de buses.

## 6. Conclusión

La investigación se considera finalizada para el alcance del Sprint 5. No se implementa un adaptador de proveedor de transporte terrestre porque ninguna de las fuentes evaluadas confirmó cumplir todos los criterios de aceptación.
