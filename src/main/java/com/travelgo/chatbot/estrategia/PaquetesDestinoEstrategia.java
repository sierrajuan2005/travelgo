package com.travelgo.chatbot.estrategia;

import com.travelgo.chatbot.BuscadorPaquetes;
import com.travelgo.chatbot.ContextoChat;
import com.travelgo.chatbot.DatosMensaje;
import com.travelgo.chatbot.EstrategiaChatbot;
import com.travelgo.chatbot.Formato;
import com.travelgo.chatbot.RespuestaChatbot;
import com.travelgo.model.PaqueteTuristico;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cuando el mensaje solo nombra un destino ("algo para 2 personas en diciembre en Cartagena"): muestra los
 * paquetes del destino. El filtro por fechas y personas llega con la búsqueda del Integrante 1; hasta
 * entonces el chatbot lo avisa.
 */
@Component
@Order(80)
public class PaquetesDestinoEstrategia implements EstrategiaChatbot {

	static final int MAXIMO = 5;

	private final BuscadorPaquetes buscador;

	public PaquetesDestinoEstrategia(BuscadorPaquetes buscador) {
		this.buscador = buscador;
	}

	@Override
	public String nombre() {
		return "PAQUETES_DESTINO";
	}

	@Override
	public boolean aplica(DatosMensaje datos) {
		return datos.destino() != null || datos.mes() != null || datos.personas() != null
			|| datos.contiene("paquete", "viaje", "viajar", "destino");
	}

	@Override
	public RespuestaChatbot responder(DatosMensaje datos, ContextoChat contexto) {
		List<PaqueteTuristico> paquetes = buscador.porDestino(datos.destino());
		if (paquetes.isEmpty()) {
			return RespuestaChatbot.de("No encontré paquetes" + (datos.destino() != null ? " para " + datos.destino() : "") + ".");
		}
		StringBuilder sb = new StringBuilder("Estos son los paquetes"
			+ (datos.destino() != null ? " de " + datos.destino() : " disponibles") + ":\n");
		sb.append(Formato.paquetes(paquetes, MAXIMO));
		if (datos.mes() != null || datos.personas() != null) {
			sb.append("\nAún no puedo filtrar por fechas ni por número de personas; los precios son por paquete.");
		}
		return RespuestaChatbot.de(sb.toString());
	}
}
