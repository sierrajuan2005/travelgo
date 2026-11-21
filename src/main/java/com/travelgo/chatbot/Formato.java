package com.travelgo.chatbot;

import com.travelgo.model.PaqueteTuristico;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Ayudas para escribir precios, fechas y listas en las respuestas del chatbot. */
public final class Formato {

	public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private Formato() {
	}

	/** Pesos colombianos con punto de miles: $1.200.000 COP. */
	public static String cop(double valor) {
		return String.format(Locale.US, "$%,.0f COP", valor).replace(',', '.');
	}

	public static String cop(BigDecimal valor) {
		return cop(valor.doubleValue());
	}

	/** Lista de hasta {@code maximo} paquetes, con su precio como referencia. */
	public static String paquetes(List<PaqueteTuristico> paquetes, int maximo) {
		StringBuilder sb = new StringBuilder();
		int mostrados = 0;
		for (PaqueteTuristico p : paquetes) {
			if (mostrados == maximo) {
				break;
			}
			sb.append("• ").append(p.getNombre()).append(" — ").append(p.getDuracionDias()).append(" días — ")
				.append(cop(p.getPrecio())).append(" (precio de referencia)\n");
			mostrados++;
		}
		if (paquetes.size() > maximo) {
			sb.append("… y ").append(paquetes.size() - maximo).append(" más.\n");
		}
		return sb.toString().trim();
	}
}
