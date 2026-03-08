package com.recetas.app.dto;

public class UsuarioResponse {
    private Long id;
    private String nombre;
    private String email;
    private String fotoPerfil;
    private String token;
    private boolean emailVerificado;

    // Constructor vacío
    public UsuarioResponse() {
    }

    // Constructor sin token (para compatibilidad)
    public UsuarioResponse(Long id, String nombre, String email, String fotoPerfil) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.fotoPerfil = fotoPerfil;
    }

    // Constructor con token
    public UsuarioResponse(Long id, String nombre, String email, String fotoPerfil, String token) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.fotoPerfil = fotoPerfil;
        this.token = token;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public boolean isEmailVerificado() { return emailVerificado; }
    public void setEmailVerificado(boolean emailVerificado) { this.emailVerificado = emailVerificado; }
}