package com.travelgo.expo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Acceso a los viajes lanzados en la ExpoSoftware. */
public interface LanzamientoDAO {

    /**
     * Guarda un lanzamiento visible y devuelve su id (creciente: la pantalla pide "los nuevos desde el id X").
     *
     * @param invitadoPor id del viaje que le compartió la postal; null si llegó por el QR del stand
     */
    long guardar(String nombre, String destinoId, LocalDateTime fecha, Long invitadoPor);

    Optional<Lanzamiento> buscar(long id);

    /** Los últimos {@code limite} lanzamientos visibles, del más viejo al más nuevo. */
    List<Lanzamiento> recientesVisibles(int limite);

    /** Lanzamientos visibles con id mayor que {@code desdeId}, del más viejo al más nuevo. */
    List<Lanzamiento> visiblesDesde(long desdeId, int limite);

    /** Los últimos lanzamientos, visibles u ocultos, del más nuevo al más viejo (panel del administrador). */
    List<Lanzamiento> ultimos(int limite);

    long contarVisibles();

    /** Cuántas personas lanzaron un viaje a ese destino (visibles). */
    long contarVisiblesDe(String destinoId);

    /** Oculta o vuelve a mostrar un lanzamiento. Devuelve false si el id no existe. */
    boolean cambiarOculto(long id, boolean oculto);

    /** Guarda el celular autorizado por el visitante. telefono null = borrarlo. Devuelve false si el id no existe. */
    boolean guardarContacto(long id, String telefono, LocalDateTime fecha);

    /** El visitante abrió la página de su destino. */
    void marcarVioDestino(long id);

    /** El visitante compartió (o descargó) su postal. */
    void marcarCompartioPostal(long id);

    /** El visitante abrió un paquete desde la página de su destino. */
    void marcarAbrioPaquete(long id);

    /** Registra un WhatsApp enviado (o un nuevo intento) con su id en el proveedor. */
    void guardarWhatsApp(long id, String idMensaje, String estado, LocalDateTime fecha);

    void actualizarEstadoWhatsApp(long id, String estado);

    /** El último WhatsApp enviado a ese viaje; vacío si nunca se envió. */
    Optional<EnvioWhatsApp> envioWhatsApp(long id);

    /** Cuántos visitantes llegaron a cada paso del recorrido (solo lanzamientos visibles). */
    Embudo embudo();

    /** WhatsApp enviado a un viaje: id del mensaje en el proveedor, estado ({@link EstadoWhatsApp}) y fecha. */
    record EnvioWhatsApp(String idMensaje, String estado, LocalDateTime fecha) {
    }

    /**
     * Embudo de conversión del stand: lanzaron un viaje → vieron su destino → compartieron su postal →
     * dejaron su celular → recibieron su viaje por WhatsApp → abrieron un paquete de TravelGo.
     * {@code llegaronPorPostal}: cuántos lanzaron su viaje desde la postal que les compartió otro visitante.
     */
    record Embudo(long lanzaron, long vieronDestino, long compartieronPostal, long dejaronContacto,
                  long recibieronWhatsApp, long abrieronPaquete, long llegaronPorPostal) {

        /** Porcentaje de {@code parte} sobre los que lanzaron (0 si nadie ha lanzado). */
        public int porcentaje(long parte) {
            return lanzaron == 0 ? 0 : (int) Math.round(100.0 * parte / lanzaron);
        }
    }
}
