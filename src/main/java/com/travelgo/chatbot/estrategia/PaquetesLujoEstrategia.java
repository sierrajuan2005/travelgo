package com.travelgo.chatbot.estrategia;

import com.travelgo.chatbot.BuscadorPaquetes;
import com.travelgo.chatbot.ContextoChat;
import com.travelgo.chatbot.DatosMensaje;
import com.travelgo.chatbot.EstrategiaChatbot;
import com.travelgo.chatbot.Formato;
import com.travelgo.chatbot.Gama;
import com.travelgo.chatbot.RespuestaChatbot;
import com.travelgo.model.PaqueteTuristico;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * "Paquetes con hoteles de lujo en Cartagena." Depende de la gama de los hoteles (Integrante 2): mientras
 * el buscador no la soporte, lo dice con claridad en vez de inventar.
 */
@Component
@Order(60)
public class PaquetesLujoEstrategia implements EstrategiaChatbot {

	static final int MAXIMO = 5;

	private final BuscadorPaquetes buscador;

	public PaquetesLujoEstrategia(BuscadorPaquetes buscador) {
		this.buscador = buscador;
	}

	@Override
	public String nombre() {
		return "PAQUETES_LUJO";
	}

	@Override
	public boolean aplica(DatosMensaje datos) {
		return datos.gama() != null;
	}

	@Override
	public RespuestaChatbot responder(DatosMensaje datos, ContextoChat contexto) {
		Optional<List<PaqueteTuristico>> encontrados = buscador.porGama(datos.destino(), datos.gama());
		if (encontrados.isEmpty()) {
			return RespuestaChatbot.de("Todavía no puedo filtrar paquetes por la gama del hotel"
				+ (datos.gama() == Gama.LUJO ? " (lujo)" : "") + ". Mientras tanto, puedo mostrarte los paquetes"
				+ (datos.destino() != null ? " de " + datos.destino() : "") + ".");
		}
		if (encontrados.get().isEmpty()) {
			return RespuestaChatbot.de("No encontré paquetes con hoteles de esa gama"
				+ (datos.destino() != null ? " en " + datos.destino() : "") + ".");
		}
		return RespuestaChatbot.de("Estos paquetes tienen hoteles de gama " + datos.gama().name().toLowerCase() + ":\n"
			+ Formato.paquetes(encontrados.get(), MAXIMO));
	}
}
