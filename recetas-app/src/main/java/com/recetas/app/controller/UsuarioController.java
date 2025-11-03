package com.recetas.app.controller;

import com.recetas.app.dto.ApiResponse;
import com.recetas.app.dto.EditarUsuarioRequest;
import com.recetas.app.dto.UsuarioResponse;
import com.recetas.app.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {
    @Autowired
    private UsuarioService usuarioService;

    // Obtener perfil del usuario
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtenerPerfil(@PathVariable Long id) {
        ApiResponse<UsuarioResponse> response = usuarioService.obtenerPerfil(id);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // Actualizar perfil (nombre y opcionalmente foto)
    @PutMapping("/{id}/perfil")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizarPerfil(
            @PathVariable Long id,
            @Valid @RequestBody EditarUsuarioRequest request) {

        ApiResponse<UsuarioResponse> response = usuarioService.actualizarPerfil(id, request);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Subir foto de perfil
    @PostMapping("/{id}/foto")
    public ResponseEntity<ApiResponse<String>> subirFotoPerfil(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        ApiResponse<String> response = usuarioService.subirFotoPerfil(id, file);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
