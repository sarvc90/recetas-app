package com.recetas.app.controller;

import com.recetas.app.dto.ApiResponse;
import com.recetas.app.dto.AsignarRecetasRequest;
import com.recetas.app.dto.DashboardCreadorResponse;
import com.recetas.app.dto.PlanRequest;
import com.recetas.app.dto.PlanResponse;
import com.recetas.app.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/planes")
@CrossOrigin(origins = "*")
public class PlanController {

    @Autowired
    private PlanService planService;

    // Crear plan (creador autenticado)
    @PostMapping
    public ResponseEntity<ApiResponse<PlanResponse>> crearPlan(@Valid @RequestBody PlanRequest request) {
        ApiResponse<PlanResponse> response = planService.crearPlan(request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    // Editar plan
    @PutMapping("/{planId}")
    public ResponseEntity<ApiResponse<PlanResponse>> editarPlan(
            @PathVariable Long planId,
            @Valid @RequestBody PlanRequest request) {
        ApiResponse<PlanResponse> response = planService.editarPlan(planId, request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    // Desactivar plan
    @DeleteMapping("/{planId}")
    public ResponseEntity<ApiResponse<Void>> inactivarPlan(@PathVariable Long planId) {
        ApiResponse<Void> response = planService.inactivarPlan(planId);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    // Asignar recetas al plan
    @PostMapping("/{planId}/recetas")
    public ResponseEntity<ApiResponse<PlanResponse>> asignarRecetas(
            @PathVariable Long planId,
            @Valid @RequestBody AsignarRecetasRequest request) {
        ApiResponse<PlanResponse> response = planService.asignarRecetas(planId, request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    // Remover receta del plan
    @DeleteMapping("/{planId}/recetas/{recetaId}")
    public ResponseEntity<ApiResponse<Void>> removerReceta(
            @PathVariable Long planId,
            @PathVariable Long recetaId) {
        ApiResponse<Void> response = planService.removerReceta(planId, recetaId);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    // Obtener planes de un creador (público)
    @GetMapping("/creador/{creadorId}")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> getPlanesDeCreador(@PathVariable Long creadorId) {
        ApiResponse<List<PlanResponse>> response = planService.getPlanesDeCreador(creadorId);
        return ResponseEntity.ok(response);
    }

    // Listar todos los planes activos (público)
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlanResponse>>> getPlanesActivos() {
        return ResponseEntity.ok(planService.getPlanesActivos());
    }

    // Detalle de un plan (público)
    @GetMapping("/{planId}")
    public ResponseEntity<ApiResponse<PlanResponse>> getPlan(@PathVariable Long planId) {
        ApiResponse<PlanResponse> response = planService.getPlan(planId);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    // Dashboard del creador autenticado
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardCreadorResponse>> getDashboard() {
        ApiResponse<DashboardCreadorResponse> response = planService.getDashboard();
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
}
