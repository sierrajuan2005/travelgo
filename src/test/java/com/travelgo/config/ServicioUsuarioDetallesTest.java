package com.travelgo.config;

import com.travelgo.model.RolUsuario;
import com.travelgo.model.Usuario;
import com.travelgo.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Pruebas del login por rol: el UserDetailsService debe cargar al usuario por correo
 * y convertir su rol en la autoridad ROLE_<ROL> que usa Spring Security.
 */
@ExtendWith(MockitoExtension.class)
class ServicioUsuarioDetallesTest {

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private ServicioUsuarioDetalles servicio;

    @ParameterizedTest
    @EnumSource(RolUsuario.class)
    @DisplayName("Cada rol se carga como la autoridad ROLE_<ROL>, con el correo como username")
    void cargaElRolComoAutoridad(RolUsuario rol) {
        Usuario usuario = new Usuario("u1", "Ana", "ana@correo.com", "$2a$10$hash", rol);
        when(usuarioService.buscarPorCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));

        UserDetails detalles = servicio.loadUserByUsername("ana@correo.com");

        List<String> autoridades = detalles.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();
        assertEquals(List.of("ROLE_" + rol.name()), autoridades);
        assertEquals("ana@correo.com", detalles.getUsername());
        assertEquals("$2a$10$hash", detalles.getPassword());
    }

    @Test
    @DisplayName("Un correo que no existe lanza UsernameNotFoundException")
    void usuarioInexistente() {
        when(usuarioService.buscarPorCorreo("nadie@correo.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> servicio.loadUserByUsername("nadie@correo.com"));
    }
}
