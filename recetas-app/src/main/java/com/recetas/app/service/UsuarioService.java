package com.recetas.app.service;

import com.recetas.app.dto.ApiResponse;
import com.recetas.app.dto.EditarUsuarioRequest;
import com.recetas.app.dto.UsuarioResponse;
import com.recetas.app.entity.Usuario;
import com.recetas.app.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ImageService imageService;

    public ApiResponse<UsuarioResponse> obtenerPerfil(Long usuarioId) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);

            if (usuarioOpt.isEmpty()) {
                return new ApiResponse<>(false, "Usuario no encontrado");
            }

            Usuario usuario = usuarioOpt.get();
            UsuarioResponse response = new UsuarioResponse(
                    usuario.getId(),
                    usuario.getNombre(),
                    usuario.getEmail(),
                    usuario.getFotoPerfil()
            );

            return new ApiResponse<>(true, "Perfil obtenido exitosamente", response);

        } catch (Exception e) {
            System.out.println("Error al obtener perfil: " + e.getMessage());
            return new ApiResponse<>(false, "Error interno del servidor");
        }
    }

    public ApiResponse<UsuarioResponse> actualizarPerfil(Long usuarioId, EditarUsuarioRequest request) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);

            if (usuarioOpt.isEmpty()) {
                return new ApiResponse<>(false, "Usuario no encontrado");
            }

            Usuario usuario = usuarioOpt.get();

            // Actualizar nombre
            usuario.setNombre(request.getNombre());

            // Actualizar foto si se proporcionó
            if (request.getFotoPerfil() != null && !request.getFotoPerfil().isEmpty()) {
                usuario.setFotoPerfil(request.getFotoPerfil());
            }

            // Guardar cambios
            Usuario usuarioActualizado = usuarioRepository.save(usuario);

            // Crear respuesta
            UsuarioResponse response = new UsuarioResponse(
                    usuarioActualizado.getId(),
                    usuarioActualizado.getNombre(),
                    usuarioActualizado.getEmail(),
                    usuarioActualizado.getFotoPerfil()
            );

            return new ApiResponse<>(true, "Perfil actualizado exitosamente", response);

        } catch (Exception e) {
            System.out.println("Error al actualizar perfil: " + e.getMessage());
            return new ApiResponse<>(false, "Error interno del servidor");
        }
    }

    public ApiResponse<String> subirFotoPerfil(Long usuarioId, MultipartFile file) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);

            if (usuarioOpt.isEmpty()) {
                return new ApiResponse<>(false, "Usuario no encontrado");
            }

            // Validar que sea una imagen
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return new ApiResponse<>(false, "El archivo debe ser una imagen");
            }

            // Subir imagen a Cloudinary
            String imageUrl = imageService.uploadImage(file);

            // Actualizar usuario con la nueva foto
            Usuario usuario = usuarioOpt.get();
            usuario.setFotoPerfil(imageUrl);
            usuarioRepository.save(usuario);

            return new ApiResponse<>(true, "Foto subida exitosamente", imageUrl);

        } catch (IOException e) {
            System.out.println("Error al subir foto: " + e.getMessage());
            return new ApiResponse<>(false, "Error al subir la imagen");
        } catch (Exception e) {
            System.out.println("Error interno: " + e.getMessage());
            return new ApiResponse<>(false, "Error interno del servidor");
        }
    }
}
