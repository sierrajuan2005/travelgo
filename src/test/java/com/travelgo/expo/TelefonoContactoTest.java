package com.travelgo.expo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas del celular que deja el visitante y de la distancia del pase de abordar. */
class TelefonoContactoTest {

    @ParameterizedTest
    @ValueSource(strings = {"3001234567", "300 123 4567", "300-123-4567", "(300) 123 4567", "+57 300 123 4567",
            "573001234567"})
    @DisplayName("Un celular colombiano escrito de cualquier forma queda como +57 y 10 dígitos")
    void normalizaCelularColombiano(String escrito) {
        assertThat(TelefonoContacto.normalizar(escrito)).isEqualTo("+573001234567");
    }

    @Test
    @DisplayName("Acepta números de otros países con el indicativo")
    void aceptaInternacional() {
        assertThat(TelefonoContacto.normalizar("+1 305 555 0123")).isEqualTo("+13055550123");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "6051234567", "30012345678", "hola", "+0 123 456 789"})
    @DisplayName("Rechaza fijos, números cortos o largos y texto")
    void rechazaInvalidos(String escrito) {
        assertThatThrownBy(() -> TelefonoContacto.normalizar(escrito))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 dígitos");
    }

    @Test
    @DisplayName("Enmascara el número para mostrarlo sin exponerlo completo")
    void enmascara() {
        assertThat(TelefonoContacto.enmascarar("+573001234567")).isEqualTo("+57 300 ••• 4567");
        assertThat(TelefonoContacto.enmascarar("+13055550123")).isEqualTo("+130 ••• 0123");
        assertThat(TelefonoContacto.enmascarar(null)).isEmpty();
    }

    @Test
    @DisplayName("La distancia desde Valledupar es la del círculo máximo (aprox. 8.100 km a París, 0 a sí misma)")
    void distanciaDesdeValledupar() {
        CatalogoDestinosSonados catalogo = new CatalogoDestinosSonados();

        assertThat(catalogo.buscar("paris").orElseThrow().kmDesdeValledupar()).isBetween(7950L, 8200L);
        assertThat(catalogo.buscar("cartagena").orElseThrow().kmDesdeValledupar()).isBetween(230L, 260L);
        DestinoSonado valledupar = new DestinoSonado("vup", "Valledupar", "Colombia", "Colombia",
                DestinoSonado.VALLEDUPAR_LATITUD, DestinoSonado.VALLEDUPAR_LONGITUD, null, "VUP");
        assertThat(valledupar.kmDesdeValledupar()).isZero();
    }
}
