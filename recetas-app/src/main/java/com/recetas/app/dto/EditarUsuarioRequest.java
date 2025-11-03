package com.recetas.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EditarUsuarioRequest {

    @NotBlank(message = "Nombre es obligatorio")
    @Size(min = 2, max = 100, message = "Nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    private String fotoPerfil;

    public EditarUsuarioRequest() {}

    public EditarUsuarioRequest(String nombre, String fotoPerfil) {
        this.nombre = nombre;
        this.fotoPerfil = fotoPerfil;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }
}
