package com.recetas.app.service;

import com.recetas.app.config.smtp.SmtpProperties;
import com.recetas.app.dto.EmailDto;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.email.EmailBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final SmtpProperties smtpProperties;
    private final Mailer mailer;

    public EmailService(SmtpProperties smtpProperties, Mailer mailer) {
        this.smtpProperties = smtpProperties;
        this.mailer = mailer;
    }

    /**
     * Envía un correo de forma síncrona.
     * Lanza excepción si el envío falla, para que el llamador pueda reaccionar
     * (p. ej. invalidar el código de verificación generado).
     * El timeout de sesión está configurado en SmtpConfiguration (10 s),
     * por lo que nunca se bloqueará indefinidamente.
     */
    @Async
    public void sendMail(EmailDto emailDTO) throws Exception {
        log.info("Enviando email a {} — asunto: {}", emailDTO.recipient(), emailDTO.subject());

        Email email = EmailBuilder.startingBlank()
                .from(smtpProperties.getUsername())
                .to(emailDTO.recipient())
                .withSubject(emailDTO.subject())
                .withPlainText(emailDTO.body())
                .buildEmail();

        try {
            // false = síncrono: espera confirmación del servidor SMTP
            mailer.sendMail(email, true);
            log.info("✅ Email enviado exitosamente a {}", emailDTO.recipient());
        } catch (Exception e) {
            log.error("❌ Error enviando email a {}: {}", emailDTO.recipient(), e.getMessage());
            throw e;
        }
    }
}


