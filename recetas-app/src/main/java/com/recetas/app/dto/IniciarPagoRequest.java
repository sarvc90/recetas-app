package com.recetas.app.dto;

import jakarta.validation.constraints.NotNull;

public class IniciarPagoRequest {

    @NotNull(message = "El ID del plan es obligatorio")
    private Long planId;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
}
