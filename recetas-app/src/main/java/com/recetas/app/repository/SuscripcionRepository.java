package com.recetas.app.repository;

import com.recetas.app.entity.EstadoSuscripcion;
import com.recetas.app.entity.Suscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {

    List<Suscripcion> findBySuscriptorId(Long suscriptorId);

    List<Suscripcion> findByPlanId(Long planId);

    List<Suscripcion> findByPlanIdAndEstado(Long planId, EstadoSuscripcion estado);

    Optional<Suscripcion> findBySuscriptorIdAndPlanIdAndEstado(Long suscriptorId, Long planId, EstadoSuscripcion estado);

    long countByPlanIdAndEstado(Long planId, EstadoSuscripcion estado);
}
