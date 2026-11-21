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

/** "Paquetes baratos para Bogotá." Muestra el tercio más barato de los paquetes del destino. */
@Component
@Order(70)
public class PaquetesBaratosEstrategia implements EstrategiaChatbot {

	static final int MAXIMO = 5;

	private final BuscadorPaquetes buscador;

	public PaquetesBaratosEstrategia(BuscadorPaquetes buscador) {
		this.buscador = buscador;
	}

	@Override
	public String nombre() {
		return "PAQUETES_BARATOS";
	}

	@Override
	public boolean aplica(DatosMensaje datos) {
		return datos.barato();
	}

	@Override
	public RespuestaChatbot responder(DatosMensaje datos, ContextoChat contexto) {
		List<PaqueteTuristico> baratos = buscador.masBaratos(datos.destino());
		if (baratos.isEmpty()) {
			return RespuestaChatbot.de("No encontré paquetes"
				+ (datos.destino() != null ? " para " + datos.destino() : "") + ".");
		}
		return RespuestaChatbot.de("Los paquetes más baratos" + (datos.destino() != null ? " para " + datos.destino() : "")
			+ ":\n" + Formato.paquetes(baratos, MAXIMO));
	}
}
