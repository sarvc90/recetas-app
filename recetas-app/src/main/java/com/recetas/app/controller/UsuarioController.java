package com.recetas.app.controller;

import com.recetas.app.dto.ApiResponse;
import com.recetas.app.dto.EditarUsuarioRequest;
import com.recetas.app.dto.UsuarioResponse;
import com.recetas.app.dto.VerificarEmailRequest;
import com.recetas.app.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtenerPerfil(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPerfil(id));
    }

    @PutMapping("/{id}/perfil")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizarPerfil(
            @PathVariable Long id,
            @Valid @RequestBody EditarUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarPerfil(id, request));
    }

    @PostMapping("/{id}/foto")
    public ResponseEntity<ApiResponse<String>> subirFotoPerfil(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(usuarioService.subirFotoPerfil(id, file));
    }

    @PostMapping("/{id}/solicitar-verificacion-email")
    public ResponseEntity<ApiResponse<Void>> solicitarVerificacionEmail(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.solicitarVerificacionEmail(id));
    }

    @PostMapping("/{id}/verificar-email")
    public ResponseEntity<ApiResponse<Void>> verificarEmail(
            @PathVariable Long id,
            @Valid @RequestBody VerificarEmailRequest request) {
        return ResponseEntity.ok(usuarioService.verificarEmail(id, request.getCodigo()));
    }
}