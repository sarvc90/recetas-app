package com.recetas.app.repository;

import com.recetas.app.entity.Codigo;
import com.recetas.app.entity.TipoCodigo;
import com.recetas.app.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodigoRecuperacionRepository extends JpaRepository<Codigo, Long> {

    // Buscar el código más reciente no usado de un usuario
    Optional<Codigo> findTopByUsuarioAndUsadoFalseOrderByFechaCreacionDesc(Usuario usuario);

    // Buscar por código y que no este usado
    Optional<Codigo> findByCodigoAndUsadoFalse(String codigo);

    // Invalidar códigos anteriores: se buscan todos los no usados del usuario
    java.util.List<Codigo> findByUsuarioAndUsadoFalse(Usuario usuario);

    // Buscar el código más reciente no usado de un usuario y de un tipo específico
    Optional<Codigo> findTopByUsuarioAndUsadoFalseAndTipoOrderByFechaCreacionDesc(Usuario usuario, TipoCodigo tipo);
}
