package com.recetas.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "recetas")
public class Receta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(length = 1000)
    private String descripcion;

    @NotBlank(message = "Los ingredientes son obligatorios")
    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String ingredientes;

    @NotBlank(message = "Las instrucciones son obligatorias")
    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String instrucciones;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "tiempo_preparacion")
    private Integer tiempoPreparacion; // en minutos

    private Integer porciones;

    @NotNull
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // Constructor vacío (requerido por JPA)
    public Receta() {
        this.fechaCreacion = LocalDateTime.now();
    }

    // Constructor con parámetros básicos
    public Receta(String nombre, String descripcion, String ingredientes,
                  String instrucciones, Long usuarioId) {
        this();
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.ingredientes = ingredientes;
        this.instrucciones = instrucciones;
        this.usuarioId = usuarioId;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getIngredientes() { return ingredientes; }
    public void setIngredientes(String ingredientes) { this.ingredientes = ingredientes; }

    public String getInstrucciones() { return instrucciones; }
    public void setInstrucciones(String instrucciones) { this.instrucciones = instrucciones; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public Integer getTiempoPreparacion() { return tiempoPreparacion; }
    public void setTiempoPreparacion(Integer tiempoPreparacion) { this.tiempoPreparacion = tiempoPreparacion; }

    public Integer getPorciones() { return porciones; }
    public void setPorciones(Integer porciones) { this.porciones = porciones; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}