package com.travelgo.estado;

import com.travelgo.proveedor.destinos.Coordenadas;
import com.travelgo.proveedor.destinos.Geocodificador;
import com.travelgo.proveedor.destinos.ProveedorAtracciones;
import com.travelgo.proveedor.destinos.ProveedorClima;
import com.travelgo.proveedor.destinos.ProveedorFotos;
import com.travelgo.proveedor.hotel.ProveedorHoteles;
import com.travelgo.proveedor.lugar.CategoriaMapa;
import com.travelgo.proveedor.lugar.ProveedorLugares;
import com.travelgo.proveedor.moneda.ProveedorTasaCambio;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

/**
 * "Probar ahora" del panel: una consulta real y pequeña a cada API, con Valledupar como ejemplo. Pasa por el
 * mismo adaptador (y el mismo proxy) que usa la app, así que queda medida como cualquier otra llamada.
 *
 * Ignav no se prueba nunca: tiene 1.000 consultas compartidas que no se recuperan.
 */
@Service
public class SondasProveedores {

    private static final Coordenadas VALLEDUPAR = new Coordenadas(10.4631, -73.2532);
    /** Clave de Cartagena en Xotelo (la misma de CiudadesMapa). */
    private static final String XOTELO_CARTAGENA = "g297476";

    private final Map<String, Runnable> sondas;

    public SondasProveedores(ObjectProvider<ProveedorClima> clima, ObjectProvider<Geocodificador> geocodificador,
                             ObjectProvider<ProveedorFotos> fotos, ObjectProvider<ProveedorAtracciones> atracciones,
                             ObjectProvider<ProveedorHoteles> hoteles, ObjectProvider<ProveedorLugares> lugares,
                             ObjectProvider<ProveedorTasaCambio> tasaCambio) {
        // ProveedorAtracciones es Wikipedia u OpenTripMap según haya llave: se prueba el que esté activo
        this.sondas = Map.of(
                "open-meteo-clima", () -> clima.getObject().consultarClima(VALLEDUPAR),
                "open-meteo-ubicacion", () -> geocodificador.getObject().ubicar("Valledupar", "Colombia"),
                "wikipedia-fotos", () -> fotos.getObject().buscarFoto("Valledupar"),
                "wikipedia-atracciones", () -> atracciones.getObject().buscarAtracciones(VALLEDUPAR, 3),
                "opentripmap", () -> atracciones.getObject().buscarAtracciones(VALLEDUPAR, 3),
                "xotelo", () -> hoteles.getObject().buscarHoteles(XOTELO_CARTAGENA, 1),
                "geoapify", () -> lugares.getObject().buscar(CategoriaMapa.RESTAURANTE,
                        VALLEDUPAR.latitud(), VALLEDUPAR.longitud(), 1000, 1),
                "tasa-cambio", () -> tasaCambio.getObject().obtenerUsdACop());
    }

    /**
     * Prueba una API. Si falla no lanza nada: el resultado queda en el registro y el panel lo muestra.
     *
     * @return false si esa API no se puede probar (Ignav) o no existe
     */
    public boolean probar(String fuenteId) {
        Runnable sonda = sondas.get(fuenteId);
        boolean sePuede = FuenteMonitoreada.porId(fuenteId).map(FuenteMonitoreada::sePuedeProbar).orElse(false);
        if (sonda == null || !sePuede) {
            return false;
        }
        try {
            sonda.run();
        } catch (RuntimeException e) {
            // Ya quedó registrado por el MonitorProveedores
        }
        return true;
    }

    /** Prueba todas las APIs activas que se pueden probar. */
    public void probarTodas(Collection<String> activas) {
        activas.forEach(this::probar);
    }
}
