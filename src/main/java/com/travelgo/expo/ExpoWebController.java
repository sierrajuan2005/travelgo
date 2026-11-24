package com.travelgo.expo;

import com.travelgo.exception.RecursoNoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Páginas de "Lanza tu viaje" para la ExpoSoftware.
 *
 * /expo           → página del celular (pública): nombre + destino + "Lanzar"
 * /expo/pantalla  → pantalla grande del stand (pública): globo 3D, QR, contador y ranking
 * /expo/qr.svg    → QR con la dirección que abren los celulares
 * /expo/viaje/{id} → al aterrizar: el destino con datos reales, sus paquetes y el contacto para la postal
 * /admin/expo     → dirección pública (túnel), embudo del stand, contactos y moderación de nombres
 */
@Controller
public class ExpoWebController {

    private static final int ULTIMOS_PARA_MODERAR = 100;

    private final LanzamientoService service;
    private final DestinoExpoService destinoExpo;
    private final WhatsAppExpoService whatsApp;
    private final FotosPostal fotosPostal;
    private final UrlTelefono urlTelefono;

    public ExpoWebController(LanzamientoService service, DestinoExpoService destinoExpo, WhatsAppExpoService whatsApp,
                             FotosPostal fotosPostal, UrlTelefono urlTelefono) {
        this.service = service;
        this.destinoExpo = destinoExpo;
        this.whatsApp = whatsApp;
        this.fotosPostal = fotosPostal;
        this.urlTelefono = urlTelefono;
    }

    /**
     * Página del celular. Desde la postal de un amigo llega con {@code ?destino=paris&con=12}: el destino queda
     * elegido y arriba dice a quién se une. Si el viaje del amigo no existe (o lo ocultaron), es la página normal.
     */
    @GetMapping("/expo")
    public String lanzar(@RequestParam(required = false) String destino, @RequestParam(required = false) Long con,
                         Model model) {
        if (con != null) {
            try {
                LanzamientoService.ViajeLanzado invitacion = service.viaje(con);
                model.addAttribute("invitacion", invitacion);
                destino = destino == null ? invitacion.destino().id() : destino;
            } catch (RecursoNoEncontradoException e) {
                // Enlace viejo o viaje oculto: se lanza normal
            }
        }
        model.addAttribute("destinoElegido", destino == null ? "" : destino);
        // Agrupados por región, en el orden del catálogo (Colombia primero)
        Map<String, List<DestinoSonado>> regiones = service.destinos().stream().collect(
                Collectors.groupingBy(DestinoSonado::region, LinkedHashMap::new, Collectors.toList()));
        model.addAttribute("regiones", regiones);
        return "expo/lanzar";
    }

    /**
     * Lo que ve el visitante cuando su avión aterriza. Si el viaje no existe (o lo ocultaron), vuelve a /expo.
     * Solo la visita del propio visitante cuenta en el embudo.
     */
    @GetMapping("/expo/viaje/{id}")
    public String viaje(@PathVariable long id, HttpSession sesion, HttpServletRequest peticion, Model model) {
        LanzamientoService.ViajeLanzado viaje;
        try {
            viaje = service.viaje(id);
        } catch (RecursoNoEncontradoException e) {
            return "redirect:/expo";
        }
        boolean propio = SesionExpo.esPropio(sesion, id);
        if (propio) {
            service.marcarVioDestino(id);
        }
        DestinoSonado destino = viaje.destino();
        model.addAttribute("viaje", viaje.lanzamiento());
        model.addAttribute("destino", destino);
        model.addAttribute("propio", propio);
        model.addAttribute("telefono", propio ? TelefonoContacto.enmascarar(viaje.lanzamiento().telefono()) : "");
        model.addAttribute("km", destino.kmDesdeValledupar());
        model.addAttribute("sonadores", service.sonadoresDe(destino.id()));
        model.addAttribute("info", destinoExpo.info(destino));
        model.addAttribute("paquetes", destinoExpo.paquetes(destino));
        model.addAttribute("whatsappDisponible", whatsApp.disponible());
        model.addAttribute("enlaceActivacion", whatsApp.enlaceActivacion());
        model.addAttribute("estadoWhatsApp", propio ? whatsApp.estadoGuardado(id).orElse("") : "");
        // Enlace que va en la postal compartida: la dirección pública (túnel), la misma del QR
        model.addAttribute("urlCompartir", urlTelefono.resolver(peticion).base() + "/expo/viaje/" + id);
        return "expo/destino";
    }

