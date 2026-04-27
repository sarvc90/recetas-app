package com.recetas.app.repository;

import com.recetas.app.entity.EstadoPlan;
import com.recetas.app.entity.PlanSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanSuscripcionRepository extends JpaRepository<PlanSuscripcion, Long> {

    List<PlanSuscripcion> findByCreadorId(Long creadorId);

    List<PlanSuscripcion> findByCreadorIdAndEstado(Long creadorId, EstadoPlan estado);

    List<PlanSuscripcion> findByEstado(EstadoPlan estado);
}
