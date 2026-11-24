package com.travelgo.expo;

import com.travelgo.exception.ProveedorNoDisponibleException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API pública de "Lanza tu viaje" (SecurityConfig la deja abierta: los visitantes no tienen cuenta).
 *
 * GET  /api/expo/destinos              → destinos que se pueden elegir
 * POST /api/expo/lanzamientos          → {nombre, destinoId, invitadoPor?}; el celular lanza un viaje
 * GET  /api/expo/lanzamientos?desde=N  → la pantalla pide los viajes nuevos y el total
 * POST   /api/expo/lanzamientos/{id}/contacto → {telefono, autoriza}; solo desde el celular que lanzó el viaje
 * DELETE /api/expo/lanzamientos/{id}/contacto → el visitante borra su celular
 * POST   /api/expo/lanzamientos/{id}/whatsapp → envía su viaje por WhatsApp (solo desde el celular que lanzó)
 * GET    /api/expo/lanzamientos/{id}/whatsapp → estado real del WhatsApp (enviando, entregado, sin activar...)
 * POST   /api/expo/lanzamientos/{id}/compartio → el visitante compartió o descargó su postal (embudo)
 */
@RestController
@RequestMapping("/api/expo")
public class ExpoApiController {

    private final LanzamientoService service;
    private final WhatsAppExpoService whatsApp;
    private final UrlTelefono urlTelefono;

    public ExpoApiController(LanzamientoService service, WhatsAppExpoService whatsApp, UrlTelefono urlTelefono) {
        this.service = service;
        this.whatsApp = whatsApp;
        this.urlTelefono = urlTelefono;
    }

    @GetMapping("/destinos")
    public List<DestinoSonado> destinos() {
        return service.destinos();
    }

    @PostMapping("/lanzamientos")
    @ResponseStatus(HttpStatus.CREATED)
    public ViajeLanzadoDTO lanzar(@RequestBody LanzamientoRequest solicitud, HttpSession sesion) {
        LanzamientoService.ViajeLanzado viaje =
                service.lanzar(solicitud.nombre(), solicitud.destinoId(), sesion.getId(), solicitud.invitadoPor());
        SesionExpo.recordar(sesion, viaje.lanzamiento().id());
        return ViajeLanzadoDTO.de(viaje);
    }

    @PostMapping("/lanzamientos/{id}/contacto")
    public ContactoDTO guardarContacto(@PathVariable long id, @RequestBody ContactoRequest solicitud,
                                       HttpSession sesion) {
        SesionExpo.exigirPropio(sesion, id);
        String telefono = service.registrarContacto(id, solicitud.telefono(), solicitud.autoriza());
        return new ContactoDTO(TelefonoContacto.enmascarar(telefono));
    }

    @DeleteMapping("/lanzamientos/{id}/contacto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrarContacto(@PathVariable long id, HttpSession sesion) {
        SesionExpo.exigirPropio(sesion, id);
        service.borrarContacto(id);
    }

    @PostMapping("/lanzamientos/{id}/compartio")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void compartioPostal(@PathVariable long id, HttpSession sesion) {
        SesionExpo.exigirPropio(sesion, id);
        service.marcarCompartioPostal(id);
    }

    @PostMapping("/lanzamientos/{id}/whatsapp")
    public EstadoWhatsApp enviarWhatsApp(@PathVariable long id, HttpSession sesion, HttpServletRequest peticion) {
        SesionExpo.exigirPropio(sesion, id);
        try {
            // Los enlaces del mensaje usan la dirección pública (la del túnel), la misma del QR
            return whatsApp.enviar(id, urlTelefono.resolver(peticion).base());
        } catch (CanalWhatsApp.WhatsAppException e) {
            throw new ProveedorNoDisponibleException("WhatsApp no respondió. Intenta otra vez en un momento.", e);
        }
    }

    @GetMapping("/lanzamientos/{id}/whatsapp")
    public ResponseEntity<EstadoWhatsApp> estadoWhatsApp(@PathVariable long id, HttpSession sesion) {
        SesionExpo.exigirPropio(sesion, id);
        try {
            return whatsApp.consultar(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
        } catch (CanalWhatsApp.WhatsAppException e) {
            throw new ProveedorNoDisponibleException("No se pudo consultar el WhatsApp.", e);
        }
    }

    @GetMapping("/lanzamientos")
    public PantallaDTO pantalla(@RequestParam(defaultValue = "0") long desde) {
        LanzamientoService.Pantalla p = service.pantalla(desde);
        return new PantallaDTO(p.total(), p.viajes().stream().map(ViajeLanzadoDTO::de).toList());
    }

    /** invitadoPor: id del viaje de quien le compartió la postal (opcional). */
    public record LanzamientoRequest(String nombre, String destinoId, Long invitadoPor) {
    }

    public record ContactoRequest(String telefono, boolean autoriza) {
    }

    /** Solo el número enmascarado ("+57 300 ••• 4567"): el completo nunca vuelve al navegador. */
    public record ContactoDTO(String telefono) {
    }

    public record PantallaDTO(long total, List<ViajeLanzadoDTO> viajes) {
    }
}
