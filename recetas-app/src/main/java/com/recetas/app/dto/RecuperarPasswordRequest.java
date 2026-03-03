package com.recetas.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class RecuperarPasswordRequest {

    @Email(message = "Email debe ser valido")
    @NotBlank(message = "Email es obligatorio")
    private String email;

    public RecuperarPasswordRequest() {}

    public RecuperarPasswordRequest(String email) {
        this.email = email;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

