package com.recetas.app.service;

import com.recetas.app.config.smtp.SmtpProperties;
import com.recetas.app.dto.EmailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final SmtpProperties smtpProperties;

    public EmailService(SmtpProperties smtpProperties) {
        this.smtpProperties = smtpProperties;
    }

    public void sendMail(EmailDto emailDTO) throws Exception {
        log.info("========== EMAIL SERVICE - INICIO ==========");
        log.info("SMTP Host: {}", smtpProperties.getHost());
        log.info("SMTP Port: {}", smtpProperties.getPort());
        log.info("SMTP Username: {}", smtpProperties.getUsername());
        log.info("SMTP Password (length): {}, starts with: {}",
                smtpProperties.getPassword() != null ? smtpProperties.getPassword().length() : "NULL",
                smtpProperties.getPassword() != null && smtpProperties.getPassword().length() >= 4
                        ? smtpProperties.getPassword().substring(0, 4) + "****" : "NULL");
        log.info("Destinatario: {}", emailDTO.recipient());
        log.info("Asunto: {}", emailDTO.subject());
        log.info("=============================================");

        Email email = EmailBuilder.startingBlank()
                .from(smtpProperties.getUsername())
                .to(emailDTO.recipient())
                .withSubject(emailDTO.subject())
                .withPlainText(emailDTO.body())
                .buildEmail();

        try (Mailer mailer = MailerBuilder
                .withSMTPServer(smtpProperties.getHost(), smtpProperties.getPort(), smtpProperties.getUsername(), smtpProperties.getPassword())
                .withTransportStrategy(TransportStrategy.SMTP_TLS)
                .withDebugLogging(true)
                .buildMailer()) {

            mailer.sendMail(email);
            log.info("✅ Email enviado exitosamente a {}", emailDTO.recipient());
        } catch (Exception e) {
            log.error("❌ Error enviando email a {}: {}", emailDTO.recipient(), e.getMessage());
            throw e;
        }
    }
}
