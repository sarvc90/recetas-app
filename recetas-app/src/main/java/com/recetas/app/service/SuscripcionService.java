package com.recetas.app.service;

import com.recetas.app.dto.ApiResponse;
import com.recetas.app.dto.PagoResponse;
import com.recetas.app.dto.SuscripcionResponse;
import com.recetas.app.entity.EstadoPago;
import com.recetas.app.entity.EstadoSuscripcion;
import com.recetas.app.entity.Pago;
import com.recetas.app.entity.PlanSuscripcion;
import com.recetas.app.entity.RecetaPlan;
import com.recetas.app.entity.Suscripcion;
import com.recetas.app.entity.Usuario;
import com.recetas.app.repository.PagoRepository;
import com.recetas.app.repository.PlanSuscripcionRepository;
import com.recetas.app.repository.RecetaPlanRepository;
import com.recetas.app.repository.RecetaRepository;
import com.recetas.app.repository.SuscripcionRepository;
import com.recetas.app.repository.UsuarioRepository;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuscripcionService {

  @Value("${stripe.secret.key:sk_test_placeholder}")
  private String stripeSecretKey;

  @Autowired
  private SuscripcionRepository suscripcionRepository;

  @Autowired
  private PagoRepository pagoRepository;

  @Autowired
  private PlanSuscripcionRepository planRepository;

  @Autowired
  private RecetaRepository recetaRepository;

  @Autowired
  private RecetaPlanRepository recetaPlanRepository;

  @Autowired
  private UsuarioRepository usuarioRepository;

  @Autowired
  private EmailService emailService;

  // -------------------------------------------------------------------------
  // Iniciar pago (crea PaymentIntent en Stripe)
  // -------------------------------------------------------------------------

  @Transactional
  public ApiResponse<PagoResponse> iniciarPago(Long planId) {
    try {
      Long usuarioId = getCurrentUserId();

      Optional<PlanSuscripcion> planOpt = planRepository.findById(planId);
      if (planOpt.isEmpty()) {
        return new ApiResponse<>(false, "Plan no encontrado");
      }

      PlanSuscripcion plan = planOpt.get();

      if (plan.getCreadorId().equals(usuarioId)) {
        return new ApiResponse<>(
          false,
          "No puedes suscribirte a tu propio plan"
        );
      }

      Optional<Suscripcion> suscripcionExistente =
        suscripcionRepository.findBySuscriptorIdAndPlanIdAndEstado(
          usuarioId,
          planId,
          EstadoSuscripcion.ACTIVA
        );
      if (
        suscripcionExistente.isPresent() &&
        suscripcionExistente.get().isVigente()
      ) {
        return new ApiResponse<>(
          false,
          "Ya tienes una suscripcion activa para este plan"
        );
      }

      Stripe.apiKey = stripeSecretKey;

      long montoEnCentavos = plan
        .getPrecio()
        .multiply(new java.math.BigDecimal("100"))
        .longValue();

      PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
        .setAmount(montoEnCentavos)
        .setCurrency("usd")
        .addPaymentMethodType("card")
        .putMetadata("planId", String.valueOf(planId))
        .putMetadata("usuarioId", String.valueOf(usuarioId))
        .build();

      PaymentIntent paymentIntent = PaymentIntent.create(params);

      Pago pago = new Pago(
        usuarioId,
        planId,
        plan.getPrecio(),
        paymentIntent.getId()
      );
      pago = pagoRepository.save(pago);

      PagoResponse response = new PagoResponse();
      response.setPagoId(pago.getId());
      response.setClientSecret(paymentIntent.getClientSecret());
      response.setStripePaymentIntentId(paymentIntent.getId());
      response.setMonto(plan.getPrecio());
      response.setEstado(EstadoPago.PENDIENTE);
      response.setFechaPago(pago.getFechaPago());

      return new ApiResponse<>(
        true,
        "PaymentIntent creado. Usa el clientSecret para completar el pago.",
        response
      );
    } catch (Exception e) {
      return new ApiResponse<>(
        false,
        "Error al iniciar el pago: " + e.getMessage()
      );
    }
  }

  // -------------------------------------------------------------------------
  // Confirmar pago (verifica con Stripe y activa la suscripción)
  // -------------------------------------------------------------------------

  @Transactional
  public ApiResponse<SuscripcionResponse> confirmarPago(
    String paymentIntentId
  ) {
    try {
      Long usuarioId = getCurrentUserId();

      Optional<Pago> pagoOpt = pagoRepository.findByStripePaymentIntentId(
        paymentIntentId
      );
      if (pagoOpt.isEmpty()) {
        return new ApiResponse<>(false, "Pago no encontrado");
      }

      Pago pago = pagoOpt.get();

      if (!pago.getPagadorId().equals(usuarioId)) {
        return new ApiResponse<>(
          false,
          "No tienes permiso para confirmar este pago"
        );
      }

      if (EstadoPago.COMPLETADO.equals(pago.getEstado())) {
        return new ApiResponse<>(false, "Este pago ya fue procesado");
      }

      Stripe.apiKey = stripeSecretKey;
      PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

      if (!"succeeded".equals(paymentIntent.getStatus())) {
        pago.setEstado(EstadoPago.FALLIDO);
        pagoRepository.save(pago);
        return new ApiResponse<>(
          false,
          "El pago no fue completado. Estado: " + paymentIntent.getStatus()
        );
      }

      pago.setEstado(EstadoPago.COMPLETADO);

      Optional<PlanSuscripcion> planOpt = planRepository.findById(
        pago.getPlanId()
      );
      if (planOpt.isEmpty()) {
        return new ApiResponse<>(false, "Plan no encontrado");
      }

      PlanSuscripcion plan = planOpt.get();
      LocalDateTime ahora = LocalDateTime.now();
      LocalDateTime fechaFin = ahora.plusDays(plan.getDuracionDias());

      Suscripcion suscripcion = new Suscripcion(
        usuarioId,
        plan.getId(),
        ahora,
        fechaFin
      );
      suscripcion = suscripcionRepository.save(suscripcion);

      pago.setSuscripcionId(suscripcion.getId());
      pagoRepository.save(pago);

      enviarConfirmacionSuscripcion(usuarioId, plan, suscripcion);

      return new ApiResponse<>(
        true,
        "Suscripcion activada exitosamente",
        toResponse(suscripcion, plan.getNombre())
      );
    } catch (Exception e) {
      return new ApiResponse<>(
        false,
        "Error al confirmar el pago: " + e.getMessage()
      );
    }
  }

  // -------------------------------------------------------------------------
  // Cancelar suscripción
  // -------------------------------------------------------------------------

  @Transactional
  public ApiResponse<Void> cancelarSuscripcion(Long suscripcionId) {
    try {
      Long usuarioId = getCurrentUserId();
      Optional<Suscripcion> suscripcionOpt = suscripcionRepository.findById(
        suscripcionId
      );

      if (suscripcionOpt.isEmpty()) {
        return new ApiResponse<>(false, "Suscripcion no encontrada");
      }

      Suscripcion suscripcion = suscripcionOpt.get();

      if (!suscripcion.getSuscriptorId().equals(usuarioId)) {
        return new ApiResponse<>(
          false,
          "No tienes permiso para cancelar esta suscripcion"
        );
      }

      if (!EstadoSuscripcion.ACTIVA.equals(suscripcion.getEstado())) {
        return new ApiResponse<>(false, "La suscripcion ya no esta activa");
      }

      suscripcion.setEstado(EstadoSuscripcion.CANCELADA);
      suscripcionRepository.save(suscripcion);

      return new ApiResponse<>(true, "Suscripcion cancelada exitosamente");
    } catch (Exception e) {
      return new ApiResponse<>(
        false,
        "Error al cancelar la suscripcion: " + e.getMessage()
      );
    }
  }

  // -------------------------------------------------------------------------
  // Reembolso
  // -------------------------------------------------------------------------

  @Transactional
  public ApiResponse<Void> solicitarReembolso(Long suscripcionId) {
    try {
      Long usuarioId = getCurrentUserId();
      Optional<Suscripcion> suscripcionOpt = suscripcionRepository.findById(
        suscripcionId
      );

      if (suscripcionOpt.isEmpty()) {
        return new ApiResponse<>(false, "Suscripcion no encontrada");
      }

      Suscripcion suscripcion = suscripcionOpt.get();

      if (!suscripcion.getSuscriptorId().equals(usuarioId)) {
        return new ApiResponse<>(
          false,
          "No tienes permiso para solicitar reembolso en esta suscripcion"
        );
      }

      List<Pago> pagos = pagoRepository
        .findByPlanIdAndEstado(suscripcion.getPlanId(), EstadoPago.COMPLETADO)
        .stream()
        .filter(
          p ->
            p.getPagadorId().equals(usuarioId) &&
            suscripcionId.equals(p.getSuscripcionId())
        )
        .collect(Collectors.toList());

      if (pagos.isEmpty()) {
        return new ApiResponse<>(
          false,
          "No se encontro un pago completado para esta suscripcion"
        );
      }

      Pago pago = pagos.get(0);

      Stripe.apiKey = stripeSecretKey;
      RefundCreateParams refundParams = RefundCreateParams.builder()
        .setPaymentIntent(pago.getStripePaymentIntentId())
        .build();
      Refund.create(refundParams);

      pago.setEstado(EstadoPago.REEMBOLSADO);
      pagoRepository.save(pago);

      suscripcion.setEstado(EstadoSuscripcion.CANCELADA);
      suscripcionRepository.save(suscripcion);

      return new ApiResponse<>(true, "Reembolso procesado exitosamente");
    } catch (Exception e) {
      return new ApiResponse<>(
        false,
        "Error al procesar el reembolso: " + e.getMessage()
      );
    }
  }

  // -------------------------------------------------------------------------
  // Consultas
  // -------------------------------------------------------------------------

  public ApiResponse<List<SuscripcionResponse>> getMisSuscripciones() {
    try {
      Long usuarioId = getCurrentUserId();
      List<Suscripcion> suscripciones =
        suscripcionRepository.findBySuscriptorId(usuarioId);

      List<SuscripcionResponse> respuestas = suscripciones
        .stream()
        .map(s -> {
          String nombrePlan = planRepository
            .findById(s.getPlanId())
            .map(PlanSuscripcion::getNombre)
            .orElse("Plan eliminado");
          return toResponse(s, nombrePlan);
        })
        .collect(Collectors.toList());

      return new ApiResponse<>(true, "Suscripciones obtenidas", respuestas);
    } catch (Exception e) {
      return new ApiResponse<>(
        false,
        "Error al obtener las suscripciones: " + e.getMessage()
      );
    }
  }

  public ApiResponse<Boolean> verificarAcceso(Long recetaId) {
    try {
      Long usuarioId = getCurrentUserId();

      List<RecetaPlan> planesConReceta = recetaPlanRepository.findByRecetaId(
        recetaId
      );

      if (planesConReceta.isEmpty()) {
        return new ApiResponse<>(true, "La receta es de acceso libre", true);
      }

      for (RecetaPlan rp : planesConReceta) {
        Optional<Suscripcion> suscripcion =
          suscripcionRepository.findBySuscriptorIdAndPlanIdAndEstado(
            usuarioId,
            rp.getPlanId(),
            EstadoSuscripcion.ACTIVA
          );
        if (suscripcion.isPresent() && suscripcion.get().isVigente()) {
          return new ApiResponse<>(true, "Acceso permitido", true);
        }
      }

      Long creadorDeLaReceta = recetaRepository
        .findById(recetaId)
        .map(r -> r.getUsuarioId())
        .orElse(null);
      if (usuarioId.equals(creadorDeLaReceta)) {
        return new ApiResponse<>(true, "Acceso permitido como creador", true);
      }

      return new ApiResponse<>(
        false,
        "Necesitas una suscripcion activa para acceder a esta receta",
        false
      );
    } catch (Exception e) {
      return new ApiResponse<>(
        false,
        "Error al verificar el acceso: " + e.getMessage()
      );
    }
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private void enviarConfirmacionSuscripcion(
    Long usuarioId,
    PlanSuscripcion plan,
    Suscripcion suscripcion
  ) {
    try {
      Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
      if (usuarioOpt.isEmpty()) return;

      Usuario usuario = usuarioOpt.get();
      com.recetas.app.dto.EmailDto emailDto = new com.recetas.app.dto.EmailDto(
        "Confirmacion de suscripcion - RecetasApp",
        "Hola " +
          usuario.getNombre() +
          ",\n\n" +
          "Tu suscripcion al plan \"" +
          plan.getNombre() +
          "\" ha sido activada exitosamente.\n\n" +
          "Detalles:\n" +
          "  Plan: " +
          plan.getNombre() +
          "\n" +
          "  Precio: $" +
          plan.getPrecio() +
          "\n" +
          "  Vigente hasta: " +
          suscripcion.getFechaFin() +
          "\n\n" +
          "Ya puedes acceder al contenido exclusivo del plan.\n\n" +
          "Gracias por suscribirte.",
        usuario.getEmail()
      );
      emailService.sendMail(emailDto);
    } catch (Exception ignored) {
      // El fallo del email no debe interrumpir el flujo de pago
    }
  }

  private SuscripcionResponse toResponse(Suscripcion s, String nombrePlan) {
    SuscripcionResponse r = new SuscripcionResponse();
    r.setId(s.getId());
    r.setPlanId(s.getPlanId());
    r.setNombrePlan(nombrePlan);
    r.setSuscriptorId(s.getSuscriptorId());
    r.setEstado(s.getEstado());
    r.setFechaInicio(s.getFechaInicio());
    r.setFechaFin(s.getFechaFin());
    r.setVigente(s.isVigente());
    return r;
  }

  private Long getCurrentUserId() {
    Authentication auth =
      SecurityContextHolder.getContext().getAuthentication();
    return (Long) auth.getCredentials();
  }
}
