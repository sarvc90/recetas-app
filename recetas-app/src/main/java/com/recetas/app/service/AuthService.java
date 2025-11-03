package com.recetas.app.service;

import com.recetas.app.dto.*;
import com.recetas.app.entity.Usuario;
import com.recetas.app.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ApiResponse<UsuarioResponse> login(LoginRequest request) {
        try {
            // Buscar usuario por email
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getEmail());

            if (usuarioOpt.isEmpty()) {
                return new ApiResponse<>(false, "Credenciales incorrectas");
            }

            Usuario usuario = usuarioOpt.get();

            // Verificar contraseña
            if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
                return new ApiResponse<>(false, "Credenciales incorrectas");
            }

            // Actualizar último acceso
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);

            // Crear respuesta
            UsuarioResponse usuarioResponse = new UsuarioResponse(
                    usuario.getId(),
                    usuario.getNombre(),
                    usuario.getEmail(),
                    usuario.getFotoPerfil()
            );

            return new ApiResponse<>(true, "Login exitoso", usuarioResponse);

        } catch (Exception e) {
            System.out.println("Error en login: " + e.getMessage());
            return new ApiResponse<>(false, "Error interno del servidor");
        }
    }

    public ApiResponse<UsuarioResponse> registro(RegistroRequest request) {
        try {
            // Verificar si el email ya existe
            if (usuarioRepository.existsByEmail(request.getEmail())) {
                return new ApiResponse<>(false, "El email ya está registrado");
            }

            // Crear nuevo usuario
            Usuario nuevoUsuario = new Usuario(
                    request.getNombre(),
                    request.getEmail(),
                    passwordEncoder.encode(request.getPassword())
            );

            // Guardar en base de datos
            Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

            // Crear respuesta
            UsuarioResponse usuarioResponse = new UsuarioResponse(
                    usuarioGuardado.getId(),
                    usuarioGuardado.getNombre(),
                    usuarioGuardado.getEmail(),
                    usuarioGuardado.getFotoPerfil()
            );

            return new ApiResponse<>(true, "Usuario registrado exitosamente", usuarioResponse);

        } catch (Exception e) {
            System.out.println("Error en registro: " + e.getMessage());
            return new ApiResponse<>(false, "Error interno del servidor");
        }
    }
}