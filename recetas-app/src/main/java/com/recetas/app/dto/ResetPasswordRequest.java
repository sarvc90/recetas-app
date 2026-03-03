package com.recetas.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @Email(message = "Email debe ser valido")
    @NotBlank(message = "Email es obligatorio")
    private String email;

    @NotBlank(message = "El codigo es obligatorio")
    @Size(min = 6, max = 6, message = "El codigo debe tener 6 digitos")
    private String codigo;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String nuevaPassword;

    public ResetPasswordRequest() {}

    public ResetPasswordRequest(String email, String codigo, String nuevaPassword) {
        this.email = email;
        this.codigo = codigo;
        this.nuevaPassword = nuevaPassword;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNuevaPassword() { return nuevaPassword; }
    public void setNuevaPassword(String nuevaPassword) { this.nuevaPassword = nuevaPassword; }
}

