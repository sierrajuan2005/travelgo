package com.travelgo.web;

import com.travelgo.recordatorio.RecordatorioService;
import com.travelgo.recordatorio.RecordatorioService.ResumenRevision;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

/**
 * Panel de recordatorios del administrador (/admin/** ya exige el rol ADMINISTRADOR en SecurityConfig):
 * qué canales están configurados, los últimos envíos y un botón para revisar ahora sin esperar al programador
 * (útil en la sustentación).
 */
@Controller
public class RecordatoriosWebController {

	private static final int ULTIMOS_ENVIOS = 50;

	private final RecordatorioService recordatorioService;

	public RecordatoriosWebController(RecordatorioService recordatorioService) {
		this.recordatorioService = recordatorioService;
	}

	@GetMapping("/admin/recordatorios")
	public String panel(Model model) {
		model.addAttribute("canales", recordatorioService.estadoCanales());
		model.addAttribute("envios", recordatorioService.ultimosEnvios(ULTIMOS_ENVIOS));
		return "admin/recordatorios";
	}

	@PostMapping("/admin/recordatorios/revisar")
	public String revisarAhora(RedirectAttributes flash) {
		ResumenRevision resumen = recordatorioService.revisar(LocalDateTime.now());
		if (!resumen.hayCanales()) {
			flash.addFlashAttribute("error",
				"No hay ningún canal configurado (correo o SMS). Revisa docs/recordatorios.md.");
		} else {
			flash.addFlashAttribute("mensaje", "Revisión hecha: " + resumen.revisadas() + " reservas, "
				+ resumen.enviados() + " avisos enviados, " + resumen.fallidos() + " fallidos.");
		}
		return "redirect:/admin/recordatorios";
	}
}
