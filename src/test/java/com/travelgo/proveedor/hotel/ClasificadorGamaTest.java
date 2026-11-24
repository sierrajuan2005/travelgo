package com.travelgo.proveedor.hotel;

import com.travelgo.model.Hotel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Pruebas de la clasificación de hoteles por gama. */
class ClasificadorGamaTest {

    private final ClasificadorGama clasificador = new ClasificadorGama(400000, 1000000);

    @Test
    @DisplayName("Un hotel barato es económico, uno intermedio es medio y uno caro es lujo")
    void clasificaPorPrecio() {
        assertEquals(GamaHotel.ECONOMICO, clasificador.clasificar(280000));
        assertEquals(GamaHotel.MEDIO, clasificador.clasificar(600000));
        assertEquals(GamaHotel.LUJO, clasificador.clasificar(2300000));
    }

    @Test
    @DisplayName("Los cortes son inclusivos: el económico incluye su tope y el lujo incluye su base")
    void cortesInclusivos() {
        assertEquals(GamaHotel.ECONOMICO, clasificador.clasificar(400000));
        assertEquals(GamaHotel.MEDIO, clasificador.clasificar(400001));
        assertEquals(GamaHotel.LUJO, clasificador.clasificar(1000000));
    }

    @Test
    @DisplayName("Un precio inválido lanza IllegalArgumentException")
    void precioInvalido() {
        assertThrows(IllegalArgumentException.class, () -> clasificador.clasificar(0));
        assertThrows(IllegalArgumentException.class, () -> clasificador.clasificar(Double.NaN));
    }

    @Test
    @DisplayName("Unos cortes incoherentes lanzan IllegalArgumentException")
    void cortesInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new ClasificadorGama(0, 1000000));
        assertThrows(IllegalArgumentException.class, () -> new ClasificadorGama(500000, 500000));
    }

    @Test
    @DisplayName("La gama de un hotel guardado sale de su precio por noche; sin precio es null")
    void gamaDeUnHotel() {
        Hotel economico = new Hotel("h1", 280000.0, true, "ibis", "Cartagena", "Habitación", 3);
        Hotel sinPrecio = new Hotel("h2", 0.0, true, "Hotel", "Cartagena", "Habitación", 3);

        assertEquals(GamaHotel.ECONOMICO, clasificador.gamaDe(economico));
        assertNull(clasificador.gamaDe(sinPrecio));
        assertNull(clasificador.gamaDe(null));
    }
}