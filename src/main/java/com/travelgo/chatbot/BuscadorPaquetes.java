package com.travelgo.chatbot;

import com.travelgo.model.PaqueteTuristico;

import java.util.List;
import java.util.Optional;

/**
 * Lo que el chatbot necesita saber buscar. Es una interfaz pequeña para que, cuando el Integrante 1 entregue
 * la búsqueda por gama y fechas, solo haya que cambiar la implementación y no las estrategias.
 */
public interface BuscadorPaquetes {

	/** Paquetes activos de un destino; con destino null, todos. */
	List<PaqueteTuristico> porDestino(String destino);

	/** El tercio más barato de los paquetes del destino (o de todos si el destino es null). */
	List<PaqueteTuristico> masBaratos(String destino);

	/** Paquetes cuyo hotel es de la gama pedida. Vacío si todavía no se puede filtrar por gama. */
	Optional<List<PaqueteTuristico>> porGama(String destino, Gama gama);
}
