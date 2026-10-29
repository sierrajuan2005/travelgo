package com.travelgo.config;

import com.travelgo.model.Usuario;
import com.travelgo.service.UsuarioService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/*
    Servicio de detalles de usuario para la autenticación con Spring Security.
 */
@Service
public class ServicioUsuarioDetalles implements UserDetailsService {
    private final UsuarioService usuarioService;

    /*
     * Constructor del servicio de detalles de usuario.
     */
    public ServicioUsuarioDetalles(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }


    /*Busca un usuario por su correo electrónico y carga su rol */
    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioService.buscarPorCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + correo));

        return User.withUsername(usuario.getCorreo())
                .password(usuario.getPassword())
                .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()))
                .build();
    }
}
