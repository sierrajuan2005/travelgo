package com.travelgo.chatbot.estrategia;

import com.travelgo.chatbot.ContextoChat;
import com.travelgo.chatbot.DatosMensaje;
import com.travelgo.chatbot.EstrategiaChatbot;
import com.travelgo.chatbot.Formato;
import com.travelgo.chatbot.RespuestaChatbot;
import com.travelgo.dao.PaqueteServicioDAO;
import com.travelgo.model.Hotel;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Vuelo;
import com.travelgo.service.PaqueteService;
import com.travelgo.util.Textos;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** "¿Qué incluye el paquete X?" Muestra sus servicios con su precio de referencia, fecha y enlace de reserva. */
@Component
@Order(50)
public class ServiciosPaqueteEstrategia implements EstrategiaChatbot {

	private final PaqueteService paqueteService;
	private final PaqueteServicioDAO paqueteServicioDAO;

	public ServiciosPaqueteEstrategia(PaqueteService paqueteService, PaqueteServicioDAO paqueteServicioDAO) {
		this.paqueteService = paqueteService;
		this.paqueteServicioDAO = paqueteServicioDAO;
	}

	@Override
	public String nombre() {
		return "SERVICIOS_PAQUETE";
	}

	@Override
	public boolean aplica(DatosMensaje datos) {
		return datos.contiene("incluye", "que trae", "que servicios", "que tiene el paquete", "que hay en el paquete");
	}

	@Override
	public RespuestaChatbot responder(DatosMensaje datos, ContextoChat contexto) {
		List<PaqueteTuristico> activos = paqueteService.listar(true);
		PaqueteTuristico elegido = null;
		for (PaqueteTuristico p : activos) {
			String nombre = Textos.normalizar(p.getNombre());
			if (!nombre.isEmpty() && datos.texto().contains(nombre)
					&& (elegido == null || nombre.length() > Textos.normalizar(elegido.getNombre()).length())) {
				elegido = p;
			}
		}
		if (elegido == null) {
			if (activos.isEmpty()) {
				return RespuestaChatbot.de("Por ahora no hay paquetes disponibles.");
			}
			return RespuestaChatbot.de("¿De cuál paquete? Escribe su nombre, por ejemplo:\n"
				+ Formato.paquetes(activos, 5));
		}
		List<ServicioReservable> servicios = paqueteServicioDAO.listarServicios(elegido.getId());
		if (servicios.isEmpty()) {
			return RespuestaChatbot.de("El paquete \"" + elegido.getNombre() + "\" todavía no tiene servicios registrados.");
		}
		StringBuilder sb = new StringBuilder("El paquete \"" + elegido.getNombre() + "\" incluye:\n");
		for (ServicioReservable s : servicios) {
			sb.append("• ").append(describir(s)).append(" — ").append(Formato.cop(s.getPrecio())).append(" (referencia");
			if (s.getFechaConsulta() != null) {
				sb.append(", consultado el ").append(s.getFechaConsulta().format(Formato.FECHA));
			}
			sb.append(')');
			if (s.getEnlaceReserva() != null && !s.getEnlaceReserva().isBlank()) {
				sb.append(" — reserva en: ").append(s.getEnlaceReserva());
			}
			sb.append('\n');
		}
		return RespuestaChatbot.de(sb.toString().trim());
	}

	private String describir(ServicioReservable servicio) {
		if (servicio instanceof Vuelo v) {
			return "Vuelo " + v.getAerolinea() + " " + v.getOrigen() + " → " + v.getDestino();
		}
		if (servicio instanceof Hotel h) {
			return "Hotel " + h.getNombre() + " (" + h.getNoches() + " noches)";
		}
		return "Servicio de transporte";
	}
}
