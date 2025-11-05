package com.recetas.app.dto;

public class UsuarioResponse {
    private Long id;
    private String nombre;
    private String email;
    private String fotoPerfil;

    // Constructor vacío
    public UsuarioResponse() {
    }

    // Constructor completo
    public UsuarioResponse(Long id, String nombre, String email, String fotoPerfil) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.fotoPerfil = fotoPerfil;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }
}