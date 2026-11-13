package com.travelgo.service;
import com.travelgo.dao.DestinoDAO;
import com.travelgo.dao.PaqueteDAO;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Destino;
import com.travelgo.model.PaqueteTuristico;
import org.springframework.stereotype.Service;

import com.travelgo.util.Textos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Reglas de negocio del catálogo de paquetes turísticos. */
@Service
public class PaqueteService {

    private final PaqueteDAO paqueteDAO;
    private final DestinoDAO destinoDAO;

    public PaqueteService(PaqueteDAO paqueteDAO, DestinoDAO destinoDAO) {
        this.paqueteDAO = paqueteDAO;
        this.destinoDAO = destinoDAO;
    }

    public PaqueteTuristico crear(PaqueteTuristico paquete) {
        paquete.setId(null);
        paquete.setDestinos(resolverDestinos(paquete.getDestinos()));
        return paqueteDAO.guardar(paquete);
    }

    public PaqueteTuristico obtener(Long id) {
        return paqueteDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paquete no encontrado: " + id));
    }

    public List<PaqueteTuristico> listar(boolean soloActivos) {
        return soloActivos ? paqueteDAO.listarActivos() : paqueteDAO.listarTodos();
    }

    public PaqueteTuristico actualizar(Long id, PaqueteTuristico datos) {
        PaqueteTuristico existente = obtener(id);
        existente.setNombre(datos.getNombre());
        existente.setDescripcion(datos.getDescripcion());
        existente.setPrecio(datos.getPrecio());
        existente.setDuracionDias(datos.getDuracionDias());
        existente.setActivo(datos.isActivo());
        existente.setDestinos(resolverDestinos(datos.getDestinos()));
        return paqueteDAO.guardar(existente);
    }

    /** Baja lógica: el paquete deja de mostrarse en el catálogo pero conserva su historial. */
    public void desactivar(Long id) {
        PaqueteTuristico paquete = obtener(id);
        paquete.setActivo(false);
        paqueteDAO.guardar(paquete);
    }

    /**
     * Búsqueda con filtros sobre los paquetes activos guardados (Sprint 4, Integrante 4).
     * Todos los filtros son opcionales (null = no filtrar) y se combinan con Y.
     * El destino se compara sin tildes ni mayúsculas contra nombre, ciudad y país.
     *
     * @param destino      texto contenido en el nombre, ciudad o país de algún destino del paquete
     * @param precioMin    precio mínimo (incluido)
     * @param precioMax    precio máximo (incluido)
     * @param duracionMin  duración mínima en días (incluida)
     * @param duracionMax  duración máxima en días (incluida)
     * @throws IllegalArgumentException si un rango queda al revés (mínimo mayor que máximo)
     */
    public List<PaqueteTuristico> buscar(String destino, BigDecimal precioMin, BigDecimal precioMax,
                                         Integer duracionMin, Integer duracionMax) {
        if (precioMin != null && precioMax != null && precioMin.compareTo(precioMax) > 0) {
            throw new IllegalArgumentException("El precio mínimo no puede ser mayor que el máximo.");
        }
        if (duracionMin != null && duracionMax != null && duracionMin > duracionMax) {
            throw new IllegalArgumentException("La duración mínima no puede ser mayor que la máxima.");
        }
        String texto = destino == null || destino.isBlank() ? null : Textos.normalizar(destino.trim());
        List<PaqueteTuristico> resultado = new ArrayList<>();
        for (PaqueteTuristico p : paqueteDAO.listarActivos()) {
            if (precioMin != null && p.getPrecio().compareTo(precioMin) < 0) continue;
            if (precioMax != null && p.getPrecio().compareTo(precioMax) > 0) continue;
            if (duracionMin != null && p.getDuracionDias() < duracionMin) continue;
            if (duracionMax != null && p.getDuracionDias() > duracionMax) continue;
            if (texto != null && !tieneDestino(p, texto)) continue;
            resultado.add(p);
        }
        return resultado;
    }

    private boolean tieneDestino(PaqueteTuristico paquete, String textoNormalizado) {
        for (Destino d : paquete.getDestinos()) {
            if (contiene(d.getNombre(), textoNormalizado) || contiene(d.getCiudad(), textoNormalizado)
                    || contiene(d.getPais(), textoNormalizado)) {
                return true;
            }
        }
        return false;
    }

    private boolean contiene(String campo, String textoNormalizado) {
        return campo != null && Textos.normalizar(campo).contains(textoNormalizado);
    }

    private List<Destino> resolverDestinos(List<Destino> recibidos) {
        List<Destino> resueltos = new ArrayList<>();
        if (recibidos == null) {
            return resueltos;
        }
        for (Destino d : recibidos) {
            resueltos.add(destinoDAO.buscarPorId(d.getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Destino no encontrado: " + d.getId())));
        }
        return resueltos;
    }
}
