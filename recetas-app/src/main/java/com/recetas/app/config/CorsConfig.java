package com.recetas.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:}")
    private String allowedOriginsStr;

    @Value("${cors.allowed-origin-patterns:}")
    private String allowedOriginPatternsStr;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Procesar orígenes exactos permitidos
        if (allowedOriginsStr != null && !allowedOriginsStr.isBlank()) {
            Arrays.stream(allowedOriginsStr.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .forEach(config::addAllowedOrigin);
        }

        // Procesar patrones de origen permitidos
        if (allowedOriginPatternsStr != null && !allowedOriginPatternsStr.isBlank()) {
            Arrays.stream(allowedOriginPatternsStr.split(","))
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .forEach(config::addAllowedOriginPattern);
        }

        // Permitir todos los headers
        config.addAllowedHeader("*");

        // Permitir todos los métodos (GET, POST, PUT, DELETE, etc.)
        config.addAllowedMethod("*");

        // Permitir cookies/credenciales
        config.setAllowCredentials(true);

        // Exponer headers adicionales al frontend
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));

        // Tiempo de caché para preflight requests (en segundos)
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
