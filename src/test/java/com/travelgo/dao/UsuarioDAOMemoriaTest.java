package com.travelgo.dao;

import com.travelgo.model.RolUsuario;
import com.travelgo.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del DAO en memoria de usuarios. Son las mismas que deberá pasar el DAO JDBC
 * cuando se migre (la búsqueda por correo es la que usa el login y la creación de reservas).
 */
class UsuarioDAOMemoriaTest {

    private UsuarioDAOMemoria dao;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        dao = new UsuarioDAOMemoria();
        ana = new Usuario("u1", "Ana", "ana@correo.com", "secreto", RolUsuario.CLIENTE);
    }

    @Test
    @DisplayName("Guardar un usuario permite buscarlo por id")
    void guardarYBuscarPorId() {
        dao.guardar(ana);

        assertSame(ana, dao.buscarPorId("u1").orElseThrow());
    }

    @Test
    @DisplayName("Buscar un id que no existe devuelve vacío")
    void buscarPorIdInexistente() {
        assertTrue(dao.buscarPorId("no-existe").isEmpty());
    }

    @Test
    @DisplayName("Buscar por correo encuentra al usuario sin importar mayúsculas")
    void buscarPorCorreoSinMayusculas() {
        dao.guardar(ana);

        assertSame(ana, dao.buscarPorCorreo("ANA@Correo.COM").orElseThrow());
    }

    @Test
    @DisplayName("Buscar por correo que no existe devuelve vacío")
    void buscarPorCorreoInexistente() {
        dao.guardar(ana);

        assertTrue(dao.buscarPorCorreo("otro@correo.com").isEmpty());
    }

    @Test
    @DisplayName("Buscar por correo no confunde el correo con el id")
    void elCorreoNoEsElId() {
        dao.guardar(ana);

        assertTrue(dao.buscarPorId("ana@correo.com").isEmpty());
    }

    @Test
    @DisplayName("Guardar dos veces el mismo id reemplaza al usuario")
    void guardarReemplaza() {
        dao.guardar(ana);
        Usuario modificada = new Usuario("u1", "Ana María", "ana@correo.com", "secreto", RolUsuario.AGENTE);
        dao.guardar(modificada);

        assertEquals(RolUsuario.AGENTE, dao.buscarPorId("u1").orElseThrow().getRol());
    }
}
