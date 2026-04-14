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

            if (!usuario.isEmailVerificado()) {
                return new ApiResponse<>(false, "Debes verificar tu email antes de iniciar sesión");
            }

            if (request.getAuthCode() == null) {
                // Verificacion en dos pasos.
                invalidarCodigosPorTipo(usuario, TipoCodigo.AUTENTIFICACION);

                String codigo = codeGenerationService.generarCodigo();
                Codigo codigoAutenticacion = new Codigo(codigo, usuario, MINUTOS_EXPIRACION, TipoCodigo.AUTENTIFICACION);
                codigoRecuperacionRepository.save(codigoAutenticacion);

                EmailDto emailDto = new EmailDto(
                        "Autenticacion de usuario - RecetasApp",
                        "Hola " + usuario.getNombre() + ",\n\n"
                                + "Alguien ha intentado iniciar sesion en tu cuenta. Si fuiste tu, usa el siguiente codigo para completar el inicio de sesion:\n\n"
                                + "Codigo de autenticacion: " + codigo + "\n\n"
                                + "Este codigo es valido por " + MINUTOS_EXPIRACION + " minutos.\n\n"
                                + "Si no fuiste tu, te recomendamos cambiar tu contrasena inmediatamente.",
                        usuario.getEmail()
                );

                try {
                    emailService.sendMail(emailDto);
                } catch (Exception emailError) {
                    codigoAutenticacion.setUsado(true);
                    codigoRecuperacionRepository.save(codigoAutenticacion);
                    return new ApiResponse<>(false, "No se pudo enviar el codigo de autenticacion: " + extraerMensajeEmail(emailError));
                }

                return new ApiResponse<>(true, "Si el email esta registrado, recibiras un codigo de autenticacion para completar el inicio de sesion");
            }

            Optional<Codigo> codigoOpt = codigoRecuperacionRepository
                    .findTopByUsuarioAndUsadoFalseAndTipoOrderByFechaCreacionDesc(usuario, TipoCodigo.AUTENTIFICACION);

            if (codigoOpt.isEmpty()) {
                return new ApiResponse<>(false, "No hay un codigo de autenticacion activo. Solicita uno nuevo");
            }

            Codigo codigo = codigoOpt.get();

            if (!TipoCodigo.AUTENTIFICACION.equals(codigo.getTipo())) {
                return new ApiResponse<>(false, "Codigo invalido para autenticacion");
            }

            if (!codigo.getCodigo().equals(request.getAuthCode())) {
                return new ApiResponse<>(false, "El codigo de autenticacion es incorrecto");
            }

            if (codigo.isExpirado()) {
                codigo.setUsado(true);
                codigoRecuperacionRepository.save(codigo);
                return new ApiResponse<>(false, "El codigo de autenticacion ha expirado. Solicita uno nuevo");
            }

            codigo.setUsado(true);
            codigoRecuperacionRepository.save(codigo);

            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);

            String token = jwtUtil.generateToken(usuario.getId(), usuario.getEmail());

            UsuarioResponse usuarioResponse = new UsuarioResponse(
                    usuario.getId(),
                    usuario.getNombre(),
                    usuario.getEmail(),
                    usuario.getFotoPerfil(),
                    token
            );
            usuarioResponse.setEmailVerificado(usuario.isEmailVerificado());

            return new ApiResponse<>(true, "Login exitoso", usuarioResponse);

        } catch (Exception e) {
            System.out.println("Error en login: " + e.getMessage());
            e.printStackTrace();
            return new ApiResponse<>(false, "Error interno del servidor");
        }
    }

    public ApiResponse<UsuarioResponse> registro(RegistroRequest request) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getEmail());
            String verificationCode = normalizarCodigo(request.getVerificationCode());

            if (verificationCode == null) {
                return iniciarRegistroConVerificacion(request, usuarioOpt);
            }

            return confirmarRegistroConCodigo(request, usuarioOpt, verificationCode);

        } catch (Exception e) {
            System.out.println("Error en registro: " + e.getMessage());
            e.printStackTrace();
            return new ApiResponse<>(false, "Error interno del servidor");
        }
    }

    private ApiResponse<UsuarioResponse> iniciarRegistroConVerificacion(RegistroRequest request, Optional<Usuario> usuarioOpt) {
        Usuario usuario;

        if (usuarioOpt.isPresent()) {
            usuario = usuarioOpt.get();

            if (usuario.isEmailVerificado()) {
                return new ApiResponse<>(false, "El email ya esta registrado");
            }

            if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
                return new ApiResponse<>(false, "Ya existe un registro pendiente para este email. Verifica tu email con la misma contrasena usada al registrarte");
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
                "Verificacion de email - RecetasApp",
                "Hola " + usuario.getNombre() + ",\n\n"
                        + "Para completar tu registro, usa este codigo de verificacion:\n\n"
                        + "Codigo de verificacion: " + codigo + "\n\n"
                        + "Este codigo es valido por " + MINUTOS_EXPIRACION + " minutos.\n\n"
                        + "Si no solicitaste este registro, ignora este mensaje.",
                usuario.getEmail()
        );

        try {
            emailService.sendMail(emailDto);
        } catch (Exception emailError) {
            codigoVerificacion.setUsado(true);
            codigoRecuperacionRepository.save(codigoVerificacion);
            return new ApiResponse<>(false, "No se pudo enviar el codigo de verificacion: " + extraerMensajeEmail(emailError));
        }

        return new ApiResponse<>(true, "Te enviamos un codigo de verificacion al email. Envia nuevamente el registro con verificationCode para activar la cuenta");
    }

    private ApiResponse<UsuarioResponse> confirmarRegistroConCodigo(RegistroRequest request, Optional<Usuario> usuarioOpt, String verificationCode) {
        if (usuarioOpt.isEmpty()) {
            return new ApiResponse<>(false, "No existe un registro pendiente para este email. Solicita primero el codigo de verificacion");
        }

        Usuario usuario = usuarioOpt.get();

        if (usuario.isEmailVerificado()) {
            return new ApiResponse<>(false, "El email ya esta verificado. Puedes iniciar sesion");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            return new ApiResponse<>(false, "Credenciales incorrectas");
        }

        Optional<Codigo> codigoOpt = codigoRecuperacionRepository
                .findTopByUsuarioAndUsadoFalseAndTipoOrderByFechaCreacionDesc(usuario, TipoCodigo.VERIFICACION_EMAIL);

        if (codigoOpt.isEmpty()) {
            return new ApiResponse<>(false, "No hay un codigo de verificacion activo. Solicita uno nuevo");
        }

        Codigo codigo = codigoOpt.get();

        if (!verificationCode.equals(codigo.getCodigo())) {
            return new ApiResponse<>(false, "El codigo de verificacion es incorrecto");
        }

        if (codigo.isExpirado()) {
            codigo.setUsado(true);
            codigoRecuperacionRepository.save(codigo);
            return new ApiResponse<>(false, "El codigo de verificacion ha expirado. Solicita uno nuevo");
        }

        codigo.setUsado(true);
        codigoRecuperacionRepository.save(codigo);

        usuario.setEmailVerificado(true);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        String token = jwtUtil.generateToken(usuario.getId(), usuario.getEmail());

        UsuarioResponse usuarioResponse = new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getFotoPerfil(),
                token
        );
        usuarioResponse.setEmailVerificado(usuario.isEmailVerificado());

        return new ApiResponse<>(true, "Email verificado y registro completado exitosamente", usuarioResponse);
    }

    private void invalidarCodigosPorTipo(Usuario usuario, TipoCodigo tipoCodigo) {
        List<Codigo> codigosAnteriores = codigoRecuperacionRepository.findByUsuarioAndUsadoFalse(usuario);
        for (Codigo codigoAnterior : codigosAnteriores) {
            if (tipoCodigo.equals(codigoAnterior.getTipo())) {
                codigoAnterior.setUsado(true);
            }
        }
        codigoRecuperacionRepository.saveAll(codigosAnteriores);
    }

    private String normalizarCodigo(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public ApiResponse<UsuarioResponse> getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return new ApiResponse<>(false, "No hay una sesión activa");
        }

        String email = (String) authentication.getPrincipal();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isEmpty()) {
            return new ApiResponse<>(false, "Usuario no encontrado");
        }

        Usuario usuario = usuarioOpt.get();
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

    private String extraerMensajeEmail(Exception exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "Revisa la configuracion SMTP del servidor";
        }
        return exception.getMessage();
    }
}

