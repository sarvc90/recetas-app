package com.recetas.app.repository;

import com.recetas.app.entity.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Long> {


    List<Comentario> findByRecetaIdOrderByFechaCreacionDesc(Long recetaId);


    boolean existsByRecetaIdAndUsuarioId(Long recetaId, Long usuarioId);
}
