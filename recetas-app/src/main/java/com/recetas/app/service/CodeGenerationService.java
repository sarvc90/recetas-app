package com.recetas.app.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class CodeGenerationService {

    /**
     * Genera un código numérico aleatorio de 6 dígitos.
     */
    public String generarCodigo() {
        SecureRandom random = new SecureRandom();
        int numero = random.nextInt(900000) + 100000; // Rango 100000 - 999999
        return String.valueOf(numero);
    }
}
