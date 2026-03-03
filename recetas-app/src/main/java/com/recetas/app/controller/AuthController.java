package com.recetas.app.controller;

import com.recetas.app.dto.*;
import com.recetas.app.service.AuthService;
import com.recetas.app.service.PasswordRecoveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UsuarioResponse>> login(@Valid @RequestBody LoginRequest request) {
        ApiResponse<UsuarioResponse> response = authService.login(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/registro")
    public ResponseEntity<ApiResponse<UsuarioResponse>> registro(@Valid @RequestBody RegistroRequest request) {
        ApiResponse<UsuarioResponse> response = authService.registro(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/recuperar-password")
    public ResponseEntity<ApiResponse<Void>> recuperarPassword(@Valid @RequestBody RecuperarPasswordRequest request) {
        ApiResponse<Void> response = passwordRecoveryService.solicitarRecuperacion(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ApiResponse<Void> response = passwordRecoveryService.resetPassword(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}