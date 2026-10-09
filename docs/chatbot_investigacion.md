# Investigación Chatbot — Sistema de Agencia de Viajes

## 1. Objetivo
Diseñar un chatbot simple integrado en el sistema web, desarrollado con **Spring Boot 3**, que permita responder consultas básicas de los clientes sobre paquetes turísticos y reservas.  
El chatbot se implementará en el **Sprint 4**, únicamente si el núcleo del sistema está estable.

---

## 2. Tecnologías
- **Spring Boot 3 (Java 17+)**
- **Spring Web**: para exponer endpoints REST.
- **Spring Data JPA**: para consultar la base de datos PostgreSQL.
- **PostgreSQL**: motor de base de datos.
- **Respuestas guiadas**: reglas simples (if/else o mapa de intenciones).

---

## 3. Flujo de funcionamiento
1. El cliente escribe un mensaje en la interfaz web.
2. El `ChatbotController` recibe el mensaje y lo envía al `ChatbotService`.
3. El `ChatbotService` interpreta la intención.
4. Se consulta la base de datos con JPA si es necesario.
5. Se devuelve una respuesta guiada al cliente.

---

## 4. Ejemplos de interacciones
- **Cliente:** “Ver paquetes disponibles”
- **Bot:** “Paquete 1: Cartagena — $500. Paquete 2: Bogotá — $400.”

- **Cliente:** “Estado de mi reserva 123”
- **Bot:** “Reserva 123: Confirmada, total $500, fecha 2026-10-05.”

---

## 5. Alcance en el proyecto
- **Sprint 4:** Implementación inicial del chatbot.
    - Consultar catálogo de paquetes.
    - Consultar estado de una reserva.
- **Condicionado:** Solo se implementa si el núcleo está estable.
- **Extensiones posibles:** historial de viajes, recomendaciones personalizadas.

---

## 6. Justificación
El chatbot se integra directamente en el backend con Spring Boot, lo que asegura:
- Acceso seguro a la base de datos mediante JPA.
- Respuestas consistentes con la lógica del negocio.
- Facilidad de despliegue junto al resto de la aplicación.
