package com.travelgo.recordatorio;

/**
 * Un aviso ya redactado, listo para enviar por cualquier canal.
 *
 * @param tipo       tipo de aviso
 * @param asunto     asunto del correo
 * @param texto      texto completo (correo)
 * @param textoCorto texto breve para SMS (máximo unos 160 caracteres)
 */
public record Aviso(TipoAviso tipo, String asunto, String texto, String textoCorto) {
}
