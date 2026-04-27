package com.recetas.app.controller;

import com.recetas.app.dto.ApiResponse;
import com.recetas.app.dto.IniciarPagoRequest;
import com.recetas.app.dto.PagoResponse;
import com.recetas.app.dto.SuscripcionResponse;
import com.recetas.app.service.SuscripcionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suscripciones")
@CrossOrigin(origins = "*")
public class SuscripcionController {

    @Autowired
    private SuscripcionService suscripcionService;

    // Iniciar pago: crea PaymentIntent en Stripe y retorna clientSecret
    @PostMapping("/iniciar-pago")
    public ResponseEntity<ApiResponse<PagoResponse>> iniciarPago(@Valid @RequestBody IniciarPagoRequest request) {
        ApiResponse<PagoResponse> response = suscripcionService.iniciarPago(request.getPlanId());
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    // Confirmar pago: verifica con Stripe y activa la suscripción
    @PostMapping("/confirmar-pago")
    public ResponseEntity<ApiResponse<SuscripcionResponse>> confirmarPago(
            @RequestBody Map<String, String> body) {
        String paymentIntentId = body.get("paymentIntentId");
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "paymentIntentId es obligatorio"));
        }
        ApiResponse<SuscripcionResponse> response = suscripcionService.confirmarPago(paymentIntentId);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    // Mis suscripciones
    @GetMapping("/mis-suscripciones")
    public ResponseEntity<ApiResponse<List<SuscripcionResponse>>> getMisSuscripciones() {
        return ResponseEntity.ok(suscripcionService.getMisSuscripciones());
    }

    // Cancelar suscripción
    @DeleteMapping("/{suscripcionId}")
    public ResponseEntity<ApiResponse<Void>> cancelarSuscripcion(@PathVariable Long suscripcionId) {
        ApiResponse<Void> response = suscripcionService.cancelarSuscripcion(suscripcionId);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    // Solicitar reembolso
    @PostMapping("/{suscripcionId}/reembolsar")
    public ResponseEntity<ApiResponse<Void>> solicitarReembolso(@PathVariable Long suscripcionId) {
        ApiResponse<Void> response = suscripcionService.solicitarReembolso(suscripcionId);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    // Verificar acceso a una receta exclusiva
    @GetMapping("/acceso/receta/{recetaId}")
    public ResponseEntity<ApiResponse<Boolean>> verificarAcceso(@PathVariable Long recetaId) {
        return ResponseEntity.ok(suscripcionService.verificarAcceso(recetaId));
    }
}
