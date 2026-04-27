package com.recetas.app.service;

import com.recetas.app.dto.*;
import com.recetas.app.entity.Codigo;
import com.recetas.app.entity.TipoCodigo;
import com.recetas.app.entity.Usuario;
import com.recetas.app.repository.CodigoRecuperacionRepository;
import com.recetas.app.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordRecoveryService {

  private static final int MINUTOS_EXPIRACION = 15;

  @Autowired
  private UsuarioRepository usuarioRepository;

  @Autowired
  private CodigoRecuperacionRepository codigoRecuperacionRepository;

  @Autowired
  private EmailService emailService;

  @Autowired
  private CodeGenerationService codeGenerationService;

  private final BCryptPasswordEncoder passwordEncoder =
    new BCryptPasswordEncoder();

  /**
   * Genera un código de recuperación y lo envía al email del usuario.
   */
  @Transactional
  public ApiResponse<Void> solicitarRecuperacion(
    RecuperarPasswordRequest request
  ) {
    try {
      Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(
        request.getEmail()
      );

      if (usuarioOpt.isEmpty()) {
        // Por seguridad, no revelamos si el email existe o no
        return new ApiResponse<>(
          true,
          "Si el email está registrado, recibirás un código de recuperación"
        );
      }

      Usuario usuario = usuarioOpt.get();

      // Invalidar códigos anteriores no usados
      List<Codigo> codigosAnteriores =
        codigoRecuperacionRepository.findByUsuarioAndUsadoFalse(usuario);
      for (Codigo c : codigosAnteriores) {
        if (c.getTipo().equals(TipoCodigo.RECUPERACION)) {
          c.setUsado(true);
        }
      }
      codigoRecuperacionRepository.saveAll(codigosAnteriores);

      // Generar nuevo código de 6 dígitos
      String codigo = codeGenerationService.generarCodigo();

      // Guardar en la base de datos
      Codigo codigoRecuperacion = new Codigo(
        codigo,
        usuario,
        MINUTOS_EXPIRACION,
        TipoCodigo.RECUPERACION
      );
      codigoRecuperacionRepository.save(codigoRecuperacion);

      // Enviar email con el código
      EmailDto emailDto = new EmailDto(
        "Recuperación de contraseña - RecetasApp",
        "Hola " +
          usuario.getNombre() +
          ",\n\n" +
          "Tu código de recuperación es: " +
          codigo +
          "\n\n" +
          "Este código expira en " +
          MINUTOS_EXPIRACION +
          " minutos.\n\n" +
          "Si no solicitaste este cambio, ignora este mensaje.\n\n" +
          "- El equipo de El Rincón De Los Sabores",
        usuario.getEmail()
      );

      try {
        emailService.sendMail(emailDto);
      } catch (Exception emailError) {
        codigoRecuperacion.setUsado(true);
        codigoRecuperacionRepository.save(codigoRecuperacion);
        return new ApiResponse<>(
          false,
          "No se pudo enviar el codigo de recuperacion: " +
            extraerMensajeEmail(emailError)
        );
      }

      return new ApiResponse<>(
        true,
        "Si el email esta registrado, recibiras un codigo de recuperacion"
      );
    } catch (Exception e) {
      System.out.println("Error al solicitar recuperacion: " + e.getMessage());
      e.printStackTrace();
      return new ApiResponse<>(false, "Error interno del servidor");
    }
  }

  /**
   * Valida el código y cambia la contraseña del usuario.
   */
  @Transactional
  public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
    try {
      Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(
        request.getEmail()
      );

      if (usuarioOpt.isEmpty()) {
        return new ApiResponse<>(false, "Credenciales inválidas");
      }

      Usuario usuario = usuarioOpt.get();

      // Buscar el código más reciente no usado del usuario
      Optional<Codigo> codigoOpt =
        codigoRecuperacionRepository.findTopByUsuarioAndUsadoFalseAndTipoOrderByFechaCreacionDesc(
          usuario,
          TipoCodigo.RECUPERACION
        );

      if (codigoOpt.isEmpty()) {
        return new ApiResponse<>(
          false,
          "No hay un código de recuperación activo. Solicita uno nuevo"
        );
      }

      Codigo codigo = codigoOpt.get();

      // Verificar que el código coincida
      if (!codigo.getCodigo().equals(request.getCodigo())) {
        return new ApiResponse<>(
          false,
          "El código de recuperación es incorrecto"
        );
      }

      // Verificar que no haya expirado
      if (codigo.isExpirado()) {
        codigo.setUsado(true);
        codigoRecuperacionRepository.save(codigo);
        return new ApiResponse<>(
          false,
          "El código de recuperación ha expirado. Solicita uno nuevo"
        );
      }

      // Marcar código como usado
      codigo.setUsado(true);
      codigoRecuperacionRepository.save(codigo);

      // Cambiar la contraseña
      usuario.setPassword(passwordEncoder.encode(request.getNuevaPassword()));
      usuarioRepository.save(usuario);

      return new ApiResponse<>(true, "Contraseña actualizada exitosamente");
    } catch (Exception e) {
      System.out.println("Error en reset password: " + e.getMessage());
      e.printStackTrace();
      return new ApiResponse<>(false, "Error interno del servidor");
    }
  }

  private String extraerMensajeEmail(Exception exception) {
    if (exception.getMessage() == null || exception.getMessage().isBlank()) {
      return "Revisa la configuracion SMTP del servidor";
    }
    return exception.getMessage();
  }
}
