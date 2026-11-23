package com.travelgo.chatbot.estrategia;

import com.travelgo.chatbot.ContextoChat;
import com.travelgo.chatbot.DatosMensaje;
import com.travelgo.chatbot.EstrategiaChatbot;
import com.travelgo.chatbot.Formato;
import com.travelgo.chatbot.RespuestaChatbot;
import com.travelgo.model.Reserva;
import com.travelgo.service.ReservaService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/** "¿Cómo va mi reserva?" Consulta privada: solo con sesión y solo las reservas del propio usuario. */
@Component
@Order(10)
public class EstadoReservaEstrategia implements EstrategiaChatbot {

	static final int MAXIMO = 5;

	private final ReservaService reservaService;

	public EstadoReservaEstrategia(ReservaService reservaService) {
		this.reservaService = reservaService;
	}

	@Override
	public String nombre() {
		return "ESTADO_RESERVA";
	}

	@Override
	public boolean aplica(DatosMensaje datos) {
		return datos.contiene("mi reserva", "mis reservas", "como va", "estado de mi", "estado de la reserva");
	}

	@Override
	public RespuestaChatbot responder(DatosMensaje datos, ContextoChat contexto) {
		if (!contexto.autenticado()) {
			return RespuestaChatbot.de("Para consultar tus reservas necesitas iniciar sesión en TravelGo.");
		}
		List<Reserva> reservas = reservaService.listarPropias(contexto.correo());
		if (reservas.isEmpty()) {
			return RespuestaChatbot.de("Todavía no tienes reservas. Puedes pedirme paquetes por destino.");
		}
		List<Reserva> recientes = reservas.stream()
			.sorted(Comparator.comparing(Reserva::getFechaReserva).reversed())
			.limit(MAXIMO)
			.toList();
		StringBuilder sb = new StringBuilder("Estas son tus reservas más recientes:\n");
		for (Reserva r : recientes) {
			String paquete = r.getPaqueteTuristico() != null ? r.getPaqueteTuristico().getNombre() : "Reserva sin paquete";
			sb.append("• ").append(paquete).append(" — ").append(r.getEstado()).append(" — ")
				.append(Formato.cop(r.getTotal())).append(" — reservada el ")
				.append(r.getFechaReserva().format(Formato.FECHA)).append('\n');
		}
		return RespuestaChatbot.de(sb.toString().trim());
	}
}
