package com.recetas.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:null}")
    private List<String> allowedOrigins;

    @Value("${cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private List<String> allowedOriginPatterns;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Origenes exactos permitidos (configurables por perfil)
        allowedOrigins.stream()
            .filter(origin -> origin != null && !origin.isBlank())
            .forEach(config::addAllowedOrigin);

        // Patrones de origen permitidos (ej: localhost en cualquier puerto)
        allowedOriginPatterns.stream()
            .filter(pattern -> pattern != null && !pattern.isBlank())
            .forEach(config::addAllowedOriginPattern);

        // Permitir todos los headers
        config.addAllowedHeader("*");

        // Permitir todos los metodos (GET, POST, PUT, DELETE, etc.)
        config.addAllowedMethod("*");

        // Permitir cookies/credenciales
        config.setAllowCredentials(true);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
