package com.travelgo.chatbot.estrategia;

import com.travelgo.chatbot.ContextoChat;
import com.travelgo.chatbot.DatosMensaje;
import com.travelgo.chatbot.EstrategiaChatbot;
import com.travelgo.chatbot.Formato;
import com.travelgo.chatbot.RespuestaChatbot;
import com.travelgo.dao.UsuarioDAO;
import com.travelgo.model.EstadoFactura;
import com.travelgo.model.Factura;
import com.travelgo.service.FacturaService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** "¿Tengo pagos pendientes?" Consulta privada: solo las facturas pendientes del propio usuario. */
@Component
@Order(20)
public class PagosPendientesEstrategia implements EstrategiaChatbot {

	private final FacturaService facturaService;
	private final UsuarioDAO usuarioDAO;

	public PagosPendientesEstrategia(FacturaService facturaService, UsuarioDAO usuarioDAO) {
		this.facturaService = facturaService;
		this.usuarioDAO = usuarioDAO;
	}

	@Override
	public String nombre() {
		return "PAGOS_PENDIENTES";
	}

	@Override
	public boolean aplica(DatosMensaje datos) {
		return datos.contiene("pago pendiente", "pagos pendientes", "pendiente de pago", "pendientes de pago",
			"cuanto debo", "tengo que pagar", "mis facturas", "mi factura");
	}

	@Override
	public RespuestaChatbot responder(DatosMensaje datos, ContextoChat contexto) {
		if (!contexto.autenticado()) {
			return RespuestaChatbot.de("Para consultar tus pagos necesitas iniciar sesión en TravelGo.");
		}
		String usuarioId = usuarioDAO.buscarPorCorreo(contexto.correo()).map(u -> u.getId()).orElse(null);
		if (usuarioId == null) {
			return RespuestaChatbot.de("No encontré tu usuario. Vuelve a iniciar sesión e inténtalo de nuevo.");
		}
		List<Factura> pendientes = facturaService.listarPorUsuario(usuarioId).stream()
			.filter(f -> f.getEstado() == EstadoFactura.PENDIENTE)
			.toList();
		if (pendientes.isEmpty()) {
			return RespuestaChatbot.de("No tienes pagos pendientes.");
		}
		StringBuilder sb = new StringBuilder("Tienes " + pendientes.size() + " pago(s) pendiente(s):\n");
		for (Factura f : pendientes) {
			sb.append("• Factura ").append(f.getNumero()).append(" — ").append(Formato.cop(f.getTotal()))
				.append(" — emitida el ").append(f.getFechaEmision().format(Formato.FECHA)).append('\n');
		}
		return RespuestaChatbot.de(sb.toString().trim());
	}
}
