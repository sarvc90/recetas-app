package com.recetas.app.service;

import com.recetas.app.config.JwtUtil;
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

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ApiResponse<UsuarioResponse> login(LoginRequest request) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getEmail());

            if (usuarioOpt.isEmpty()) {
                return new ApiResponse<>(false, "Credenciales incorrectas");
            }

            Usuario usuario = usuarioOpt.get();

            if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
                return new ApiResponse<>(false, "Credenciales incorrectas");
            }

            // Actualizar último acceso
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);

            // Generar token JWT
            String token = jwtUtil.generateToken(usuario.getId(), usuario.getEmail());

            // Retornar respuesta con token
            UsuarioResponse usuarioResponse = new UsuarioResponse(
                    usuario.getId(),
                    usuario.getNombre(),
                    usuario.getEmail(),
                    usuario.getFotoPerfil(),
                    token
            );

            return new ApiResponse<>(true, "Login exitoso", usuarioResponse);

        } catch (Exception e) {
            System.out.println("Error en login: " + e.getMessage());
            e.printStackTrace();
            return new ApiResponse<>(false, "Error interno del servidor");
        }
    }

    public ApiResponse<UsuarioResponse> registro(RegistroRequest request) {
        try {
            if (usuarioRepository.existsByEmail(request.getEmail())) {
                return new ApiResponse<>(false, "El email ya está registrado");
            }

            Usuario nuevoUsuario = new Usuario(
                    request.getNombre(),
                    request.getEmail(),
                    passwordEncoder.encode(request.getPassword())
            );

            nuevoUsuario.setFotoPerfil(null);
            Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

            // Generar token JWT también en registro
            String token = jwtUtil.generateToken(usuarioGuardado.getId(), usuarioGuardado.getEmail());

            UsuarioResponse usuarioResponse = new UsuarioResponse(
                    usuarioGuardado.getId(),
                    usuarioGuardado.getNombre(),
                    usuarioGuardado.getEmail(),
                    usuarioGuardado.getFotoPerfil(),
                    token
            );

            return new ApiResponse<>(true, "Usuario registrado exitosamente", usuarioResponse);

        } catch (Exception e) {
            System.out.println("Error en registro: " + e.getMessage());
            e.printStackTrace();
            return new ApiResponse<>(false, "Error interno del servidor");
        }
    }
}