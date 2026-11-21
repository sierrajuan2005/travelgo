package com.travelgo.chatbot.estrategia;

import com.travelgo.chatbot.ContextoChat;
import com.travelgo.chatbot.DatosMensaje;
import com.travelgo.chatbot.EstrategiaChatbot;
import com.travelgo.chatbot.RespuestaChatbot;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Siempre va de última: si nada más aplica, ofrece las opciones que el chatbot sí maneja. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class NoEntendidaEstrategia implements EstrategiaChatbot {

	@Override
	public String nombre() {
		return "NO_ENTENDIDA";
	}

	@Override
	public boolean aplica(DatosMensaje datos) {
		return true;
	}

	@Override
	public RespuestaChatbot responder(DatosMensaje datos, ContextoChat contexto) {
		return RespuestaChatbot.de("No entendí tu pregunta. Puedo ayudarte con paquetes por destino (baratos o con "
			+ "hoteles de lujo), qué incluye un paquete, cupones vigentes, cómo pagar y el estado de tus reservas "
			+ "y pagos.");
	}
}
