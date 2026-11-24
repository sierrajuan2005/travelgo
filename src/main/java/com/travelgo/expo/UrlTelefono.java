package com.travelgo.expo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Decide qué dirección lleva el QR de la pantalla del stand, es decir, a dónde entran los celulares.
 *
 * 1. La dirección pública que el administrador pegó en /admin/expo (por ejemplo, la de un túnel de
 *    Cloudflare: https://algo.trycloudflare.com). Con ella entra cualquier celular, con cualquier internet.
 *    Empieza con {@code travelgo.expo.url-publica} si existe.
 * 2. Si la pantalla se abrió por una dirección pública (el túnel o la app desplegada), se usa esa.
 *    Detrás de un túnel se leen X-Forwarded-Proto y X-Forwarded-Host, que pone el túnel.
 * 3. Si se abrió con "localhost", se busca la IP del portátil en la red WiFi (solo sirve en la misma red
 *    y si el firewall de Windows deja entrar).
 */
@Component
public class UrlTelefono {

    private static final List<String> ADAPTADORES_VIRTUALES =
            List.of("virtual", "vethernet", "vmware", "hyper-v", "docker", "wsl", "loopback", "bluetooth");

    private final AtomicReference<String> urlPublica = new AtomicReference<>("");

    public UrlTelefono(@Value("${travelgo.expo.url-publica:}") String urlPublica) {
        if (urlPublica != null && !urlPublica.isBlank()) {
            cambiarUrlPublica(urlPublica);
        }
    }

    /** Dirección pública guardada ("" si no hay). Sin "/expo" al final. */
    public String urlPublica() {
        return urlPublica.get();
    }

    /**
     * Guarda la dirección pública que pegó el administrador. Acepta la dirección con o sin "/expo" al final.
     * Vacía = quitarla. Se guarda en memoria: el túnel gratis cambia de dirección cada vez que se abre.
     *
     * @throws IllegalArgumentException si no es una dirección http o https
     */
    public void cambiarUrlPublica(String url) {
        String limpia = url == null ? "" : url.strip();
        if (limpia.isEmpty()) {
            urlPublica.set("");
            return;
        }
        if (!limpia.contains("://")) {
            limpia = "https://" + limpia;
        }
        try {
            URI uri = new URI(limpia);
            String esquema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!(esquema.equals("http") || esquema.equals("https")) || uri.getHost() == null) {
                throw new IllegalArgumentException("Pega una dirección que empiece por https://");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Esa dirección no es válida: " + url, e);
        }
        limpia = sinBarraFinal(limpia);
        if (limpia.endsWith("/expo")) {
            limpia = limpia.substring(0, limpia.length() - "/expo".length());
        }
        urlPublica.set(limpia);
    }

    /**
     * @param esquema   http o https con el que se abrió la pantalla (o el de X-Forwarded-Proto)
     * @param host      host con el que se abrió la pantalla (o el de X-Forwarded-Host)
     * @param puerto    puerto; 0 o menos si es el normal del esquema
     * @param contexto  context path de la app ("" normalmente)
     */
    public Direccion resolver(String esquema, String host, int puerto, String contexto) {
        String publica = urlPublica.get();
        if (!publica.isEmpty()) {
            return new Direccion(publica + "/expo", false);
        }
        String hostTelefono = esLocal(host) ? ipEnLaRed().orElse(host) : host;
        boolean puertoNormal = puerto <= 0
                || ("http".equals(esquema) && puerto == 80) || ("https".equals(esquema) && puerto == 443);
        String url = esquema + "://" + hostTelefono + (puertoNormal ? "" : ":" + puerto) + contexto + "/expo";
        return new Direccion(url, esLocal(hostTelefono));
    }

    /**
     * Igual que {@link #resolver(String, String, int, String)}, leyendo la petición. Detrás de un túnel
     * (Cloudflare, ngrok) la app recibe la petición por http://localhost:8080, pero el túnel dice en
     * X-Forwarded-Proto y X-Forwarded-Host cómo la abrió el usuario (https://algo.trycloudflare.com).
     */
    public Direccion resolver(HttpServletRequest peticion) {
        String esquema = primerValor(peticion.getHeader("X-Forwarded-Proto"));
        String host = primerValor(peticion.getHeader("X-Forwarded-Host"));
        if (esquema == null && host == null) {
            return resolver(peticion.getScheme(), peticion.getServerName(), peticion.getServerPort(),
                    peticion.getContextPath());
        }
        // El puerto interno (8080) no sirve afuera: se usa el normal de https/http
        return resolver(esquema == null ? peticion.getScheme() : esquema,
                host == null ? peticion.getServerName() : host, 0, peticion.getContextPath());
    }

    // "https, http" → "https" (cuando hay varios proxies)
    private static String primerValor(String cabecera) {
        if (cabecera == null || cabecera.isBlank()) {
            return null;
        }
        return cabecera.split(",")[0].strip();
    }

    static boolean esLocal(String host) {
        return host == null || host.equals("localhost") || host.startsWith("127.")
                || host.equals("::1") || host.equals("[::1]") || host.equals("0:0:0:0:0:0:0:1");
    }

    /**
     * @param url                 dirección que lleva el QR
     * @param soloEsteComputador  true si es "localhost": un celular no podría entrar y la pantalla lo avisa
     */
    public record Direccion(String url, boolean soloEsteComputador) {

        /** Dirección de la app sin "/expo" (para armar otros enlaces: /expo/viaje/5, /catalogo/2...). */
        public String base() {
            return url.endsWith("/expo") ? url.substring(0, url.length() - "/expo".length()) : url;
        }

        /** true si la dirección es de la red local (192.168..., 10...): solo entran celulares del mismo WiFi. */
        public boolean soloRedLocal() {
            String host = URI.create(url).getHost();
            return soloEsteComputador || host == null || host.startsWith("192.168.") || host.startsWith("10.")
                    || host.matches("172\\.(1[6-9]|2\\d|3[01])\\..*");
        }
    }

    /** IPv4 privada del portátil, prefiriendo la del WiFi o el cable (192.168.x, luego 10.x, luego 172.x). */
    static Optional<String> ipEnLaRed() {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                    .filter(UrlTelefono::esAdaptadorReal)
                    .flatMap(i -> Collections.list(i.getInetAddresses()).stream())
                    .filter(a -> a instanceof Inet4Address && a.isSiteLocalAddress())
                    .map(InetAddress::getHostAddress)
                    .min(Comparator.comparingInt(UrlTelefono::prioridad));
        } catch (SocketException e) {
            return Optional.empty();
        }
    }

    private static boolean esAdaptadorReal(NetworkInterface i) {
        try {
            String nombre = (i.getName() + " " + i.getDisplayName()).toLowerCase(Locale.ROOT);
            return i.isUp() && !i.isLoopback() && !i.isVirtual()
                    && ADAPTADORES_VIRTUALES.stream().noneMatch(nombre::contains);
        } catch (SocketException e) {
            return false;
        }
    }

    static int prioridad(String ip) {
        if (ip.startsWith("192.168.")) return 0;
        if (ip.startsWith("10.")) return 1;
        return 2;
    }

    private static String sinBarraFinal(String url) {
        String u = url;
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }
}
