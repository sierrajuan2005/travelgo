package com.travelgo.chatbot.estrategia;

import com.travelgo.chatbot.ContextoChat;
import com.travelgo.chatbot.DatosMensaje;
import com.travelgo.chatbot.EstrategiaChatbot;
import com.travelgo.chatbot.RespuestaChatbot;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** "¿Cómo pago?" Explica el flujo de pago con una respuesta fija. */
@Component
@Order(25)
public class ComoPagarEstrategia implements EstrategiaChatbot {

	static final String TEXTO = """
		Para pagar en TravelGo:
		1. Elige un paquete y haz tu reserva.
		2. Ve a "Mis reservas" y abre la factura de la reserva.
		3. Si tienes un cupón, escribe su código para aplicar el descuento.
		4. Elige el método de pago: tarjeta de crédito, tarjeta débito, transferencia o efectivo.
		El pago es simulado: no se hace ningún cobro real.""";

	@Override
	public String nombre() {
		return "COMO_PAGAR";
	}

	@Override
	public boolean aplica(DatosMensaje datos) {
		return datos.contiene("como pago", "como pagar", "como puedo pagar", "metodos de pago", "formas de pago",
			"medios de pago");
	}

	@Override
	public RespuestaChatbot responder(DatosMensaje datos, ContextoChat contexto) {
		return RespuestaChatbot.de(TEXTO);
	}
}
