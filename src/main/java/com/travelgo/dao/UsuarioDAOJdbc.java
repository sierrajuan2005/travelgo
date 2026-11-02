package com.travelgo.dao;

import com.travelgo.exception.AccesoDatosException;
import com.travelgo.model.RolUsuario;
import com.travelgo.model.Usuario;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Implementación JDBC del DAO de usuarios. */
@Repository
@Primary
public class UsuarioDAOJdbc implements UsuarioDAO {

    private static final String SELECT_USUARIO = "SELECT id, nombre, correo, password, rol FROM usuario";

    private final JdbcTemplate jdbc;

    public UsuarioDAOJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        try {
            jdbc.update("""
                    INSERT INTO usuario (id, nombre, correo, password, rol)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        nombre = EXCLUDED.nombre, correo = EXCLUDED.correo,
                        password = EXCLUDED.password, rol = EXCLUDED.rol
                    """, usuario.getId(), usuario.getNombre(), usuario.getCorreo(),
                    usuario.getPassword(), usuario.getRol().name());
            return usuario;
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo guardar el usuario", e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(String id) {
        try {
            return jdbc.query(SELECT_USUARIO + " WHERE id = ?", this::mapear, id).stream().findFirst();
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo consultar el usuario " + id, e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        try {
            return jdbc.query(SELECT_USUARIO + " WHERE LOWER(correo) = LOWER(?)", this::mapear, correo)
                    .stream().findFirst();
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo consultar el usuario por correo", e);
        }
    }

    /** Convierte una fila de la tabla usuario en un objeto Usuario. */
    private Usuario mapear(ResultSet rs, int fila) throws SQLException {
        return new Usuario(rs.getString("id"), rs.getString("nombre"), rs.getString("correo"),
                rs.getString("password"), RolUsuario.valueOf(rs.getString("rol")));
    }
}