package com.recetas.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.recetas.app.repository")
@EnableTransactionManagement
public class JpaConfig {
    // La configuración básica se toma de application.properties
}
