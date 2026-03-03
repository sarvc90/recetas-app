package com.recetas.app.config.smtp;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = SmtpConfigurationProperties.class)
public class SmtpConfiguration {

    @Bean
    public SmtpProperties smtpProperties(SmtpConfigurationProperties properties) {
        return SmtpProperties.builder()
                .host(properties.getHost())
                .port(properties.getPort())
                .username(properties.getUsername())
                .password(properties.getPassword())
                .build();
    }

}
