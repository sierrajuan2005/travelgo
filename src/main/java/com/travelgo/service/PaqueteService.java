package com.travelgo.service;
import com.travelgo.dao.DestinoDAO;
import com.travelgo.dao.PaqueteDAO;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Destino;
import com.travelgo.model.PaqueteTuristico;
import org.springframework.stereotype.Service;

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
