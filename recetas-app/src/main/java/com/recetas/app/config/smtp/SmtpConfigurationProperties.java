package com.recetas.app.config.smtp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "smtp")
public class SmtpConfigurationProperties {

    private String host;

    private int port;

    private String username;

    private String password;
}

