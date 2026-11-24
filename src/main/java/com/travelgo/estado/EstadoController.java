package com.travelgo.estado;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Set;

/**
 * Panel de estado de las APIs externas (/admin/** ya exige el rol ADMINISTRADOR en SecurityConfig).
 *
 * GET  /admin/estado                    → la página
 * GET  /admin/estado/datos              → JSON que la página pide cada 3 s
 * POST /admin/estado/{id}/probar        → consulta real y pequeña a esa API
 * POST /admin/estado/probar-todas       → igual, a todas (menos Ignav)
 * POST /admin/estado/{id}/caida?activa= → simular (o quitar) la caída de una API
 * POST /admin/estado/sin-internet?activa= → simular (o quitar) la caída de todas
 */
@Controller
public class EstadoController {

    private final RegistroLlamadas registro;
    private final SondasProveedores sondas;
    private final MonitorProveedores monitor;

    public EstadoController(RegistroLlamadas registro, SondasProveedores sondas, MonitorProveedores monitor) {
        this.registro = registro;
        this.sondas = sondas;
        this.monitor = monitor;
    }

    @GetMapping("/admin/estado")
    public String panel(Model model) {
        model.addAttribute("datos", datos());
        return "admin/estado";
    }

    @GetMapping("/admin/estado/datos")
    @ResponseBody
    public Panel datos() {
        Set<String> activas = monitor.activas();
        return new Panel(registro.sinInternet(activas), registro.estados(activas));
    }

    @PostMapping("/admin/estado/{id}/probar")
    @ResponseBody
    public ResponseEntity<Panel> probar(@PathVariable String id) {
        if (!monitor.activas().contains(id) || !sondas.probar(id)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(datos());
    }

    @PostMapping("/admin/estado/probar-todas")
    @ResponseBody
    public Panel probarTodas() {
        sondas.probarTodas(monitor.activas());
        return datos();
    }

    @PostMapping("/admin/estado/{id}/caida")
    @ResponseBody
    public ResponseEntity<Panel> caida(@PathVariable String id, @RequestParam boolean activa) {
        if (!monitor.activas().contains(id)) {
            return ResponseEntity.notFound().build();
        }
        registro.simularCaida(id, activa);
        return ResponseEntity.ok(datos());
    }

    @PostMapping("/admin/estado/sin-internet")
    @ResponseBody
    public Panel sinInternet(@RequestParam boolean activa) {
        registro.simularSinInternet(monitor.activas(), activa);
        return datos();
    }

    /** Todo el panel: si está simulado "sin internet" y el estado de cada API. */
    public record Panel(boolean sinInternet, List<EstadoFuente> fuentes) {
    }
}
