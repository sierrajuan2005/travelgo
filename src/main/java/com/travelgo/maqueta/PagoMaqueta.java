package com.travelgo.maqueta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pago de ejemplo con los mismos atributos que Pago del diagrama de clases.
 * Aplica las reglas del Sprint 3: una reserva se paga una sola vez, no se paga una reserva cancelada,
 * y el pago exitoso confirma la reserva y genera la factura.
 * Se elimina cuando existan Pago y PagoService.
 */
public class PagoMaqueta {

	public static final String TARJETA = "Tarjeta";
	public static final String TRANSFERENCIA = "Transferencia";

	private static final String APROBADO = "APROBADO";

	private static final List<PagoMaqueta> PAGOS = new ArrayList<>();

	private final String id;
	private final double monto;
	private final LocalDate fechaPago;
	private final String metodo;
	private final String estado;
	private final ReservaMaqueta reserva;
	private final FacturaMaqueta factura;

	private PagoMaqueta(String id, ReservaMaqueta reserva, String metodo) {
		this.id = id;
		this.monto = reserva.calcularTotal();
		this.fechaPago = LocalDate.now();
		this.metodo = metodo;
		this.estado = APROBADO;
		this.reserva = reserva;
		this.factura = FacturaMaqueta.generar(reserva);
	}

	/**
	 * Paga una reserva por su total: la confirma y genera su factura.
	 *
	 * @param reserva reserva a pagar
	 * @param metodo  Tarjeta o Transferencia
	 * @return el pago registrado, con su factura
	 * @throws IllegalArgumentException si el método de pago está vacío
	 * @throws IllegalStateException    si la reserva está cancelada o ya está pagada
	 */
	public static PagoMaqueta registrar(ReservaMaqueta reserva, String metodo) {
		if (metodo == null || metodo.isBlank()) {
			throw new IllegalArgumentException("Elige un método de pago.");
		}
		validar(reserva);

		reserva.confirmar();
		PagoMaqueta pago = new PagoMaqueta("p" + (PAGOS.size() + 1), reserva, metodo);
		PAGOS.add(pago);
		return pago;
	}

	/**
	 * Revisa que la reserva se pueda pagar: no puede estar cancelada ni pagada.
	 *
	 * @throws IllegalStateException con el motivo si no se puede pagar
	 */
	public static void validar(ReservaMaqueta reserva) {
		if (ReservaMaqueta.CANCELADA.equals(reserva.getEstado())) {
			throw new IllegalStateException("No se puede pagar una reserva cancelada.");
		}
		if (ReservaMaqueta.CONFIRMADA.equals(reserva.getEstado())) {
			throw new IllegalStateException("La reserva ya está pagada.");
		}
	}

	public String getId() {
		return id;
	}

	public double getMonto() {
		return monto;
	}

	public LocalDate getFechaPago() {
		return fechaPago;
	}

	public String getMetodo() {
		return metodo;
	}

	public String getEstado() {
		return estado;
	}

	public ReservaMaqueta getReserva() {
		return reserva;
	}

	public FacturaMaqueta getFactura() {
		return factura;
	}

}
