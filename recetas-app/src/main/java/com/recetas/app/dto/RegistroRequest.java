package com.recetas.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegistroRequest {
    @NotBlank(message = "Nombre es obligatorio")
    @Size(min = 2, max = 100, message = "Nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @Email(message = "Email debe ser válido")
    @NotBlank(message = "Email es obligatorio")
    private String email;

    @NotBlank(message = "Password es obligatorio")
    @Size(min = 6, message = "Password debe tener mínimo 6 caracteres")
    private String password;

    @Size(min = 6, max = 6, message = "El código de verificación debe tener exactamente 6 caracteres")
    private String verificationCode;

    // Constructores
    public RegistroRequest() {}

    public RegistroRequest(String nombre, String email, String password) {
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.verificationCode = null;
    }

    public RegistroRequest(String nombre, String email, String password, String verificationCode) {
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.verificationCode = verificationCode;
    }

    // Getters y Setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
}