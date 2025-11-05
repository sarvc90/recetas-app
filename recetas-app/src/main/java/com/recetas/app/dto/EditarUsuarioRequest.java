package com.recetas.app.dto;

import jakarta.validation.constraints.NotBlank;

public class EditarUsuarioRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String fotoPerfil;

    // Constructores
    public EditarUsuarioRequest() {
    }

    public EditarUsuarioRequest(String nombre, String fotoPerfil) {
        this.nombre = nombre;
        this.fotoPerfil = fotoPerfil;
    }

    // Getters y Setters
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }
}