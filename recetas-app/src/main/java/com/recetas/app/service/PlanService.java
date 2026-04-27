package com.recetas.app.service;

import com.recetas.app.dto.ApiResponse;
import com.recetas.app.dto.AsignarRecetasRequest;
import com.recetas.app.dto.DashboardCreadorResponse;
import com.recetas.app.dto.PlanRequest;
import com.recetas.app.dto.PlanResponse;
import com.recetas.app.entity.EstadoPago;
import com.recetas.app.entity.EstadoPlan;
import com.recetas.app.entity.EstadoSuscripcion;
import com.recetas.app.entity.PlanSuscripcion;
import com.recetas.app.entity.RecetaPlan;
import com.recetas.app.entity.Usuario;
import com.recetas.app.repository.PagoRepository;
import com.recetas.app.repository.PlanSuscripcionRepository;
import com.recetas.app.repository.RecetaPlanRepository;
import com.recetas.app.repository.RecetaRepository;
import com.recetas.app.repository.SuscripcionRepository;
import com.recetas.app.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanService {

  @Autowired
  private PlanSuscripcionRepository planRepository;

  @Autowired
  private RecetaPlanRepository recetaPlanRepository;

  @Autowired
  private SuscripcionRepository suscripcionRepository;

  @Autowired
  private PagoRepository pagoRepository;

  @Autowired
  private UsuarioRepository usuarioRepository;

  @Autowired
  private RecetaRepository recetaRepository;

  // -------------------------------------------------------------------------
  // CRUD de planes
  // -------------------------------------------------------------------------

  public ApiResponse<PlanResponse> crearPlan(PlanRequest request) {
    try {
      Long creadorId = getCurrentUserId();

      PlanSuscripcion plan = new PlanSuscripcion(
        request.getNombre(),
        request.getDescripcion(),
        request.getPrecio(),
        request.getDuracionDias(),
        creadorId
      );

      PlanSuscripcion guardado = planRepository.save(plan);
      return new ApiResponse<>(
        true,
        "Plan creado exitosamente",
        toResponse(guardado)
      );
    } catch (Exception e) {
      return new ApiResponse<>(
        false,
        "Error al crear el plan: " + e.getMessage()
      );
    }
  }

  public ApiResponse<PlanResponse> editarPlan(
    Long planId,
    PlanRequest request
  ) {
    try {
      Long creadorId = getCurrentUserId();
      Optional<PlanSuscripcion> planOpt = planRepository.findById(planId);

      if (planOpt.isEmpty()) {
        return new ApiResponse<>(false, "Plan no encontrado");
      }

      PlanSuscripcion plan = planOpt.get();

      if (!plan.getCreadorId().equals(creadorId)) {
        return new ApiResponse<>(
          false,
          "No tienes permiso para editar este plan"
        );
      }

      plan.setNombre(request.getNombre());
      plan.setDescripcion(request.getDescripcion());
      plan.setPrecio(request.getPrecio());
      if (request.getDuracionDias() != null) {
        plan.setDuracionDias(request.getDuracionDias());
      }
      plan.setFechaActualizacion(LocalDateTime.now());

      return new ApiResponse<>(
        true,
        "Plan actualizado exitosamente",
        toResponse(planRepository.save(plan))
      );
    } catch (Exception e) {
      return new ApiResponse<>(
        false,
        "Error al editar el plan: " + e.getMessage()
      );
    }
  }

  public ApiResponse<Void> inactivarPlan(Long planId) {
    try {
      Long creadorId = getCurrentUserId();
      Optional<PlanSuscripcion> planOpt = planRepository.findById(planId);

      if (planOpt.isEmpty()) {
        return new ApiResponse<>(false, "Plan no encontrado");
      }

      PlanSuscripcion plan = planOpt.get();

      if (!plan.getCreadorId().equals(creadorId)) {
        return new ApiResponse<>(
          false,
          "No tienes permiso para eliminar este plan"
        );
      }

      plan.setEstado(EstadoPlan.INACTIVO);
      plan.setFechaActualizacion(LocalDateTime.now());
      planRepository.save(plan);

      return new ApiResponse<>(true, "Plan desactivado exitosamente");
    } catch (Exception e) {
      return new ApiResponse<>(
        false,
        "Error al desactivar el plan: " + e.getMessage()
      );
    }
  }

  // -------------------------------------------------------------------------
  // Gestión de recetas en el plan
  // -------------------------------------------------------------------------

  @Transactional
  public ApiResponse<PlanResponse> asignarRecetas(
    Long planId,
    AsignarRecetasRequest request
  ) {
    try {
      Long creadorId = getCurrentUserId();
      Optional<PlanSuscripcion> planOpt = planRepository.findById(planId);

      if (planOpt.isEmpty()) {
        return new ApiResponse<>(false, "Plan no encontrado");
      }

      PlanSuscripcion plan = planOpt.get();

      if (!plan.getCreadorId().equals(creadorId)) {
        return new ApiResponse<>(
          false,
          "No tienes permiso para modificar este plan"
        );
      }

      List<Long> asignadas = new ArrayList<>();
      for (Long recetaId : request.getRecetaIds()) {
        recetaRepository
          .findById(recetaId)
          .ifPresent(receta -> {
            if (
              !recetaPlanRepository.existsByPlanIdAndRecetaId(planId, recetaId)
            ) {
              recetaPlanRepository.save(new RecetaPlan(planId, recetaId));
              receta.setEsExclusiva(true);
              recetaRepository.save(receta);
              asignadas.add(recetaId);
            }
          });
      }

      return new ApiResponse<>(
        true,
        asignadas.size() + " receta(s) asignada(s) al plan",
        toResponse(plan)
      );
    } catch (Exception e) {
      return new ApiResponse<>(
        false,
        "Error al asignar recetas: " + e.getMessage()
      );
    }
  }

  @Transactional
  public ApiResponse<Void> removerReceta(Long planId, Long recetaId) {
    try {
      Long creadorId = getCurrentUserId();
      Optional<PlanSuscripcion> planOpt = planRepository.findById(planId);

      if (planOpt.isEmpty()) {
        return new ApiResponse<>(false, "Plan no encontrado");
      }

      if (!planOpt.get().getCreadorId().equals(creadorId)) {
        return new ApiResponse<>(
          false,
          "No tienes permiso para modificar este plan"
        );
      }

      recetaPlanRepository.deleteByPlanIdAndRecetaId(planId, recetaId);

      // Si la receta ya no está en ningún plan, desmarcarla como exclusiva
      recetaRepository
        .findById(recetaId)
        .ifPresent(receta -> {
          if (recetaPlanRepository.findByRecetaId(recetaId).isEmpty()) {
            receta.setEsExclusiva(false);
            recetaRepository.save(receta);
          }
        });

      return new ApiResponse<>(true, "Receta removida del plan");
    } catch (Exception e) {
      return new ApiResponse<>(
        false,
        "Error al remover la receta: " + e.getMessage()
      );
    }
  }

  // -------------------------------------------------------------------------
  // Consultas
  // -------------------------------------------------------------------------

  public ApiResponse<List<PlanResponse>> getPlanesDeCreador(Long creadorId) {
    try {
      List<PlanSuscripcion> planes = planRepository.findByCreadorId(creadorId);
      List<PlanResponse> respuestas = planes
        .stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
      return new ApiResponse<>(true, "Planes obtenidos", respuestas);
    } catch (Exception e) {
      return new ApiResponse<>(
        false,
        "Error al obtener los planes: " + e.getMessage()
      );
    }
  }

  public ApiResponse<List<PlanResponse>> getPlanesActivos() {
    try {
      List<PlanSuscripcion> planes = planRepository.findByEstado(
        EstadoPlan.ACTIVO
      );
      List<PlanResponse> respuestas = planes
        .stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
      return new ApiResponse<>(true, "Planes activos obtenidos", respuestas);
    } catch (Exception e) {
      return new ApiResponse<>(
        false,
        "Error al obtener los planes: " + e.getMessage()
      );
    }
  }

  public ApiResponse<PlanResponse> getPlan(Long planId) {
    try {
      Optional<PlanSuscripcion> planOpt = planRepository.findById(planId);
      if (planOpt.isEmpty()) {
        return new ApiResponse<>(false, "Plan no encontrado");
      }
      return new ApiResponse<>(
        true,
        "Plan obtenido",
        toResponse(planOpt.get())
      );
    } catch (Exception e) {
      return new ApiResponse<>(
        false,
        "Error al obtener el plan: " + e.getMessage()
      );
    }
  }

  public ApiResponse<DashboardCreadorResponse> getDashboard() {
    try {
      Long creadorId = getCurrentUserId();
      Optional<Usuario> usuarioOpt = usuarioRepository.findById(creadorId);

      if (usuarioOpt.isEmpty()) {
        return new ApiResponse<>(false, "Usuario no encontrado");
      }

      List<PlanSuscripcion> planes = planRepository.findByCreadorId(creadorId);
      List<Long> planIds = planes
        .stream()
        .map(PlanSuscripcion::getId)
        .collect(Collectors.toList());

      BigDecimal ventasBrutas = planIds.isEmpty()
        ? BigDecimal.ZERO
        : pagoRepository.sumIngresosCompletadosByPlanIds(
            planIds,
            EstadoPago.COMPLETADO
          );
      if (ventasBrutas == null) ventasBrutas = BigDecimal.ZERO;

      BigDecimal ingresosCreador = ventasBrutas
        .multiply(new BigDecimal("0.75"))
        .setScale(2, RoundingMode.HALF_UP);
      BigDecimal comisionPlataforma = ventasBrutas
        .multiply(new BigDecimal("0.25"))
        .setScale(2, RoundingMode.HALF_UP);

      long suscriptoresTotales = planes
        .stream()
        .mapToLong(p ->
          suscripcionRepository.countByPlanIdAndEstado(
            p.getId(),
            EstadoSuscripcion.ACTIVA
          )
        )
        .sum();

      List<DashboardCreadorResponse.PlanMetrica> metricas = planes
        .stream()
        .map(plan -> {
          DashboardCreadorResponse.PlanMetrica m =
            new DashboardCreadorResponse.PlanMetrica();
          m.setPlanId(plan.getId());
          m.setNombrePlan(plan.getNombre());
          BigDecimal ventasPlan = pagoRepository.sumIngresosCompletadosByPlanId(
            plan.getId(),
            EstadoPago.COMPLETADO
          );
          if (ventasPlan == null) ventasPlan = BigDecimal.ZERO;
          m.setIngresos(
            ventasPlan
              .multiply(new BigDecimal("0.75"))
              .setScale(2, RoundingMode.HALF_UP)
          );
          m.setSuscriptoresActivos(
            suscripcionRepository.countByPlanIdAndEstado(
              plan.getId(),
              EstadoSuscripcion.ACTIVA
            )
          );
          m.setTotalRecetas(
            recetaPlanRepository.findByPlanId(plan.getId()).size()
          );
          return m;
        })
        .collect(Collectors.toList());

      DashboardCreadorResponse dashboard = new DashboardCreadorResponse();
      dashboard.setCreadorId(creadorId);
      dashboard.setNombreCreador(usuarioOpt.get().getNombre());
      dashboard.setIngresosTotales(ventasBrutas);
      dashboard.setIngresosCreador(ingresosCreador);
      dashboard.setComisionPlataforma(comisionPlataforma);
      dashboard.setSuscriptoresTotalesActivos(suscriptoresTotales);
      dashboard.setTotalPlanes(planes.size());
      dashboard.setMetricasPorPlan(metricas);

      return new ApiResponse<>(true, "Dashboard obtenido", dashboard);
    } catch (Exception e) {
      return new ApiResponse<>(
        false,
        "Error al obtener el dashboard: " + e.getMessage()
      );
    }
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private PlanResponse toResponse(PlanSuscripcion plan) {
    PlanResponse r = new PlanResponse();
    r.setId(plan.getId());
    r.setNombre(plan.getNombre());
    r.setDescripcion(plan.getDescripcion());
    r.setPrecio(plan.getPrecio());
    r.setDuracionDias(plan.getDuracionDias());
    r.setCreadorId(plan.getCreadorId());
    r.setEstado(plan.getEstado());
    r.setFechaCreacion(plan.getFechaCreacion());

    usuarioRepository
      .findById(plan.getCreadorId())
      .ifPresent(u -> r.setNombreCreador(u.getNombre()));

    List<Long> recetaIds = recetaPlanRepository
      .findByPlanId(plan.getId())
      .stream()
      .map(RecetaPlan::getRecetaId)
      .collect(Collectors.toList());
    r.setRecetaIds(recetaIds);

    long suscriptores = suscripcionRepository.countByPlanIdAndEstado(
      plan.getId(),
      EstadoSuscripcion.ACTIVA
    );
    r.setSuscriptoresActivos(suscriptores);

    return r;
  }

  private Long getCurrentUserId() {
    Authentication auth =
      SecurityContextHolder.getContext().getAuthentication();
    return (Long) auth.getCredentials();
  }
}
