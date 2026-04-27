package com.recetas.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recetas_plan", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"plan_id", "receta_id"})
})
public class RecetaPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "receta_id", nullable = false)
    private Long recetaId;

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;

    public RecetaPlan() {
        this.fechaAsignacion = LocalDateTime.now();
    }

    public RecetaPlan(Long planId, Long recetaId) {
        this();
        this.planId = planId;
        this.recetaId = recetaId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public Long getRecetaId() { return recetaId; }
    public void setRecetaId(Long recetaId) { this.recetaId = recetaId; }

    public LocalDateTime getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(LocalDateTime fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }
}
