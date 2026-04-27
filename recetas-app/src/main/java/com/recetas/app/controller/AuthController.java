package com.recetas.app.controller;

import com.recetas.app.dto.*;
import com.recetas.app.service.AuthService;
import com.recetas.app.service.PasswordRecoveryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

  @Autowired
  private AuthService authService;

  @Autowired
  private PasswordRecoveryService passwordRecoveryService;

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<UsuarioResponse>> login(
    @Valid @RequestBody LoginRequest request
  ) {
    try {
      return ResponseEntity.ok(authService.login(request));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(
        new ApiResponse<>(false, e.getMessage())
      );
    }
  }

  @PostMapping("/registro")
  public ResponseEntity<ApiResponse<UsuarioResponse>> registro(
    @Valid @RequestBody RegistroRequest request
  ) {
    try {
      return ResponseEntity.ok(authService.registro(request));
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(
        new ApiResponse<>(false, e.getMessage())
      );
    }
  }

  @PostMapping("/recuperar-password")
  public ResponseEntity<ApiResponse<Void>> recuperarPassword(
    @Valid @RequestBody RecuperarPasswordRequest request
  ) {
    return ResponseEntity.ok(
      passwordRecoveryService.solicitarRecuperacion(request)
    );
  }

  @PostMapping("/reset-password")
  public ResponseEntity<ApiResponse<Void>> resetPassword(
    @Valid @RequestBody ResetPasswordRequest request
  ) {
    return ResponseEntity.ok(passwordRecoveryService.resetPassword(request));
  }
}
