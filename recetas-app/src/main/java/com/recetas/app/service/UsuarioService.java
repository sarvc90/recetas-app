package com.recetas.app.service;

import com.recetas.app.dto.ApiResponse;
import com.recetas.app.dto.EditarUsuarioRequest;
import com.recetas.app.dto.EmailDto;
import com.recetas.app.dto.UsuarioResponse;
import com.recetas.app.entity.Codigo;
import com.recetas.app.entity.TipoCodigo;
import com.recetas.app.entity.Usuario;
import com.recetas.app.repository.CodigoRecuperacionRepository;
import com.recetas.app.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private static final int MINUTOS_EXPIRACION = 5;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ImageService imageService;

    @Autowired
    private CodigoRecuperacionRepository codigoRecuperacionRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CodeGenerationService codeGenerationService;

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
            response.setEmailVerificado(usuario.isEmailVerificado());

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
            response.setEmailVerificado(usuarioActualizado.isEmailVerificado());

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

    public ApiResponse<Void> solicitarVerificacionEmail(Long usuarioId) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);

            if (usuarioOpt.isEmpty()) {
                return new ApiResponse<>(false, "Usuario no encontrado");
            }

            Usuario usuario = usuarioOpt.get();

            if (usuario.isEmailVerificado()) {
                return new ApiResponse<>(false, "El email ya está verificado");
            }

            // Invalidar códigos anteriores del mismo tipo
            List<Codigo> codigosAnteriores = codigoRecuperacionRepository.findByUsuarioAndUsadoFalse(usuario);
            for (Codigo c : codigosAnteriores) {
                if (TipoCodigo.VERIFICACION_EMAIL.equals(c.getTipo())) {
                    c.setUsado(true);
                }
            }
            codigoRecuperacionRepository.saveAll(codigosAnteriores);

            String codigo = codeGenerationService.generarCodigo();
            Codigo codigoVerificacion = new Codigo(codigo, usuario, MINUTOS_EXPIRACION, TipoCodigo.VERIFICACION_EMAIL);
            codigoRecuperacionRepository.save(codigoVerificacion);

            EmailDto emailDto = new EmailDto(
                    "Verificación de email - RecetasApp",
                    "Hola " + usuario.getNombre() + ",\n\n"
                            + "Usa el siguiente código para verificar tu email:\n\n"
                            + "Código de verificación: " + codigo + "\n\n"
                            + "Este código es válido por " + MINUTOS_EXPIRACION + " minutos.\n\n"
                            + "Si no solicitaste esto, ignora este mensaje.",
                    usuario.getEmail()
            );

            emailService.sendMail(emailDto);

            return new ApiResponse<>(true, "Código de verificación enviado a tu correo");

        } catch (Exception e) {
            System.out.println("Error al solicitar verificación de email: " + e.getMessage());
            return new ApiResponse<>(false, "No se pudo enviar el código: " + e.getMessage());
        }
    }

    public ApiResponse<Void> verificarEmail(Long usuarioId, String codigo) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);

            if (usuarioOpt.isEmpty()) {
                return new ApiResponse<>(false, "Usuario no encontrado");
            }

            Usuario usuario = usuarioOpt.get();

            if (usuario.isEmailVerificado()) {
                return new ApiResponse<>(false, "El email ya está verificado");
            }

            Optional<Codigo> codigoOpt = codigoRecuperacionRepository
                    .findTopByUsuarioAndUsadoFalseAndTipoOrderByFechaCreacionDesc(usuario, TipoCodigo.VERIFICACION_EMAIL);

            if (codigoOpt.isEmpty()) {
                return new ApiResponse<>(false, "No hay un código de verificación activo. Solicita uno nuevo");
            }

            Codigo codigoEntity = codigoOpt.get();

            if (!codigoEntity.getCodigo().equals(codigo.trim())) {
                return new ApiResponse<>(false, "El código de verificación es incorrecto");
            }

            if (codigoEntity.isExpirado()) {
                codigoEntity.setUsado(true);
                codigoRecuperacionRepository.save(codigoEntity);
                return new ApiResponse<>(false, "El código ha expirado. Solicita uno nuevo");
            }

            codigoEntity.setUsado(true);
            codigoRecuperacionRepository.save(codigoEntity);

            usuario.setEmailVerificado(true);
            usuarioRepository.save(usuario);

            return new ApiResponse<>(true, "Email verificado correctamente");

        } catch (Exception e) {
            System.out.println("Error al verificar email: " + e.getMessage());
            return new ApiResponse<>(false, "Error interno del servidor");
        }
    }
}
