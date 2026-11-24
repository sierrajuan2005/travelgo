package com.travelgo.chatbot;

/**
 * Una estrategia por tipo de pregunta (patrón Strategy). El motor recorre las estrategias en orden y usa la
 * primera que {@code aplica}. Agregar un tipo de pregunta nuevo es crear otra estrategia, sin tocar el motor.
 */
public interface EstrategiaChatbot {

	/** Nombre corto de la intención; se guarda con cada respuesta. */
	String nombre();

	boolean aplica(DatosMensaje datos);

	RespuestaChatbot responder(DatosMensaje datos, ContextoChat contexto);
}
