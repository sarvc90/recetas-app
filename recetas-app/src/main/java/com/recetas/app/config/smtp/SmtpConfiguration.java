package com.recetas.app.config.smtp;

import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.mailer.MailerBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = SmtpConfigurationProperties.class)
public class SmtpConfiguration {

    // Timeout en milisegundos para la conexión y sesión SMTP
    private static final int SMTP_TIMEOUT_MS = 10_000; // 10 segundos

    @Bean
    public SmtpProperties smtpProperties(SmtpConfigurationProperties properties) {
        return new SmtpProperties(
                properties.getHost(),
                properties.getPort(),
                properties.getUsername(),
                properties.getPassword()
        );
    }

    /**
     * Bean singleton del Mailer con timeouts configurados.
     * Reutilizar el mismo Mailer evita crear una conexión TCP nueva en cada envío.
     * Los timeouts garantizan que nunca se quede bloqueado indefinidamente.
     */
    @Bean(destroyMethod = "close")
    public Mailer mailer(SmtpProperties props) {
        return MailerBuilder
                .withSMTPServer(props.getHost(), props.getPort(), props.getUsername(), props.getPassword())
                .withTransportStrategy(TransportStrategy.SMTP_TLS)
                // Cuánto tiempo esperar para establecer la conexión TCP con el servidor SMTP
                .withSessionTimeout(SMTP_TIMEOUT_MS)
                // Pool de conexiones: mantiene hasta 4 conexiones SMTP reutilizables
                .withConnectionPoolCoreSize(1)
                .withConnectionPoolMaxSize(4)
                // Tiempo que una conexión puede estar inactiva antes de cerrarla
                .withConnectionPoolClaimTimeoutMillis(SMTP_TIMEOUT_MS)
                .withDebugLogging(false)
                .buildMailer();
    }
}
