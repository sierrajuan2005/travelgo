package com.travelgo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** TEMPORAL: solo para probar /api/** con Postman. No subir a develop. */
@Configuration
public class ApiPruebaSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain apiPrueba(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}