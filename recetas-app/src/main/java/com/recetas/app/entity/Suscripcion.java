package com.recetas.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "suscripciones")
public class Suscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "suscriptor_id", nullable = false)
    private Long suscriptorId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSuscripcion estado;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    public Suscripcion() {
        this.fechaCreacion = LocalDateTime.now();
        this.estado = EstadoSuscripcion.ACTIVA;
    }

    public Suscripcion(Long suscriptorId, Long planId, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        this();
        this.suscriptorId = suscriptorId;
        this.planId = planId;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public boolean isVigente() {
        return EstadoSuscripcion.ACTIVA.equals(this.estado)
                && (this.fechaFin == null || LocalDateTime.now().isBefore(this.fechaFin));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSuscriptorId() { return suscriptorId; }
    public void setSuscriptorId(Long suscriptorId) { this.suscriptorId = suscriptorId; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public EstadoSuscripcion getEstado() { return estado; }
    public void setEstado(EstadoSuscripcion estado) { this.estado = estado; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
