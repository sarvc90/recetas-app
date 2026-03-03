package com.recetas.app.dto;

public record EmailDto(
        String subject,
        String body,
        String recipient
) {}
