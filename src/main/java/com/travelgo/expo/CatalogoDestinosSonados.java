package com.travelgo.expo;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Lista fija de destinos para "Lanza tu viaje". No sale de la tabla {@code destino} a propósito:
 * el visitante elige su viaje soñado (Tokio, Dubái...) aunque TravelGo todavía no venda paquetes allí,
 * y cada destino necesita coordenadas para el globo. Los que sí tienen paquetes llevan el filtro del catálogo.
 */
@Component
public class CatalogoDestinosSonados {

    private static final String COLOMBIA = "Colombia";
    private static final String AMERICA = "América";
    private static final String EUROPA = "Europa";
    private static final String MUNDO = "Asia, África y Oceanía";

    private final List<DestinoSonado> destinos = List.of(
            new DestinoSonado("cartagena", "Cartagena", "Colombia", COLOMBIA, 10.391, -75.479, "Cartagena", "CTG"),
            new DestinoSonado("santa-marta", "Santa Marta", "Colombia", COLOMBIA, 11.241, -74.199, "Santa Marta", "SMR"),
            new DestinoSonado("san-andres", "San Andrés", "Colombia", COLOMBIA, 12.585, -81.701, null, "ADZ"),
            new DestinoSonado("medellin", "Medellín", "Colombia", COLOMBIA, 6.244, -75.581, "Medellín", "MDE"),
            new DestinoSonado("bogota", "Bogotá", "Colombia", COLOMBIA, 4.711, -74.072, "Bogotá", "BOG"),
            new DestinoSonado("cusco", "Cusco", "Perú", AMERICA, -13.532, -71.968, "Cusco", "CUZ"),
            new DestinoSonado("cancun", "Cancún", "México", AMERICA, 21.162, -86.852, "Cancún", "CUN"),
            new DestinoSonado("ciudad-de-mexico", "Ciudad de México", "México", AMERICA, 19.433, -99.133, null, "MEX"),
            new DestinoSonado("punta-cana", "Punta Cana", "República Dominicana", AMERICA, 18.560, -68.373, "Punta Cana", "PUJ"),
            new DestinoSonado("miami", "Miami", "Estados Unidos", AMERICA, 25.762, -80.192, "Miami", "MIA"),
            new DestinoSonado("nueva-york", "Nueva York", "Estados Unidos", AMERICA, 40.713, -74.006, null, "JFK"),
            new DestinoSonado("rio", "Río de Janeiro", "Brasil", AMERICA, -22.907, -43.173, null, "GIG"),
            new DestinoSonado("buenos-aires", "Buenos Aires", "Argentina", AMERICA, -34.604, -58.382, null, "EZE"),
            new DestinoSonado("madrid", "Madrid", "España", EUROPA, 40.417, -3.704, "Madrid", "MAD"),
            new DestinoSonado("paris", "París", "Francia", EUROPA, 48.857, 2.352, null, "CDG"),
            new DestinoSonado("londres", "Londres", "Reino Unido", EUROPA, 51.507, -0.128, null, "LHR"),
            new DestinoSonado("roma", "Roma", "Italia", EUROPA, 41.903, 12.496, null, "FCO"),
            new DestinoSonado("el-cairo", "El Cairo", "Egipto", MUNDO, 30.044, 31.236, null, "CAI"),
            new DestinoSonado("dubai", "Dubái", "Emiratos Árabes Unidos", MUNDO, 25.205, 55.271, null, "DXB"),
            new DestinoSonado("tokio", "Tokio", "Japón", MUNDO, 35.676, 139.650, null, "HND"),
            new DestinoSonado("seul", "Seúl", "Corea del Sur", MUNDO, 37.567, 126.978, null, "ICN"),
            new DestinoSonado("sidney", "Sídney", "Australia", MUNDO, -33.869, 151.209, null, "SYD")
    );

    public List<DestinoSonado> todos() {
        return destinos;
    }

    public Optional<DestinoSonado> buscar(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return destinos.stream().filter(d -> d.id().equals(id)).findFirst();
    }
}
