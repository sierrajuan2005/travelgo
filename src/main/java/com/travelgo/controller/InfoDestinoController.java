package com.travelgo.controller;

import com.travelgo.model.Destino;
import com.travelgo.proveedor.destinos.Atraccion;
import com.travelgo.proveedor.destinos.Clima;
import com.travelgo.proveedor.destinos.Consulta;
import com.travelgo.proveedor.destinos.FotoDestino;
import com.travelgo.proveedor.destinos.InfoDestino;
import com.travelgo.proveedor.destinos.ProveedorExternoException;
import com.travelgo.service.DestinoService;
import com.travelgo.service.InfoDestinoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Información real de un destino consultada a APIs externas. Solo lectura (GET), para cualquier usuario
 * autenticado (SecurityConfig: GET /api/destinos/** requiere sesión).
 * <ul>
 *   <li>{@code GET /api/destinos/{id}/info}: todo junto; si una API falla, responde 200 con avisos.</li>
 *   <li>{@code GET /api/destinos/{id}/atracciones}: atracciones (OpenTripMap o Wikipedia).</li>
 *   <li>{@code GET /api/destinos/{id}/clima}: clima actual y pronóstico (Open-Meteo).</li>
 *   <li>{@code GET /api/destinos/{id}/foto}: foto con autor y licencia (Wikimedia Commons).</li>
 * </ul>
 * Si la API de un endpoint individual falla, responde 503 con un mensaje claro.
 */
@RestController
@RequestMapping("/api/destinos/{id}")
public class InfoDestinoController {

	private final DestinoService destinoService;
	private final InfoDestinoService infoDestinoService;

	public InfoDestinoController(DestinoService destinoService, InfoDestinoService infoDestinoService) {
		this.destinoService = destinoService;
		this.infoDestinoService = infoDestinoService;
	}

	@GetMapping("/info")
	public InfoDestino info(@PathVariable Long id) {
		return infoDestinoService.consultar(id);
	}

	@GetMapping("/atracciones")
	public Consulta<List<Atraccion>> atracciones(@PathVariable Long id) {
		return infoDestinoService.atracciones(destino(id));
	}

	@GetMapping("/clima")
	public Consulta<Clima> clima(@PathVariable Long id) {
		return infoDestinoService.clima(destino(id));
	}

	@GetMapping("/foto")
	public ResponseEntity<?> foto(@PathVariable Long id) {
		Destino destino = destino(id);
		return infoDestinoService.foto(destino)
			.<ResponseEntity<?>>map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Map.of("error", "No se encontró una foto para " + destino.getNombre() + ".")));
	}

	/**
	 * La API externa falló: 503 (servicio no disponible) con la fuente y el motivo.
	 */
	@ExceptionHandler(ProveedorExternoException.class)
	public ResponseEntity<Map<String, String>> apiExternaFallo(ProveedorExternoException e) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
			.body(Map.of("error", e.getMessage(), "fuente", e.getFuente()));
	}

	private Destino destino(Long id) {
		return destinoService.obtener(id);
	}
}
