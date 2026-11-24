package com.travelgo.expo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas del nombre que sale en la pantalla del stand. */
class NombreViajeroTest {

    @Test
    @DisplayName("Quita los espacios de más y acepta tildes, eñes, guiones y apóstrofos")
    void limpiaYAceptaNombresReales() {
        assertThat(NombreViajero.limpiar("  Juan   Diego ")).isEqualTo("Juan Diego");
        assertThat(NombreViajero.limpiar("Begoña")).isEqualTo("Begoña");
        assertThat(NombreViajero.limpiar("José-Ángel")).isEqualTo("José-Ángel");
        assertThat(NombreViajero.limpiar("D'Artagnan")).isEqualTo("D'Artagnan");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "A"})
    @DisplayName("Rechaza nombres vacíos o de una letra")
    void rechazaNombresCortos(String nombre) {
        assertThatThrownBy(() -> NombreViajero.limpiar(nombre))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mínimo");
    }

    @Test
    @DisplayName("Rechaza nombres nulos")
    void rechazaNulo() {
        assertThatThrownBy(() -> NombreViajero.limpiar(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Rechaza nombres de más de 20 caracteres")
    void rechazaNombresLargos() {
        assertThatThrownBy(() -> NombreViajero.limpiar("Maximiliano Alejandro"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ana123", "<script>", "Ana 😀", "http://x.co"})
    @DisplayName("Rechaza números, símbolos y emojis")
    void rechazaCaracteresRaros(String nombre) {
        assertThatThrownBy(() -> NombreViajero.limpiar(nombre))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("letras");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Gonorrea", "MIERDA", "MaRiCa", "Pendéjo", "hijue puta", "Ana Malparida"})
    @DisplayName("Rechaza groserías aunque vengan con mayúsculas, tildes o espacios")
    void rechazaGroserias(String nombre) {
        assertThatThrownBy(() -> NombreViajero.limpiar(nombre))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no se puede mostrar");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Mari Carmen", "Maricarmen", "Diputado", "Hércules", "Penélope"})
    @DisplayName("No bloquea nombres reales que contienen una grosería por dentro")
    void noBloqueaNombresReales(String nombre) {
        assertThat(NombreViajero.limpiar(nombre)).isEqualTo(nombre);
    }

    @Test
    @DisplayName("Detecta groserías escritas con números")
    void detectaGroseriasConNumeros() {
        assertThat(NombreViajero.esOfensivo("p3ndej0")).isTrue();
        assertThat(NombreViajero.esOfensivo("Valentina")).isFalse();
    }
}
