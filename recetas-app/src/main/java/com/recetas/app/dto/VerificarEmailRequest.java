package com.recetas.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VerificarEmailRequest {

    @NotBlank(message = "El código es obligatorio")
    @Size(min = 6, max = 6, message = "El código de verificación debe tener exactamente 6 caracteres")
    private String codigo;

    public VerificarEmailRequest() {}

    public VerificarEmailRequest(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
}
