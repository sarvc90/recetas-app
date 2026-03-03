package com.recetas.app.config.smtp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SmtpProperties {

    private String host;
    private int port;

    private String username;
    private String password;

}