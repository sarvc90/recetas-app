package com.recetas.app.dto;

import com.recetas.app.entity.EstadoPlan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PlanResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer duracionDias;
    private Long creadorId;
    private String nombreCreador;
    private EstadoPlan estado;
    private List<Long> recetaIds;
    private long suscriptoresActivos;
    private LocalDateTime fechaCreacion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public Integer getDuracionDias() { return duracionDias; }
    public void setDuracionDias(Integer duracionDias) { this.duracionDias = duracionDias; }

    public Long getCreadorId() { return creadorId; }
    public void setCreadorId(Long creadorId) { this.creadorId = creadorId; }

    public String getNombreCreador() { return nombreCreador; }
    public void setNombreCreador(String nombreCreador) { this.nombreCreador = nombreCreador; }

    public EstadoPlan getEstado() { return estado; }
    public void setEstado(EstadoPlan estado) { this.estado = estado; }

    public List<Long> getRecetaIds() { return recetaIds; }
    public void setRecetaIds(List<Long> recetaIds) { this.recetaIds = recetaIds; }

    public long getSuscriptoresActivos() { return suscriptoresActivos; }
    public void setSuscriptoresActivos(long suscriptoresActivos) { this.suscriptoresActivos = suscriptoresActivos; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
