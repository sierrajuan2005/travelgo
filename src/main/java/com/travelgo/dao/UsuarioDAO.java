package com.travelgo.dao;

import com.travelgo.model.Usuario;

import java.util.Optional;

/** Búsqueda de usuarios. Si Integrante 1 ya tiene su DAO, usar ese y borrar este. */
public interface UsuarioDAO {

    Usuario guardar(Usuario usuario);

    Optional<Usuario> buscarPorId(String id);
}