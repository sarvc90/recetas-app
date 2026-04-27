package com.recetas.app.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class AsignarRecetasRequest {

    @NotEmpty(message = "Debe proporcionar al menos una receta")
    private List<Long> recetaIds;

    public List<Long> getRecetaIds() { return recetaIds; }
    public void setRecetaIds(List<Long> recetaIds) { this.recetaIds = recetaIds; }
}
