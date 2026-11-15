package com.travelgo.proveedor.hotel;

import com.travelgo.exception.ProveedorNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Pruebas del adaptador de Xotelo con respuestas simuladas (no llaman a internet). */
class XoteloProveedorHotelesTest {

    private static final String TARIFAS = """
            {"error": null,
             "result": {"chk_in": "2026-12-10", "chk_out": "2026-12-13", "currency": "USD",
               "rates": [
                 {"code": "BookingCom", "name": "Booking.com", "rate": 589, "tax": null},
                 {"code": "Accor", "name": "Sofitel", "rate": 589, "tax": null},
                 {"code": "CtripTA", "name": "Trip.com", "rate": 495, "tax": null},
                 {"code": "Agoda", "name": "Agoda.com", "rate": 589, "tax": null}]},
             "timestamp": 1794068973363}
            """;

    private final LocalDate entrada = LocalDate.now().plusDays(30);
    private final LocalDate salida = entrada.plusDays(3);

    private MockRestServiceServer servidor;
    private XoteloProveedorHoteles proveedor;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://data.xotelo.com/api");
        servidor = MockRestServiceServer.bindTo(builder).build();
        proveedor = new XoteloProveedorHoteles(builder.build());
    }

    @Test
    @DisplayName("Elige la tarifa más baja, guarda las demás y calcula las noches")
    void eligeLaMasBarata() {
        servidor.expect(requestTo(containsString("/rates")))
                .andRespond(withSuccess(TARIFAS, MediaType.APPLICATION_JSON));

        OfertaHotel oferta = proveedor.consultarTarifa("g1-d1", entrada, salida, 1, 2).orElseThrow();

        assertEquals("Trip.com", oferta.mejorTarifa().agencia());
        assertEquals(495.0, oferta.mejorTarifa().precioPorNoche(), 0.001);
        assertEquals(3, oferta.otrasTarifas().size());
        assertEquals(3, oferta.noches());
        assertEquals("USD", oferta.moneda());
        assertEquals(1485.0, oferta.costoTotalEstimado(), 0.001);
        assertTrue(oferta.descripcionReferencia().contains("Trip.com"));
    }

    @Test
    @DisplayName("La segunda consulta igual sale de la caché y no llama a la API")
    void usaLaCache() {
        servidor.expect(requestTo(containsString("/rates")))
                .andRespond(withSuccess(TARIFAS, MediaType.APPLICATION_JSON));

        proveedor.consultarTarifa("g1-d1", entrada, salida, 1, 2);
        proveedor.consultarTarifa("g1-d1", entrada, salida, 1, 2);

        servidor.verify(); // falla si hubo más de una llamada
    }

    @Test
    @DisplayName("Si la fuente no tiene tarifas, devuelve vacío")
    void sinTarifas() {
        servidor.expect(requestTo(containsString("/rates")))
                .andRespond(withSuccess("{\"error\":null,\"result\":{\"currency\":\"USD\",\"rates\":[]}}",
                        MediaType.APPLICATION_JSON));

        Optional<OfertaHotel> oferta = proveedor.consultarTarifa("g1-d1", entrada, salida, 1, 2);

        assertTrue(oferta.isEmpty());
    }

    @Test
    @DisplayName("Si la API responde con error, lanza ProveedorNoDisponibleException")
    void errorDeLaApi() {
        servidor.expect(requestTo(containsString("/rates")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"status_code\":400,\"message\":\"chk_in is too far in the past.\"}}"));

        assertThrows(ProveedorNoDisponibleException.class,
                () -> proveedor.consultarTarifa("g1-d1", entrada, salida, 1, 2));
    }

    @Test
    @DisplayName("Fechas inválidas lanzan IllegalArgumentException sin llamar a la API")
    void fechasInvalidas() {
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.consultarTarifa("g1-d1", entrada, entrada, 1, 2));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.consultarTarifa("g1-d1", LocalDate.now().minusDays(1), salida, 1, 2));
    }
}