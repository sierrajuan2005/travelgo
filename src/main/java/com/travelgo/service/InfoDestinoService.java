package com.travelgo.service;

import com.travelgo.util.Textos;
import com.travelgo.model.Destino;
import com.travelgo.proveedor.destinos.Atraccion;
import com.travelgo.proveedor.destinos.CacheConVencimiento;
import com.travelgo.proveedor.destinos.Clima;
import com.travelgo.proveedor.destinos.Consulta;
import com.travelgo.proveedor.destinos.Coordenadas;
import com.travelgo.proveedor.destinos.FotoDestino;
import com.travelgo.proveedor.destinos.Geocodificador;
import com.travelgo.proveedor.destinos.InfoDestino;
import com.travelgo.proveedor.destinos.ProveedorAtracciones;
import com.travelgo.proveedor.destinos.ProveedorClima;
import com.travelgo.proveedor.destinos.ProveedorExternoException;
import com.travelgo.proveedor.destinos.ProveedorFotos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Información real de los destinos (ProveedorDestinos del Sprint 4): atracciones, clima y fotos.
 * <ul>
 *   <li>Depende solo de las interfaces de los proveedores, no de una API concreta (inversión de dependencias).</li>
 *   <li>Guarda lo consultado en caché para cuidar las cuotas gratuitas.</li>
 *   <li>Si una API falla, el resto sigue funcionando: {@link #consultar(Long)} nunca lanza errores de las APIs.</li>
 * </ul>
 */
@Service
public class InfoDestinoService {

	private static final Logger LOG = LoggerFactory.getLogger(InfoDestinoService.class);

	/** Cantidad de atracciones que se muestran por destino. */
	public static final int MAX_ATRACCIONES = 8;

	private final DestinoService destinoService;
	private final Geocodificador geocodificador;
	private final ProveedorAtracciones proveedorAtracciones;
	private final ProveedorClima proveedorClima;
	private final ProveedorFotos proveedorFotos;

	// El clima cambia rápido; las ciudades, las atracciones y las fotos casi nunca
	private final CacheConVencimiento<String, Optional<Coordenadas>> cacheUbicaciones =
		new CacheConVencimiento<>(Duration.ofDays(7));
	private final CacheConVencimiento<String, Consulta<List<Atraccion>>> cacheAtracciones =
		new CacheConVencimiento<>(Duration.ofHours(24));
	private final CacheConVencimiento<String, Consulta<Clima>> cacheClima =
		new CacheConVencimiento<>(Duration.ofHours(1));
	private final CacheConVencimiento<String, Optional<Consulta<FotoDestino>>> cacheFotos =
		new CacheConVencimiento<>(Duration.ofHours(24));

	public InfoDestinoService(DestinoService destinoService, Geocodificador geocodificador,
			ProveedorAtracciones proveedorAtracciones, ProveedorClima proveedorClima,
			ProveedorFotos proveedorFotos) {
		this.destinoService = destinoService;
		this.geocodificador = geocodificador;
		this.proveedorAtracciones = proveedorAtracciones;
		this.proveedorClima = proveedorClima;
		this.proveedorFotos = proveedorFotos;
	}

	/**
	 * Junta la foto, el clima y las atracciones del destino. Lo que falle queda en los avisos.
	 *
	 * @throws com.travelgo.exception.RecursoNoEncontradoException si el destino no existe
	 */
	public InfoDestino consultar(Long destinoId) {
		return consultar(destinoService.obtener(destinoId));
	}

	/**
	 * Igual que {@link #consultar(Long)}, con el destino ya cargado.
	 */
	public InfoDestino consultar(Destino destino) {
		List<String> avisos = new ArrayList<>();

		Consulta<FotoDestino> foto = null;
		try {
			foto = foto(destino).orElse(null);
		} catch (ProveedorExternoException e) {
			avisos.add(aviso("la foto", e));
		}

		Coordenadas ubicacion = null;
		try {
			ubicacion = ubicar(destino);
		} catch (ProveedorExternoException e) {
			avisos.add(aviso("la ubicación del destino", e));
		}

		Consulta<Clima> clima = null;
		Consulta<List<Atraccion>> atracciones = null;
		if (ubicacion != null) {
			try {
				clima = clima(destino);
			} catch (ProveedorExternoException e) {
				avisos.add(aviso("el clima", e));
			}
			try {
				atracciones = atracciones(destino);
			} catch (ProveedorExternoException e) {
				avisos.add(aviso("las atracciones", e));
			}
		}
		return new InfoDestino(destino.getId(), destino.getNombre(), destino.getCiudad(), destino.getPais(),
			ubicacion, foto, clima, atracciones, avisos);
	}

	/**
	 * Atracciones cerca del centro de la ciudad del destino.
	 *
	 * @throws ProveedorExternoException si la API falla o no se encuentra la ciudad
	 */
	public Consulta<List<Atraccion>> atracciones(Destino destino) {
		Coordenadas centro = ubicar(destino);
		return cacheAtracciones.obtener(claveCiudad(destino), () -> Consulta.de(
			proveedorAtracciones.buscarAtracciones(centro, MAX_ATRACCIONES), proveedorAtracciones));
	}

	/**
	 * Clima actual y pronóstico de la ciudad del destino.
	 *
	 * @throws ProveedorExternoException si la API falla o no se encuentra la ciudad
	 */
	public Consulta<Clima> clima(Destino destino) {
		Coordenadas centro = ubicar(destino);
		return cacheClima.obtener(claveCiudad(destino), () -> Consulta.de(
			proveedorClima.consultarClima(centro), proveedorClima));
	}

	/**
	 * Foto del destino. Primero busca por el nombre del destino ("Cartagena de Indias") y, si no hay un
	 * artículo con ese nombre ("Cusco y Machu Picchu"), por la ciudad ("Cusco").
	 *
	 * @return la foto, o vacío si no se encontró ninguna
	 * @throws ProveedorExternoException si la API falla
	 */
	public Optional<Consulta<FotoDestino>> foto(Destino destino) {
		Set<String> nombres = new LinkedHashSet<>();
		agregarSiHay(nombres, destino.getNombre());
		agregarSiHay(nombres, destino.getCiudad());
		return cacheFotos.obtener(Textos.normalizar(String.join("|", nombres)), () -> {
			for (String nombre : nombres) {
				Optional<FotoDestino> foto = proveedorFotos.buscarFoto(nombre);
				if (foto.isPresent()) {
					return Optional.of(Consulta.de(foto.get(), proveedorFotos));
				}
			}
			return Optional.empty();
		});
	}

	/**
	 * Foto del destino para las tarjetas del catálogo: si la API falla, devuelve vacío y la tarjeta usa
	 * el ícono de siempre.
	 */
	public Optional<Consulta<FotoDestino>> fotoSiSePuede(Destino destino) {
		try {
			return foto(destino);
		} catch (ProveedorExternoException e) {
			LOG.warn("No se pudo consultar la foto de {}: {}", destino.getNombre(), e.getMessage());
			return Optional.empty();
		}
	}

	/**
	 * Coordenadas del centro de la ciudad del destino.
	 *
	 * @throws ProveedorExternoException si la API falla o la ciudad no existe en la API
	 */
	private Coordenadas ubicar(Destino destino) {
		return cacheUbicaciones.obtener(claveCiudad(destino),
				() -> geocodificador.ubicar(destino.getCiudad(), destino.getPais()))
			.orElseThrow(() -> new ProveedorExternoException(geocodificador.nombreFuente(),
				"No se encontró la ciudad " + destino.getCiudad() + " (" + destino.getPais() + ") en "
					+ geocodificador.nombreFuente() + ".", null));
	}

	private String aviso(String que, ProveedorExternoException e) {
		LOG.warn("No se pudo consultar {}: {}", que, e.getMessage());
		return "No se pudo consultar " + que + ": " + e.getMessage();
	}

	private static String claveCiudad(Destino destino) {
		return Textos.normalizar(destino.getCiudad() + "|" + destino.getPais());
	}

	private static void agregarSiHay(Set<String> nombres, String nombre) {
		if (nombre != null && !nombre.isBlank()) {
			nombres.add(nombre.trim());
		}
	}

}
