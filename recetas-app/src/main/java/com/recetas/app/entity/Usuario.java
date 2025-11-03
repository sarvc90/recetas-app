package com.recetas.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 100)
    private String nombre;

    @Email(message = "Email debe ser válido")
    @NotBlank(message = "El email es obligatorio")
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Column(nullable = false, length = 255)
    private String password;

    // La foto del usuario es opcional
    @Column(name = "foto_perfil", length = 500)
    private String fotoPerfil;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    // Constructor vacío (requerido por JPA)
    public Usuario() {
        this.fechaCreacion = LocalDateTime.now();
    }

    // Constructor con parámetros
    public Usuario(String nombre, String email, String password) {
        this();
        this.nombre = nombre;
        this.email = email;
        this.password = password;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    public void setUltimoAcceso(LocalDateTime ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }
}