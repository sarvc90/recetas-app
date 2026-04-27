package com.recetas.app.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "suscripcion_id")
    private Long suscripcionId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "pagador_id", nullable = false)
    private Long pagadorId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "stripe_payment_intent_id", unique = true, length = 255)
    private String stripePaymentIntentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    public Pago() {
        this.estado = EstadoPago.PENDIENTE;
        this.fechaPago = LocalDateTime.now();
    }

    public Pago(Long pagadorId, Long planId, BigDecimal monto, String stripePaymentIntentId) {
        this();
        this.pagadorId = pagadorId;
        this.planId = planId;
        this.monto = monto;
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSuscripcionId() { return suscripcionId; }
    public void setSuscripcionId(Long suscripcionId) { this.suscripcionId = suscripcionId; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public Long getPagadorId() { return pagadorId; }
    public void setPagadorId(Long pagadorId) { this.pagadorId = pagadorId; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String stripePaymentIntentId) { this.stripePaymentIntentId = stripePaymentIntentId; }

    public EstadoPago getEstado() { return estado; }
    public void setEstado(EstadoPago estado) { this.estado = estado; }

    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }
}
