package com.travelgo.web;

import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Actividad;
import com.travelgo.model.Gusto;
import com.travelgo.model.Reserva;
import com.travelgo.model.RolUsuario;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Usuario;
import com.travelgo.model.Viajero;
import com.travelgo.service.PlanViajeService;
import com.travelgo.service.ReservaService;
import com.travelgo.service.UsuarioService;
import com.travelgo.service.ViajerosService;
import com.travelgo.service.ViajerosService.DatosViajero;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "¿Quiénes viajan?": cada persona de la reserva elige sus gustos y sus actividades pagas,
 * y luego ve su propio plan día por día. Mismos permisos que las reservas: el Cliente solo las suyas,
 * el Agente las de todos, el Administrador no reserva (403).
 */
@Controller
public class ViajerosWebController {

    private final UsuarioService usuarioService;
    private final ReservaService reservaService;
    private final ViajerosService viajerosService;
    private final PlanViajeService planService;

    public ViajerosWebController(UsuarioService usuarioService, ReservaService reservaService,
                                 ViajerosService viajerosService, PlanViajeService planService) {
        this.usuarioService = usuarioService;
        this.reservaService = reservaService;
        this.viajerosService = viajerosService;
        this.planService = planService;
    }

    /** Formulario: lista de viajeros (Spring llena viajeros[0].nombre, viajeros[0].gustos...). */
    public static class ViajerosForm {
        private List<ViajeroForm> viajeros = new ArrayList<>();

        public List<ViajeroForm> getViajeros() { return viajeros; }

        public void setViajeros(List<ViajeroForm> viajeros) { this.viajeros = viajeros; }
    }

    public static class ViajeroForm {
        private String nombre;
        private List<Gusto> gustos = new ArrayList<>();
        private List<String> actividades = new ArrayList<>();

        public ViajeroForm() {
        }

        ViajeroForm(Viajero viajero) {
            this.nombre = viajero.getNombre();
            this.gustos = new ArrayList<>(viajero.getGustos());
            this.actividades = viajero.getActividades().stream().map(Actividad::id).toList();
        }

        /** Fila que quedó vacía (por ejemplo, un viajero que se quitó en el navegador). */
        boolean vacio() {
            return (nombre == null || nombre.isBlank()) && gustos.isEmpty() && actividades.isEmpty();
        }

        public String getNombre() { return nombre; }

        public void setNombre(String nombre) { this.nombre = nombre; }

        public List<Gusto> getGustos() { return gustos; }

        public void setGustos(List<Gusto> gustos) { this.gustos = gustos == null ? new ArrayList<>() : gustos; }

        public List<String> getActividades() { return actividades; }

        public void setActividades(List<String> actividades) {
            this.actividades = actividades == null ? new ArrayList<>() : actividades;
        }
    }

    @GetMapping("/reservas/{id}/viajeros")
    public String formulario(@PathVariable String id, Authentication autenticacion, Model model,
                             RedirectAttributes redirect) {
        Reserva reserva = reservaVisible(autenticacion, id);
        if (!viajerosService.editable(reserva)) {
            redirect.addFlashAttribute("error", "La reserva ya está pagada o cancelada; los viajeros no se pueden cambiar.");
            return "redirect:/reservas/" + id + "/plan";
        }

        ViajerosForm form = new ViajerosForm();
        reserva.getViajeros().forEach(v -> form.getViajeros().add(new ViajeroForm(v)));
        if (form.getViajeros().isEmpty()) {
            ViajeroForm primero = new ViajeroForm();
            primero.setNombre(primerNombre(reserva.getUsuario().getNombre()));
            form.getViajeros().add(primero);
        }

        List<Actividad> actividades = viajerosService.actividadesDisponibles(reserva);
        Map<Gusto, List<Actividad>> porGusto = new LinkedHashMap<>();
        for (Gusto gusto : Gusto.values()) {
            List<Actividad> deEste = actividades.stream().filter(a -> a.gusto() == gusto).toList();
            if (!deEste.isEmpty()) {
                porGusto.put(gusto, deEste);
            }
        }

        model.addAttribute("reserva", reserva);
        model.addAttribute("idCorto", FormatoWeb.idCorto(reserva.getId()));
        model.addAttribute("paquete", FormatoWeb.nombrePaquete(reserva));
        model.addAttribute("ciudad", ciudad(reserva));
        model.addAttribute("form", form);
        model.addAttribute("vacio", new ViajeroForm());
        model.addAttribute("gustos", Gusto.values());
        model.addAttribute("actividadesPorGusto", porGusto);
        model.addAttribute("totalServicios",
                reserva.getServicios().stream().mapToDouble(ServicioReservable::calcularCosto).sum());
        model.addAttribute("maximo", ViajerosService.MAXIMO_VIAJEROS);
        return "reserva/viajeros";
    }

