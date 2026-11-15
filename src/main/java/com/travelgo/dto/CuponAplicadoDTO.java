package com.travelgo.dto;

/** Vista previa de lo que descontaría un cupón sobre un subtotal. */
public record CuponAplicadoDTO(String codigo, double subtotal, double descuento, double totalConDescuento) {
}