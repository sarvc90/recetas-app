package com.recetas.app.repository;

import com.recetas.app.entity.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {
    List<Favorito> findByUsuarioId(Long usuarioId);
    Optional<Favorito> findByUsuarioIdAndRecetaId(Long usuarioId, Long recetaId);
    void deleteByUsuarioIdAndRecetaId(Long usuarioId, Long recetaId);
}