    /**
     * Foto del destino servida desde TravelGo, para que el celular pueda dibujar la postal y compartirla
     * (ver {@link FotosPostal}). 404 si el destino no tiene foto.
     */
    @GetMapping("/expo/viaje/{id}/foto")
    public ResponseEntity<byte[]> fotoPostal(@PathVariable long id) {
        try {
            return fotosPostal.foto(service.viaje(id).destino())
                    .map(f -> ResponseEntity.ok()
                            .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                            .contentType(MediaType.parseMediaType(f.tipo()))
                            .body(f.datos()))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (RecursoNoEncontradoException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Abre un paquete desde la página del destino y lo cuenta en el embudo ("abrió un paquete"). */
    @GetMapping("/expo/viaje/{id}/paquete/{paqueteId}")
    public String abrirPaquete(@PathVariable long id, @PathVariable long paqueteId, HttpSession sesion) {
        if (SesionExpo.esPropio(sesion, id)) {
            service.marcarAbrioPaquete(id);
        }
        return "redirect:/catalogo/" + paqueteId;
    }

    @GetMapping("/expo/pantalla")
    public String pantalla(HttpServletRequest peticion, Model model) {
        UrlTelefono.Direccion direccion = direccionDe(peticion);
        model.addAttribute("urlTelefono", direccion.url());
        // Sin dirección pública solo entran celulares del mismo WiFi: la pantalla lo avisa para arreglarlo antes
        model.addAttribute("soloRedLocal", direccion.soloRedLocal());
        return "expo/pantalla";
    }

    @GetMapping(value = "/expo/qr.svg", produces = "image/svg+xml")
    public ResponseEntity<String> qr(HttpServletRequest peticion) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(GeneradorQr.svg(direccionDe(peticion).url()));
    }

    @GetMapping("/admin/expo")
    public String moderar(HttpServletRequest peticion, Model model) {
        UrlTelefono.Direccion direccion = direccionDe(peticion);
        model.addAttribute("urlPublica", urlTelefono.urlPublica());
        model.addAttribute("urlTelefono", direccion.url());
        model.addAttribute("soloRedLocal", direccion.soloRedLocal());
        model.addAttribute("viajes", service.ultimosParaModerar(ULTIMOS_PARA_MODERAR));
        model.addAttribute("total", service.total());
        model.addAttribute("embudo", service.embudo());
        model.addAttribute("whatsappDisponible", whatsApp.disponible());
        return "admin/expo";
    }

    /** El visitante pidió que borren su celular (Ley 1581: puede revocar la autorización). */
    @PostMapping("/admin/expo/{id}/borrar-contacto")
    public String borrarContacto(@PathVariable long id, RedirectAttributes flash) {
        service.borrarContacto(id);
        flash.addFlashAttribute("mensaje", "Se borró el celular de ese visitante.");
        return "redirect:/admin/expo";
    }

    /** Guarda la dirección pública (la del túnel) para el QR. Vacía = volver a la red local. */
    @PostMapping("/admin/expo/url-publica")
    public String cambiarUrlPublica(@RequestParam(defaultValue = "") String url, RedirectAttributes flash) {
        try {
            urlTelefono.cambiarUrlPublica(url);
            flash.addFlashAttribute("mensaje", urlTelefono.urlPublica().isEmpty()
                    ? "Se quitó la dirección pública: el QR vuelve a usar la red local."
                    : "Listo: el QR ahora lleva " + urlTelefono.urlPublica()
                            + "/expo. Recarga la pantalla del stand si ya estaba abierta.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/expo";
    }

    @PostMapping("/admin/expo/{id}/ocultar")
    public String ocultar(@PathVariable long id, @RequestParam(defaultValue = "true") boolean oculto,
                          RedirectAttributes flash) {
        service.cambiarOculto(id, oculto);
        flash.addFlashAttribute("mensaje", oculto
                ? "Viaje ocultado: la pantalla lo quita en menos de un minuto."
                : "El viaje vuelve a la pantalla.");
        return "redirect:/admin/expo";
    }

    private UrlTelefono.Direccion direccionDe(HttpServletRequest peticion) {
        return urlTelefono.resolver(peticion);
    }
}
