package com.recetas.app.dto;

import com.recetas.app.entity.EstadoSuscripcion;

import java.time.LocalDateTime;

public class SuscripcionResponse {

    private Long id;
    private Long planId;
    private String nombrePlan;
    private Long suscriptorId;
    private EstadoSuscripcion estado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private boolean vigente;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public String getNombrePlan() { return nombrePlan; }
    public void setNombrePlan(String nombrePlan) { this.nombrePlan = nombrePlan; }

    public Long getSuscriptorId() { return suscriptorId; }
    public void setSuscriptorId(Long suscriptorId) { this.suscriptorId = suscriptorId; }

    public EstadoSuscripcion getEstado() { return estado; }
    public void setEstado(EstadoSuscripcion estado) { this.estado = estado; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public boolean isVigente() { return vigente; }
    public void setVigente(boolean vigente) { this.vigente = vigente; }
}
