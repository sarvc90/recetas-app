package com.recetas.app.repository;

import com.recetas.app.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar usuarios por coincidencia en nombre
    List<Usuario> findByNombreContainingIgnoreCase(String nombre);

    // Buscar usuario por email
    Optional<Usuario> findByEmail(String email);

    // Verificar si existe un email
    boolean existsByEmail(String email);
}