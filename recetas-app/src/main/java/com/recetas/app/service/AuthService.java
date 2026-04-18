package com.recetas.app.service;

import com.recetas.app.config.JwtUtil;
import com.recetas.app.dto.ApiResponse;
import com.recetas.app.dto.EmailDto;
import com.recetas.app.dto.LoginRequest;
import com.recetas.app.dto.RegistroRequest;
import com.recetas.app.dto.UsuarioResponse;
import com.recetas.app.entity.Codigo;
import com.recetas.app.entity.TipoCodigo;
import com.recetas.app.entity.Usuario;
import com.recetas.app.repository.CodigoRecuperacionRepository;
import com.recetas.app.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    private static final int MINUTOS_EXPIRACION = 5;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CodigoRecuperacionRepository codigoRecuperacionRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CodeGenerationService codeGenerationService;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ================== LOGIN ==================
    public ApiResponse<UsuarioResponse> login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new IllegalArgumentException("Credenciales incorrectas");
        }

        if (!usuario.isEmailVerificado()) {
            throw new IllegalArgumentException("Debes verificar tu email antes de iniciar sesión");
        }

        if (request.getAuthCode() == null) {
            enviarCodigoAutenticacion(usuario);
            return new ApiResponse<>(true, "Si el email está registrado, recibirás un código de autenticación para completar el inicio de sesión");
        }

        validarCodigoAutenticacion(usuario, request.getAuthCode());

        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        return new ApiResponse<>(true, "Login exitoso", buildUsuarioResponse(usuario));
    }

    private void enviarCodigoAutenticacion(Usuario usuario) {
        invalidarCodigosPorTipo(usuario, TipoCodigo.AUTENTIFICACION);

        String codigo = codeGenerationService.generarCodigo();
        Codigo codigoAutenticacion = new Codigo(codigo, usuario, MINUTOS_EXPIRACION, TipoCodigo.AUTENTIFICACION);
        codigoRecuperacionRepository.save(codigoAutenticacion);

        EmailDto emailDto = new EmailDto(
                "Autenticacion de usuario - RecetasApp",
                "Hola " + usuario.getNombre() + ",\n\n"
                        + "Alguien ha intentado iniciar sesión en tu cuenta. Si fuiste tú, usa el siguiente código:\n\n"
                        + "Código de autenticación: " + codigo + "\n\n"
                        + "Este código es válido por " + MINUTOS_EXPIRACION + " minutos.\n\n"
                        + "Si no fuiste tú, te recomendamos cambiar tu contraseña inmediatamente.",
                usuario.getEmail()
        );

        try {
            emailService.sendMail(emailDto);
        } catch (Exception e) {
            codigoAutenticacion.setUsado(true);
            codigoRecuperacionRepository.save(codigoAutenticacion);
            throw new IllegalArgumentException("No se pudo enviar el código de autenticación: " + extraerMensajeEmail(e));
        }
    }

    private void validarCodigoAutenticacion(Usuario usuario, String authCode) {
        Codigo codigo = codigoRecuperacionRepository
                .findTopByUsuarioAndUsadoFalseAndTipoOrderByFechaCreacionDesc(usuario, TipoCodigo.AUTENTIFICACION)
                .orElseThrow(() -> new IllegalArgumentException("No hay un código de autenticación activo. Solicita uno nuevo"));

        if (!TipoCodigo.AUTENTIFICACION.equals(codigo.getTipo())) {
            throw new IllegalArgumentException("Código inválido para autenticación");
        }

        if (!codigo.getCodigo().equals(authCode)) {
            throw new IllegalArgumentException("El código de autenticación es incorrecto");
        }

        if (codigo.isExpirado()) {
            codigo.setUsado(true);
            codigoRecuperacionRepository.save(codigo);
            throw new IllegalArgumentException("El código de autenticación ha expirado. Solicita uno nuevo");
        }

        codigo.setUsado(true);
        codigoRecuperacionRepository.save(codigo);
    }

    // ================== REGISTRO ==================
    public ApiResponse<UsuarioResponse> registro(RegistroRequest request) {
        String verificationCode = normalizarCodigo(request.getVerificationCode());
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getEmail());

        if (verificationCode == null) {
            return iniciarRegistroConVerificacion(request, usuarioOpt);
        }

        return confirmarRegistroConCodigo(request, usuarioOpt, verificationCode);
    }

    private ApiResponse<UsuarioResponse> iniciarRegistroConVerificacion(RegistroRequest request, Optional<Usuario> usuarioOpt) {
        Usuario usuario;

        if (usuarioOpt.isPresent()) {
            usuario = usuarioOpt.get();

            if (usuario.isEmailVerificado()) {
                throw new IllegalArgumentException("El email ya está registrado");
            }

            if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
                throw new IllegalArgumentException("Ya existe un registro pendiente para este email. Verifica tu email con la misma contraseña usada al registrarte");
            }
        } else {
            usuario = new Usuario(
                    request.getNombre(),
                    request.getEmail(),
                    passwordEncoder.encode(request.getPassword())
            );
            usuario.setFotoPerfil(null);
            usuario.setEmailVerificado(false);
            usuario = usuarioRepository.save(usuario);
        }

        invalidarCodigosPorTipo(usuario, TipoCodigo.VERIFICACION_EMAIL);

        String codigo = codeGenerationService.generarCodigo();
        Codigo codigoVerificacion = new Codigo(codigo, usuario, MINUTOS_EXPIRACION, TipoCodigo.VERIFICACION_EMAIL);
        codigoRecuperacionRepository.save(codigoVerificacion);

        EmailDto emailDto = new EmailDto(
                "Verificación de email - RecetasApp",
                "Hola " + usuario.getNombre() + ",\n\n"
                        + "Para completar tu registro, usa este código de verificación:\n\n"
                        + "Código de verificación: " + codigo + "\n\n"
                        + "Este código es válido por " + MINUTOS_EXPIRACION + " minutos.\n\n"
                        + "Si no solicitaste este registro, ignora este mensaje.",
                usuario.getEmail()
        );

        try {
            emailService.sendMail(emailDto);
        } catch (Exception e) {
            codigoVerificacion.setUsado(true);
            codigoRecuperacionRepository.save(codigoVerificacion);
            throw new RuntimeException("No se pudo enviar el código de verificación: " + extraerMensajeEmail(e));
        }

        return new ApiResponse<>(true, "Te enviamos un código de verificación al email. Envía nuevamente el registro con verificationCode para activar la cuenta");
    }

    private ApiResponse<UsuarioResponse> confirmarRegistroConCodigo(RegistroRequest request, Optional<Usuario> usuarioOpt, String verificationCode) {
        Usuario usuario = usuarioOpt
                .orElseThrow(() -> new IllegalArgumentException("No existe un registro pendiente para este email. Solicita primero el código de verificación"));

        if (usuario.isEmailVerificado()) {
            throw new IllegalArgumentException("El email ya está verificado. Puedes iniciar sesión");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new IllegalArgumentException("Credenciales incorrectas");
        }

        Codigo codigo = codigoRecuperacionRepository
                .findTopByUsuarioAndUsadoFalseAndTipoOrderByFechaCreacionDesc(usuario, TipoCodigo.VERIFICACION_EMAIL)
                .orElseThrow(() -> new IllegalArgumentException("No hay un código de verificación activo. Solicita uno nuevo"));

        if (!verificationCode.equals(codigo.getCodigo())) {
            throw new IllegalArgumentException("El código de verificación es incorrecto");
        }

        if (codigo.isExpirado()) {
            codigo.setUsado(true);
            codigoRecuperacionRepository.save(codigo);
            throw new IllegalArgumentException("El código de verificación ha expirado. Solicita uno nuevo");
        }

        codigo.setUsado(true);
        codigoRecuperacionRepository.save(codigo);

        usuario.setEmailVerificado(true);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        return new ApiResponse<>(true, "Email verificado y registro completado exitosamente", buildUsuarioResponse(usuario));
    }

    // ================== USUARIO AUTENTICADO ==================
    public ApiResponse<UsuarioResponse> getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("No hay una sesión activa");
        }

        String email = (String) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        UsuarioResponse usuarioResponse = new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getFotoPerfil(),
                null
        );
        usuarioResponse.setEmailVerificado(usuario.isEmailVerificado());

        return new ApiResponse<>(true, "Usuario obtenido exitosamente", usuarioResponse);
    }

    // ================== HELPERS ==================
    private UsuarioResponse buildUsuarioResponse(Usuario usuario) {
        String token = jwtUtil.generateToken(usuario.getId(), usuario.getEmail());
        UsuarioResponse response = new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getFotoPerfil(),
                token
        );
        response.setEmailVerificado(usuario.isEmailVerificado());
        return response;
    }

    private void invalidarCodigosPorTipo(Usuario usuario, TipoCodigo tipoCodigo) {
        List<Codigo> codigos = codigoRecuperacionRepository.findByUsuarioAndUsadoFalse(usuario);
        codigos.stream()
                .filter(c -> tipoCodigo.equals(c.getTipo()))
                .forEach(c -> c.setUsado(true));
        codigoRecuperacionRepository.saveAll(codigos);
    }

    private String normalizarCodigo(String code) {
        if (code == null) return null;
        String normalized = code.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String extraerMensajeEmail(Exception exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "Revisa la configuración SMTP del servidor";
        }
        return exception.getMessage();
    }
}