    @PostMapping("/reservas/{id}/viajeros")
    public String guardar(@PathVariable String id, @ModelAttribute ViajerosForm form,
                          Authentication autenticacion, RedirectAttributes redirect) {
        Reserva reserva = reservaVisible(autenticacion, id);
        List<DatosViajero> datos = form.getViajeros().stream()
                .filter(v -> v != null && !v.vacio())
                .map(v -> new DatosViajero(v.getNombre(),
                        v.getGustos().isEmpty() ? EnumSet.noneOf(Gusto.class) : EnumSet.copyOf(v.getGustos()),
                        v.getActividades()))
                .toList();
        try {
            Reserva guardada = viajerosService.guardarViajeros(reserva, datos);
            redirect.addFlashAttribute("mensaje", "Listo: " + guardada.getViajeros().size()
                    + (guardada.getViajeros().size() == 1 ? " viajero" : " viajeros")
                    + ". Este es el plan de cada uno.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/reservas/" + id + "/viajeros";
        }
        return "redirect:/reservas/" + id + "/plan";
    }

    @GetMapping("/reservas/{id}/plan")
    public String plan(@PathVariable String id, Authentication autenticacion, Model model) {
        Reserva reserva = reservaVisible(autenticacion, id);
        if (reserva.getViajeros().isEmpty()) {
            return viajerosService.editable(reserva) ? "redirect:/reservas/" + id + "/viajeros" : "redirect:/reservas";
        }
        model.addAttribute("reserva", reserva);
        model.addAttribute("idCorto", FormatoWeb.idCorto(reserva.getId()));
        model.addAttribute("paquete", FormatoWeb.nombrePaquete(reserva));
        model.addAttribute("plan", planService.planDe(reserva));
        model.addAttribute("editable", viajerosService.editable(reserva));
        return "reserva/plan";
    }

    // ---------- Permisos (igual que ReservaWebController) ----------

    private Usuario usuarioQueReserva(Authentication autenticacion) {
        Usuario usuario = usuarioService.buscarPorCorreo(autenticacion.getName())
                .orElseThrow(() -> new AccessDeniedException("Usuario no encontrado"));
        if (usuario.getRol() != RolUsuario.CLIENTE && usuario.getRol() != RolUsuario.AGENTE) {
            throw new AccessDeniedException("El rol " + usuario.getRol() + " no reserva.");
        }
        return usuario;
    }

    private Reserva reservaVisible(Authentication autenticacion, String id) {
        Usuario usuario = usuarioQueReserva(autenticacion);
        try {
            return usuario.getRol() == RolUsuario.AGENTE
                    ? reservaService.obtener(id)
                    : reservaService.obtenerPropia(id, usuario.getCorreo());
        } catch (RecursoNoEncontradoException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private static String ciudad(Reserva reserva) {
        var paquete = reserva.getPaqueteTuristico();
        if (paquete == null || paquete.getDestinos() == null || paquete.getDestinos().isEmpty()) {
            return "tu destino";
        }
        return paquete.getDestinos().get(0).getCiudad();
    }

    private static String primerNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "";
        }
        return nombre.trim().split("\\s+")[0];
    }
}
