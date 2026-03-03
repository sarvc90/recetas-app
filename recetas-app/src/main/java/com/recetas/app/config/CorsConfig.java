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

    @Value("${cors.allowed-origins:http://localhost:63342,http://localhost:63343}")
    private List<String> allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Origenes permitidos (configurables por perfil)
        allowedOrigins.forEach(config::addAllowedOrigin);

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
