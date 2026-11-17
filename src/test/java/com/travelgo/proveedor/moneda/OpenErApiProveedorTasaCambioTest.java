package com.travelgo.proveedor.moneda;

import com.travelgo.exception.ProveedorNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Pruebas del adaptador de tasas de cambio con respuestas simuladas. */
class OpenErApiProveedorTasaCambioTest {

    private MockRestServiceServer servidor;
    private OpenErApiProveedorTasaCambio proveedor;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://open.er-api.com/v6");
        servidor = MockRestServiceServer.bindTo(builder).build();
        proveedor = new OpenErApiProveedorTasaCambio(builder.build());
    }

    @Test
    @DisplayName("Lee la tasa de pesos por dólar y la guarda: la segunda consulta no llama a la API")
    void leeYGuardaLaTasa() {
        servidor.expect(requestTo("https://open.er-api.com/v6/latest/USD"))
                .andRespond(withSuccess(
                        "{\"result\":\"success\",\"base_code\":\"USD\",\"rates\":{\"USD\":1,\"COP\":3900.5}}",
                        MediaType.APPLICATION_JSON));

        TasaCambio primera = proveedor.obtenerUsdACop();
        TasaCambio segunda = proveedor.obtenerUsdACop();

        assertEquals(3900.5, primera.copPorUsd(), 0.001);
        assertEquals(3900.5, segunda.copPorUsd(), 0.001);
        assertEquals(273035.0, primera.convertirUsdACop(70), 0.001);
        servidor.verify(); // falla si hubo más de una llamada
    }

    @Test
    @DisplayName("Si la respuesta no trae el peso colombiano, lanza ProveedorNoDisponibleException")
    void sinCop() {
        servidor.expect(requestTo("https://open.er-api.com/v6/latest/USD"))
                .andRespond(withSuccess("{\"result\":\"success\",\"rates\":{\"USD\":1}}",
                        MediaType.APPLICATION_JSON));

        assertThrows(ProveedorNoDisponibleException.class, () -> proveedor.obtenerUsdACop());
    }

    @Test
    @DisplayName("Si la API responde con error, lanza ProveedorNoDisponibleException")
    void errorDeLaApi() {
        servidor.expect(requestTo("https://open.er-api.com/v6/latest/USD"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ProveedorNoDisponibleException.class, () -> proveedor.obtenerUsdACop());
    }
}