package com.recetas.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
public class LoginRequest {
    @Email(message = "Email debe ser válido")
    @NotBlank(message = "Email es obligatorio")
    @Pattern(regexp = "^\\S+$", message = "El email no debe contener espacios en blanco")
    private String email;

    @NotBlank(message = "Password es obligatorio")
    @Pattern(regexp = "^\\S+$", message = "La contraseña no debe contener espacios en blanco")
    private String password;

    @Size(min = 6, max = 6, message = "Auth code debe tener exactamente 6 caracteres")
    private String authCode; // Campo para el código de autenticación

    // Constructores
    public LoginRequest() {}

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
        this.authCode = null; // Inicialmente sin código de autenticación
    }

    public LoginRequest(String email, String password, String authCode) {
        this.email = email;
        this.password = password;
        this.authCode = authCode;
    }

    // Getters y Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAuthCode() { return authCode; }
    public void setAuthCode(String authCode) { this.authCode = authCode; }
}