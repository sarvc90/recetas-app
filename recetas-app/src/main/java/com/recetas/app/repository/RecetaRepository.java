package com.recetas.app.repository;

import com.recetas.app.entity.Receta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RecetaRepository extends JpaRepository<Receta, Long> {

    // Buscar recetas por usuario
    Page<Receta> findByUsuarioId(Long usuarioId, Pageable pageable);

    // Buscar recetas por nombre (para futuras funcionalidades)
    Page<Receta> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    // Buscar todas las recetas ordenadas por fecha de creación
    Page<Receta> findAllByOrderByFechaCreacionDesc(Pageable pageable);
}