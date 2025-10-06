package com.recetas.app.repository;

import com.recetas.app.entity.Receta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecetaRepository extends JpaRepository<Receta, Long> {

    // Buscar recetas por usuario
    Page<Receta> findByUsuarioId(Long usuarioId, Pageable pageable);

    // Buscar recetas de un usuario por coincidencia en el título
    List<Receta> findByUsuarioIdAndNombreContainingIgnoreCase(Long usuarioId, String nombre);

    // Buscar recetas globalmente por coincidencia en el título
    List<Receta> findByNombreContainingIgnoreCase(String nombre);

    // Buscar recetas por usuarioId
    List<Receta> findByUsuarioId(Long usuarioId);

    // Buscar todas las recetas ordenadas por fecha de creación
    Page<Receta> findAllByOrderByFechaCreacionDesc(Pageable pageable);
}