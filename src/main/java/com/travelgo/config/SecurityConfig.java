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
 * ConfiguraciÃ³n base de seguridad: rutas pÃºblicas y pÃ¡gina de login propia.
 * Integrante 1 la amplÃ­a con el UserDetailsService, el PasswordEncoder y los permisos por rol.
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

                // ExpoSoftware "Lanza tu viaje": los visitantes no tienen cuenta.
                // /admin/expo (moderación) sigue protegido por la regla de /admin/**.
                .requestMatchers("/expo", "/expo/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/expo/destinos", "/api/expo/lanzamientos").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/expo/lanzamientos", "/api/expo/lanzamientos/*/contacto",
                        "/api/expo/lanzamientos/*/whatsapp", "/api/expo/lanzamientos/*/compartio").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/expo/lanzamientos/*/whatsapp").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/expo/lanzamientos/*/contacto").permitAll()
                .requestMatchers("/api/expo/**").denyAll()

                // Administración
                .requestMatchers("/admin/**")
                    .hasRole("ADMINISTRADOR")

                // GestiÃ³n de agentes
                .requestMatchers("/agente/**")
                    .hasAnyRole("AGENTE", "ADMINISTRADOR")

                // Ãrea del cliente
                .requestMatchers("/cliente/**")
                    .hasRole("CLIENTE")

                // Destinos: consulta autenticada
                .requestMatchers(HttpMethod.GET, "/api/destinos/**")
                    .authenticated()
                .requestMatchers("/api/destinos/**")
                    .hasRole("ADMINISTRADOR")

                // Paquetes: consulta para usuarios autenticados.
                .requestMatchers(HttpMethod.GET, "/api/paquetes/**")
                    .authenticated()
                .requestMatchers("/api/paquetes/**")
                    .hasRole("ADMINISTRADOR")

                // Paquetes reales: consulta autenticada y armado exclusivo del administrador.
                .requestMatchers(HttpMethod.GET, "/api/paquetes-reales/**")
                    .authenticated()
                .requestMatchers("/api/paquetes-reales/**")
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

                // Pagos: clientes y agentes pueden registrar pagos.
                // Solo agentes y administradores pueden consultar pagos.
                .requestMatchers(HttpMethod.POST, "/api/pagos")
                .hasAnyRole("CLIENTE", "AGENTE")

                .requestMatchers(HttpMethod.GET, "/api/pagos/**")
                    .hasAnyRole("AGENTE", "ADMINISTRADOR")

                .requestMatchers("/api/pagos/**")
                    .denyAll()

                // Vuelos reales: consulta para usuarios autenticados.
                .requestMatchers(HttpMethod.GET, "/api/vuelos-reales/**")
                    .authenticated()
                .requestMatchers("/api/vuelos-reales/**")
                    .denyAll()

                // Hoteles reales: consulta para usuarios autenticados.
                .requestMatchers(HttpMethod.GET, "/api/hoteles-reales/**")
                    .authenticated()
                .requestMatchers("/api/hoteles-reales/**")
                    .denyAll()

                // Facturas: consulta para clientes, agentes y administradores.
                // La propiedad de las facturas de clientes se valida en FacturaService.
                .requestMatchers(HttpMethod.GET, "/api/facturas/**")
                    .hasAnyRole("CLIENTE", "AGENTE", "ADMINISTRADOR")

                .requestMatchers("/api/facturas/**")
                    .denyAll()

                // Cupones: consulta para usuarios autenticados.
                .requestMatchers(HttpMethod.GET, "/api/cupones/**")
                    .authenticated()

                // Crear y desactivar cupones: solo administrador.
                .requestMatchers(HttpMethod.POST, "/api/cupones")
                    .hasRole("ADMINISTRADOR")

                .requestMatchers(HttpMethod.PUT, "/api/cupones/*/desactivar")
                    .hasRole("ADMINISTRADOR")

                .requestMatchers("/api/cupones/**")
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
     * Proporciona el codificador BCrypt para las contraseÃ±as.
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
