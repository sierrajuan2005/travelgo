package com.travelgo.proveedor.lugar;

import com.travelgo.exception.ProveedorNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Pruebas del adaptador de Geoapify con respuestas simuladas (no llaman a internet). */
class GeoapifyProveedorLugaresTest {

    private static final String LLAVE = "LLAVE_SECRETA";
    private static final String RESPUESTA = """
            {"type":"FeatureCollection","features":[
              {"type":"Feature","properties":{"name":"La Cevichería","formatted":"La Cevichería, Cartagena",
                "place_id":"p1","lat":10.4,"lon":-75.55,"website":"https://ceviche.example",
                "categories":["catering.restaurant"]},"geometry":{"type":"Point","coordinates":[-75.55,10.4]}},
              {"type":"Feature","properties":{"formatted":"Sin nombre","place_id":"p2","lat":10.41,"lon":-75.56},
                "geometry":{"type":"Point","coordinates":[-75.56,10.41]}}]}
            """;

    private MockRestServiceServer servidor;
    private RestClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.geoapify.com/v2");
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = builder.build();
    }

    @Test
    @DisplayName("Convierte la respuesta en lugares y descarta los que no tienen nombre")
    void convierteLosLugares() {
        servidor.expect(requestTo(containsString("/places")))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));

        List<LugarMapa> lugares = new GeoapifyProveedorLugares(cliente, LLAVE)
                .buscar(CategoriaMapa.RESTAURANTE, 10.39, -75.47, 3000, 20);

        assertEquals(1, lugares.size());
        assertEquals("La Cevichería", lugares.get(0).nombre());
        assertEquals(CategoriaMapa.RESTAURANTE, lugares.get(0).categoria());
        assertEquals("https://ceviche.example", lugares.get(0).enlace());
    }

    @Test
    @DisplayName("La segunda consulta igual sale de la caché y no llama a la API")
    void usaLaCache() {
        servidor.expect(requestTo(containsString("/places")))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));
        GeoapifyProveedorLugares proveedor = new GeoapifyProveedorLugares(cliente, LLAVE);

        proveedor.buscar(CategoriaMapa.CAFE, 10.39, -75.47, 3000, 20);
        proveedor.buscar(CategoriaMapa.CAFE, 10.39, -75.47, 3000, 20);

        servidor.verify();
    }

    @Test
    @DisplayName("Sin llave lanza ProveedorNoDisponibleException y no llama a la API")
    void sinLlave() {
        GeoapifyProveedorLugares proveedor = new GeoapifyProveedorLugares(cliente, "");

        assertThrows(ProveedorNoDisponibleException.class,
                () -> proveedor.buscar(CategoriaMapa.MUSEO, 10.39, -75.47, 3000, 20));
        servidor.verify();
    }

    @Test
    @DisplayName("Si la API falla, lanza ProveedorNoDisponibleException sin mostrar la llave")
    void errorDeLaApi() {
        servidor.expect(requestTo(containsString("/places")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        ProveedorNoDisponibleException e = assertThrows(ProveedorNoDisponibleException.class,
                () -> new GeoapifyProveedorLugares(cliente, LLAVE)
                        .buscar(CategoriaMapa.RESTAURANTE, 10.39, -75.47, 3000, 20));

        assertFalse(e.getMessage().contains(LLAVE));
    }

    @Test
    @DisplayName("Los hoteles pueden consultarse en Geoapify como alternativa")
    void hotelAlternativo() {
        servidor.expect(requestTo(containsString("categories=accommodation.hotel")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertThrows(ProveedorNoDisponibleException.class,
                () -> new GeoapifyProveedorLugares(cliente, LLAVE)
                        .buscar(CategoriaMapa.HOTEL, 10.39, -75.47, 3000, 20));
    }
}
