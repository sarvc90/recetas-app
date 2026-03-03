package com.recetas.app.repository;

import com.recetas.app.entity.CodigoRecuperacion;
import com.recetas.app.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, Long> {

    // Buscar el codigo mas reciente no usado de un usuario
    Optional<CodigoRecuperacion> findTopByUsuarioAndUsadoFalseOrderByFechaCreacionDesc(Usuario usuario);

    // Buscar por codigo y que no este usado
    Optional<CodigoRecuperacion> findByCodigoAndUsadoFalse(String codigo);

    // Invalidar codigos anteriores: se buscan todos los no usados del usuario
    java.util.List<CodigoRecuperacion> findByUsuarioAndUsadoFalse(Usuario usuario);
}

