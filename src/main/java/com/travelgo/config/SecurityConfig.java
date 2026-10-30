package com.travelgo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

/**
 * Configuración base de seguridad: rutas públicas y página de login propia.
 * Integrante 1 la amplía con el UserDetailsService, el PasswordEncoder y los permisos por rol.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/login", "/registro", "/error",
                    "/catalogo", "/catalogo/**",
                    "/css/**", "/js/**", "/img/**"
                ).permitAll()

                // Administración
                .requestMatchers("/admin/**")
                    .hasRole("ADMINISTRADOR")

                // Gestión de agentes
                .requestMatchers("/agente/**")
                    .hasAnyRole("AGENTE", "ADMINISTRADOR")

                // Área del cliente
                .requestMatchers("/cliente/**")
                    .hasRole("CLIENTE")

                // Destinos: consulta autenticada
                .requestMatchers(HttpMethod.GET, "/api/destinos/**")
                    .authenticated()
                .requestMatchers("/api/destinos/**")
                    .hasRole("ADMINISTRADOR")

                // Paquetes: consulta autenticada
                .requestMatchers(HttpMethod.GET, "/api/paquetes/**")
                    .authenticated()
                .requestMatchers("/api/paquetes/**")
                    .hasRole("ADMINISTRADOR")

                // Servicios: consulta autenticada
                .requestMatchers(HttpMethod.GET, "/api/servicios/**")
                    .authenticated()
                .requestMatchers("/api/servicios/**")
                    .hasAnyRole("AGENTE", "ADMINISTRADOR")

                // Reservas: usuarios autenticados.
                // El propietario se verifica en ReservaService.
                .requestMatchers(HttpMethod.POST, "/api/reservas/**")
                    .hasRole("CLIENTE")

                .requestMatchers(HttpMethod.GET, "/api/reservas/**")
                    .hasAnyRole("CLIENTE", "AGENTE", "ADMINISTRADOR")

                .requestMatchers(HttpMethod.PUT, "/api/reservas/**")
                    .hasAnyRole("CLIENTE", "AGENTE", "ADMINISTRADOR")

                .requestMatchers("/api/reservas/**")
                    .denyAll()

                // Cualquier otra ruta
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

		return http.build();
	}
	/**
     * Proporciona el codificador BCrypt para las contraseñas.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

	@Bean
	public org.springframework.security.authentication.AuthenticationManager authenticationManager(org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
}
