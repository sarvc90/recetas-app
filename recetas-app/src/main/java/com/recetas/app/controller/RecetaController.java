package com.recetas.app.controller;

import com.recetas.app.dto.ApiResponse;
import com.recetas.app.entity.Receta;
import com.recetas.app.repository.RecetaRepository;
import com.recetas.app.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/recetas")
@CrossOrigin(origins = "*")
public class RecetaController {

    @Autowired
    private RecetaRepository recetaRepository;

    @Autowired
    private ImageService imageService;

    @GetMapping
    public Page<Receta> getAllRecetas(Pageable pageable) {
        return recetaRepository.findAll(pageable);
    }

    @PostMapping
    public ResponseEntity<Receta> crearReceta(@RequestBody Receta receta) {
        receta.setFechaCreacion(LocalDateTime.now());
        return ResponseEntity.ok(recetaRepository.save(receta));
    }

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("Archivo recibido: " + file.getOriginalFilename() + " tamaño: " + file.getSize());
            String imageUrl = imageService.uploadImage(file);
            return ResponseEntity.ok(Map.of("url", imageUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al subir la imagen: " + e.getMessage()));
        }
    }
}