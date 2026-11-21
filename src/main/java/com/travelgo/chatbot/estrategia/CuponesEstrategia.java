package com.travelgo.chatbot.estrategia;

import com.travelgo.chatbot.ContextoChat;
import com.travelgo.chatbot.DatosMensaje;
import com.travelgo.chatbot.EstrategiaChatbot;
import com.travelgo.chatbot.Formato;
import com.travelgo.chatbot.RespuestaChatbot;
import com.travelgo.model.Cupon;
import com.travelgo.model.TipoCupon;
import com.travelgo.service.CuponService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/** "¿Hay cupones?" Lista los cupones vigentes. */
@Component
@Order(30)
public class CuponesEstrategia implements EstrategiaChatbot {

	private final CuponService cuponService;

	public CuponesEstrategia(CuponService cuponService) {
		this.cuponService = cuponService;
	}

	@Override
	public String nombre() {
		return "CUPONES";
	}

	@Override
	public boolean aplica(DatosMensaje datos) {
		return datos.contiene("cupon", "descuento", "promocion", "promociones", "oferta");
	}

	@Override
	public RespuestaChatbot responder(DatosMensaje datos, ContextoChat contexto) {
		LocalDate hoy = LocalDate.now();
		List<Cupon> vigentes = cuponService.listar().stream().filter(c -> c.estaVigente(hoy)).toList();
		if (vigentes.isEmpty()) {
			return RespuestaChatbot.de("Por ahora no hay cupones vigentes.");
		}
		StringBuilder sb = new StringBuilder("Estos cupones están vigentes:\n");
		for (Cupon c : vigentes) {
			String descuento = c.getTipo() == TipoCupon.PORCENTAJE
				? String.format("%.0f %% de descuento", c.getValor())
				: Formato.cop(c.getValor()) + " de descuento";
			sb.append("• ").append(c.getCodigo()).append(": ").append(descuento).append(" (hasta el ")
				.append(c.getFechaFin().format(Formato.FECHA)).append(")\n");
		}
		sb.append("Escribe el código al pagar para aplicarlo.");
		return RespuestaChatbot.de(sb.toString());
	}
}
