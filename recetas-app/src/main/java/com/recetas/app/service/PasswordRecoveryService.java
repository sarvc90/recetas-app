package com.recetas.app.service;

import com.recetas.app.dto.*;
import com.recetas.app.entity.CodigoRecuperacion;
import com.recetas.app.entity.Usuario;
import com.recetas.app.repository.CodigoRecuperacionRepository;
import com.recetas.app.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
public class PasswordRecoveryService {

    private static final int MINUTOS_EXPIRACION = 15;
    private static final int LONGITUD_CODIGO = 6;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CodigoRecuperacionRepository codigoRecuperacionRepository;

    @Autowired
    private EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Genera un codigo de recuperacion y lo envia al email del usuario.
     */
    @Transactional
    public ApiResponse<Void> solicitarRecuperacion(RecuperarPasswordRequest request) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getEmail());

            if (usuarioOpt.isEmpty()) {
                // Por seguridad, no revelamos si el email existe o no
                return new ApiResponse<>(true, "Si el email esta registrado, recibiras un codigo de recuperacion");
            }

            Usuario usuario = usuarioOpt.get();

            // Invalidar codigos anteriores no usados
            List<CodigoRecuperacion> codigosAnteriores = codigoRecuperacionRepository.findByUsuarioAndUsadoFalse(usuario);
            for (CodigoRecuperacion c : codigosAnteriores) {
                c.setUsado(true);
            }
            codigoRecuperacionRepository.saveAll(codigosAnteriores);

            // Generar nuevo codigo de 6 digitos
            String codigo = generarCodigo();

            // Guardar en la base de datos
            CodigoRecuperacion codigoRecuperacion = new CodigoRecuperacion(codigo, usuario, MINUTOS_EXPIRACION);
            codigoRecuperacionRepository.save(codigoRecuperacion);

            // Enviar email con el codigo
            EmailDto emailDto = new EmailDto(
                    "Recuperación de contraseña - RecetasApp",
                    "Hola " + usuario.getNombre() + ",\n\n"
                            + "Tu código de recuperación es: " + codigo + "\n\n"
                            + "Este código expira en " + MINUTOS_EXPIRACION + " minutos.\n\n"
                            + "Si no solicitaste este cambio, ignora este mensaje.\n\n"
                            + "- El equipo de El Rincón De Los Sabores",
                    usuario.getEmail()
            );
            emailService.sendMail(emailDto);

            return new ApiResponse<>(true, "Si el email esta registrado, recibiras un codigo de recuperacion");

        } catch (Exception e) {
            System.out.println("Error en solicitar recuperacion: " + e.getMessage());
            e.printStackTrace();
            return new ApiResponse<>(false, "Error interno del servidor");
        }
    }

    /**
     * Valida el codigo y cambia la contraseña del usuario.
     */
    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getEmail());

            if (usuarioOpt.isEmpty()) {
                return new ApiResponse<>(false, "Credenciales invalidas");
            }

            Usuario usuario = usuarioOpt.get();

            // Buscar el codigo mas reciente no usado del usuario
            Optional<CodigoRecuperacion> codigoOpt = codigoRecuperacionRepository
                    .findTopByUsuarioAndUsadoFalseOrderByFechaCreacionDesc(usuario);

            if (codigoOpt.isEmpty()) {
                return new ApiResponse<>(false, "No hay un codigo de recuperacion activo. Solicita uno nuevo");
            }

            CodigoRecuperacion codigoRecuperacion = codigoOpt.get();

            // Verificar que el codigo coincida
            if (!codigoRecuperacion.getCodigo().equals(request.getCodigo())) {
                return new ApiResponse<>(false, "El codigo de recuperacion es incorrecto");
            }

            // Verificar que no haya expirado
            if (codigoRecuperacion.isExpirado()) {
                codigoRecuperacion.setUsado(true);
                codigoRecuperacionRepository.save(codigoRecuperacion);
                return new ApiResponse<>(false, "El codigo de recuperacion ha expirado. Solicita uno nuevo");
            }

            // Marcar codigo como usado
            codigoRecuperacion.setUsado(true);
            codigoRecuperacionRepository.save(codigoRecuperacion);

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

    /**
     * Genera un codigo numerico aleatorio de 6 digitos.
     */
    private String generarCodigo() {
        SecureRandom random = new SecureRandom();
        int numero = random.nextInt(900000) + 100000; // Rango 100000 - 999999
        return String.valueOf(numero);
    }
}

