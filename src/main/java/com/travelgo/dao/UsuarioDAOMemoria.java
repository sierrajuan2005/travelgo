package com.travelgo.dao;

import com.travelgo.model.Usuario;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class UsuarioDAOMemoria implements UsuarioDAO {

    private final Map<String, Usuario> almacen = new HashMap<>();

    public UsuarioDAOMemoria() {
        // Usuario de prueba: descomenta y ajusta el nombre del rol a tu enum RolUsuario
        // guardar(new Usuario("1", "Juan", "juan@correo.com", "123456", RolUsuario.CLIENTE));
    }

    @Override
    public synchronized Usuario guardar(Usuario usuario) {
        almacen.put(usuario.getId(), usuario);
        return usuario;
    }

    @Override
    public synchronized Optional<Usuario> buscarPorId(String id) {
        return Optional.ofNullable(almacen.get(id));
    }
}