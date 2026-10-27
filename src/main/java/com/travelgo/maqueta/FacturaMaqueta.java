package com.travelgo.maqueta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Factura de ejemplo con los mismos atributos que Factura del diagrama de clases.
 * Solo la crea PagoMaqueta, cuando un pago es exitoso.
 * Se elimina cuando existan Factura y FacturaService.
 */
public class FacturaMaqueta {

	private static final String PAGADA = "PAGADA";

	private static final List<FacturaMaqueta> FACTURAS = new ArrayList<>();

	private final String id;
	private final String numero;
	private final LocalDate fechaEmision;
	private final double subtotal;
	private final double total;
	private final String estado;
	private final ReservaMaqueta reserva;

	private FacturaMaqueta(int consecutivo, ReservaMaqueta reserva) {
		this.id = "f" + consecutivo;
		this.numero = String.format("F-%04d", consecutivo);
		this.fechaEmision = LocalDate.now();
		this.reserva = reserva;
		this.subtotal = reserva.calcularTotal();
		this.total = calcularTotal();
		this.estado = PAGADA;
	}

	/**
	 * Genera la factura de una reserva pagada, con el siguiente número (F-0001, F-0002...).
	 */
	static FacturaMaqueta generar(ReservaMaqueta reserva) {
		FacturaMaqueta factura = new FacturaMaqueta(FACTURAS.size() + 1, reserva);
		FACTURAS.add(factura);
		return factura;
	}

	/**
	 * Total de la factura a partir del subtotal de la reserva. Por ahora no hay impuestos ni descuentos.
	 */
	public double calcularTotal() {
		return subtotal;
	}

	public String getId() {
		return id;
	}

	public String getNumero() {
		return numero;
	}

	public LocalDate getFechaEmision() {
		return fechaEmision;
	}

	public double getSubtotal() {
		return subtotal;
	}

	public double getTotal() {
		return total;
	}

	public String getEstado() {
		return estado;
	}

	public ReservaMaqueta getReserva() {
		return reserva;
	}

}
