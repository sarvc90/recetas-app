package com.recetas.app.repository;

import com.recetas.app.entity.EstadoPago;
import com.recetas.app.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByStripePaymentIntentId(String stripePaymentIntentId);

    List<Pago> findByPagadorId(Long pagadorId);

    List<Pago> findByPlanIdAndEstado(Long planId, EstadoPago estado);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.planId = :planId AND p.estado = :estado")
    BigDecimal sumIngresosCompletadosByPlanId(@Param("planId") Long planId, @Param("estado") EstadoPago estado);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.planId IN :planIds AND p.estado = :estado")
    BigDecimal sumIngresosCompletadosByPlanIds(@Param("planIds") List<Long> planIds, @Param("estado") EstadoPago estado);
}
