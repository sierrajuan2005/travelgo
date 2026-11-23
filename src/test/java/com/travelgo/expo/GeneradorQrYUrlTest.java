package com.travelgo.expo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas del QR de la pantalla y de la dirección que lleva. */
class GeneradorQrYUrlTest {

    @Test
    @DisplayName("El QR es un SVG cuadrado con fondo blanco y módulos oscuros")
    void qrEsSvg() {
        String svg = GeneradorQr.svg("http://192.168.1.20:8080/expo");

        assertThat(svg).startsWith("<svg").endsWith("</svg>").contains("fill=\"#ffffff\"").contains("h1v1h-1z");
        assertThat(svg).containsPattern("viewBox=\"0 0 (\\d+) \\1\"");
    }

    @Test
    @DisplayName("Con url-publica configurada, el QR siempre lleva esa dirección")
    void urlPublicaGana() {
        UrlTelefono url = new UrlTelefono("https://travelgo.onrender.com/");

        UrlTelefono.Direccion d = url.resolver("http", "localhost", 8080, "");

        assertThat(d.url()).isEqualTo("https://travelgo.onrender.com/expo");
        assertThat(d.soloEsteComputador()).isFalse();
    }

    @Test
    @DisplayName("Si la pantalla se abrió con la IP del portátil, el QR usa esa IP y el puerto")
    void usaLaIpDeLaPeticion() {
        UrlTelefono.Direccion d = new UrlTelefono("").resolver("http", "192.168.1.20", 8080, "");

        assertThat(d.url()).isEqualTo("http://192.168.1.20:8080/expo");
        assertThat(d.soloEsteComputador()).isFalse();
    }

    @Test
    @DisplayName("No escribe el puerto cuando es el normal (80 en http, 443 en https)")
    void sinPuertoNormal() {
        assertThat(new UrlTelefono("").resolver("https", "travelgo.co", 443, "").url())
                .isEqualTo("https://travelgo.co/expo");
    }

    @Test
    @DisplayName("Con localhost busca la IP de la red; si no la encuentra, avisa que es solo de este computador")
    void localhostBuscaLaIp() {
        UrlTelefono.Direccion d = new UrlTelefono("").resolver("http", "localhost", 8080, "");

        if (d.soloEsteComputador()) {
            assertThat(d.url()).isEqualTo("http://localhost:8080/expo");
        } else {
            assertThat(d.url()).matches("http://\\d+\\.\\d+\\.\\d+\\.\\d+:8080/expo");
        }
    }

    @Test
    @DisplayName("Prefiere la red de la casa o del WiFi (192.168) sobre otras privadas")
    void prioridadDeRedes() {
        assertThat(UrlTelefono.prioridad("192.168.0.5")).isLessThan(UrlTelefono.prioridad("10.0.0.5"));
        assertThat(UrlTelefono.prioridad("10.0.0.5")).isLessThan(UrlTelefono.prioridad("172.20.0.5"));
    }

    @Test
    @DisplayName("La dirección pegada en el admin se limpia: agrega https, quita la barra y /expo del final")
    void limpiaLaDireccionPegada() {
        UrlTelefono url = new UrlTelefono("");

        url.cambiarUrlPublica("  algo-raro.trycloudflare.com/expo/ ");

        assertThat(url.urlPublica()).isEqualTo("https://algo-raro.trycloudflare.com");
        assertThat(url.resolver("http", "localhost", 8080, "").url())
                .isEqualTo("https://algo-raro.trycloudflare.com/expo");
    }

    @Test
    @DisplayName("Vaciar la dirección pública vuelve a la red local; una dirección que no es web se rechaza")
    void quitarYRechazar() {
        UrlTelefono url = new UrlTelefono("https://algo.trycloudflare.com");
        url.cambiarUrlPublica("");
        assertThat(url.urlPublica()).isEmpty();

        assertThatThrownBy(() -> url.cambiarUrlPublica("ftp://algo.com"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> url.cambiarUrlPublica("https://"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Detrás de un túnel (puerto 0) la dirección no lleva puerto")
    void detrasDeUnTunel() {
        UrlTelefono.Direccion d = new UrlTelefono("").resolver("https", "algo.trycloudflare.com", 0, "");

        assertThat(d.url()).isEqualTo("https://algo.trycloudflare.com/expo");
        assertThat(d.soloRedLocal()).isFalse();
    }

    @Test
    @DisplayName("Una IP privada solo sirve en el mismo WiFi")
    void ipPrivadaEsSoloRedLocal() {
        assertThat(new UrlTelefono.Direccion("http://192.168.1.20:8080/expo", false).soloRedLocal()).isTrue();
        assertThat(new UrlTelefono.Direccion("http://172.20.1.5:8080/expo", false).soloRedLocal()).isTrue();
        assertThat(new UrlTelefono.Direccion("https://travelgo.co/expo", false).soloRedLocal()).isFalse();
    }
}
