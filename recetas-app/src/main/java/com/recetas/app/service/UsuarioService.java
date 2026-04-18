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

import java.util.List;

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
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        UsuarioResponse response = buildUsuarioResponse(usuario);
        return new ApiResponse<>(true, "Perfil obtenido exitosamente", response);
    }

    public ApiResponse<UsuarioResponse> actualizarPerfil(Long usuarioId, EditarUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        usuario.setNombre(request.getNombre());

        if (request.getFotoPerfil() != null && !request.getFotoPerfil().isEmpty()) {
            usuario.setFotoPerfil(request.getFotoPerfil());
        }

        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        return new ApiResponse<>(true, "Perfil actualizado exitosamente", buildUsuarioResponse(usuarioActualizado));
    }

    public ApiResponse<String> subirFotoPerfil(Long usuarioId, MultipartFile file) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        }

        try {
            String imageUrl = imageService.uploadImage(file);
            usuario.setFotoPerfil(imageUrl);
            usuarioRepository.save(usuario);
            return new ApiResponse<>(true, "Foto subida exitosamente", imageUrl);
        } catch (Exception e) {
            throw new RuntimeException("Error al subir la imagen: " + e.getMessage());
        }
    }

    public ApiResponse<Void> solicitarVerificacionEmail(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (usuario.isEmailVerificado()) {
            throw new IllegalArgumentException("El email ya está verificado");
        }

        List<Codigo> codigosAnteriores = codigoRecuperacionRepository.findByUsuarioAndUsadoFalse(usuario);
        codigosAnteriores.stream()
                .filter(c -> TipoCodigo.VERIFICACION_EMAIL.equals(c.getTipo()))
                .forEach(c -> c.setUsado(true));
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

        try {
            emailService.sendMail(emailDto);
        } catch (Exception e) {
            codigoVerificacion.setUsado(true);
            codigoRecuperacionRepository.save(codigoVerificacion);
            throw new RuntimeException("No se pudo enviar el código: " + e.getMessage());
        }

        return new ApiResponse<>(true, "Código de verificación enviado a tu correo");
    }

    public ApiResponse<Void> verificarEmail(Long usuarioId, String codigo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (usuario.isEmailVerificado()) {
            throw new IllegalArgumentException("El email ya está verificado");
        }

        Codigo codigoEntity = codigoRecuperacionRepository
                .findTopByUsuarioAndUsadoFalseAndTipoOrderByFechaCreacionDesc(usuario, TipoCodigo.VERIFICACION_EMAIL)
                .orElseThrow(() -> new IllegalArgumentException("No hay un código de verificación activo. Solicita uno nuevo"));

        if (!codigoEntity.getCodigo().equals(codigo.trim())) {
            throw new IllegalArgumentException("El código de verificación es incorrecto");
        }

        if (codigoEntity.isExpirado()) {
            codigoEntity.setUsado(true);
            codigoRecuperacionRepository.save(codigoEntity);
            throw new IllegalArgumentException("El código ha expirado. Solicita uno nuevo");
        }

        codigoEntity.setUsado(true);
        codigoRecuperacionRepository.save(codigoEntity);

        usuario.setEmailVerificado(true);
        usuarioRepository.save(usuario);

        return new ApiResponse<>(true, "Email verificado correctamente");
    }

    // ================== HELPER ==================
    private UsuarioResponse buildUsuarioResponse(Usuario usuario) {
        UsuarioResponse response = new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getFotoPerfil()
        );
        response.setEmailVerificado(usuario.isEmailVerificado());
        return response;
    }
}