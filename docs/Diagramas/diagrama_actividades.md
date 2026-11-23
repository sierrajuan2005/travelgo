# Diagramas de actividades — TravelGo (Sprint 5, Integrante 3)

Dos actividades principales del sistema: **reservar y pagar un paquete** (cliente o agente) y **enviar recordatorios**
(servicio programado). Se dibujan con carriles (quién hace cada paso) usando Mermaid; en el documento final se pueden
pasar a UML con la misma forma.

## 1. Reservar y pagar un paquete

```mermaid
flowchart TD
    subgraph U["Cliente o Agente"]
        I((Inicio)) --> A1[Iniciar sesión]
        A2[Ver el catálogo y filtrar<br/>por destino, precio o duración]
        A3[Abrir el detalle del paquete<br/>clima, fotos, atracciones, mapa]
        A4[Elegir vuelo, hotel y/o transporte]
        A6[Corregir la elección]
        A8[Ir a Mis reservas y pulsar Pagar]
        A9[Elegir método de pago<br/>y escribir un cupón opcional]
        A11[Corregir o quitar el cupón]
        A13[Ver o imprimir la factura en PDF]
        F((Fin))
    end

    subgraph S["Sistema TravelGo"]
        S1{¿Credenciales válidas?}
        S2{¿Eligió al menos un servicio<br/>de la oferta del paquete?}
        S3[Guardar la reserva PENDIENTE]
        S4{¿El cupón existe, está vigente<br/>y tiene usos?}
        S5[Emitir la factura con el descuento<br/>registrar el pago<br/>marcar la factura PAGADA<br/>confirmar la reserva<br/>gastar un uso del cupón]
    end

    subgraph R["Recordatorios"]
        R1[Aviso de pago confirmado<br/>con el resumen de la factura]
    end

    A1 --> S1
    S1 -- No --> A1
    S1 -- Sí --> A2 --> A3 --> A4 --> S2
    S2 -- No --> A6 --> A4
    S2 -- Sí --> S3 --> A8 --> A9 --> S4
    S4 -- No --> A11 --> A9
    S4 -- "Sí o sin cupón" --> S5
    S5 --> R1
    S5 --> A13 --> F
```

Notas:
- El **agente** ve las reservas de todos los clientes y puede pagar cualquiera; el **cliente** solo las suyas
  (`docs/matriz_permisos.md`). El dueño de la reserva sale de la sesión, no del formulario.
- El pago es simulado y se guarda en una sola transacción: todo o nada.
- Una reserva PENDIENTE que no se paga en 2 días recibe el aviso de pago pendiente (actividad 2).

## 2. Enviar recordatorios (cada 5 minutos)

```mermaid
flowchart TD
    I((Cada 5 minutos<br/>o botón Revisar ahora)) --> C1{¿Hay algún canal<br/>con credenciales?}
    C1 -- No --> F((Fin))
    C1 -- Sí --> C2[Tomar la siguiente reserva]
    C2 --> C3{¿Está cancelada?}
    C3 -- Sí --> C9
    C3 -- No --> C4[Calcular la salida del viaje<br/>primer vuelo, transporte o entrada al hotel]
    C4 --> C5[Decidir los avisos que tocan<br/>7 días · 1 día · unas horas<br/>pago pendiente · pago confirmado]
    C5 --> C6{Para cada aviso y canal:<br/>¿el usuario aceptó ese canal<br/>y el aviso no se ha enviado?}
    C6 -- No --> C9
    C6 -- Sí --> C7[Enviar por correo o SMS]
    C7 --> C8{¿Se entregó?}
    C8 -- Sí --> C8a[Registrar ENVIADO]
    C8 -- No --> C8b[Registrar FALLIDO<br/>se reintenta en la próxima revisión]
    C8a --> C9{¿Quedan reservas?}
    C8b --> C9
    C9 -- Sí --> C2
    C9 -- No --> F
```

Notas:
- El registro (`envio_recordatorio`, `UNIQUE (reserva_id, tipo, canal)`) impide repetir un aviso.
- Detalle del diseño en `docs/recordatorios.md`.
