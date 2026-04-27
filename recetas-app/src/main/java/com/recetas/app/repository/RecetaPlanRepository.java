package com.recetas.app.repository;

import com.recetas.app.entity.RecetaPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecetaPlanRepository extends JpaRepository<RecetaPlan, Long> {

    List<RecetaPlan> findByPlanId(Long planId);

    List<RecetaPlan> findByRecetaId(Long recetaId);

    Optional<RecetaPlan> findByPlanIdAndRecetaId(Long planId, Long recetaId);

    boolean existsByPlanIdAndRecetaId(Long planId, Long recetaId);

    void deleteByPlanIdAndRecetaId(Long planId, Long recetaId);
}
