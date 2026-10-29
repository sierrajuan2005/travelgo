package com.travelgo.service;

import com.travelgo.model.Usuario;
import com.travelgo.dao.UsuarioDAO;
import org.springframework.stereotype.Service;
import java.util.Optional;


/* Servicio para la gestión de usuarios. */
@Service
public class UsuarioService {
    private final UsuarioDAO usuarioDAO;
    
    /*Construye el servicio de usuarios */
    public UsuarioService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    /*Guarda un usuario */
    public Usuario guardar(Usuario usuario) {
        return usuarioDAO.guardar(usuario);
    }

    /*Busca un usuario por su ID */
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioDAO.buscarPorId(id);
    }

    /*Busca un usuario por su correo electrónico */
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioDAO.buscarPorCorreo(correo);
    }
}
