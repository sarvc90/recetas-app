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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recetas")
@CrossOrigin(origins = "*")
public class RecetaController {

    @Autowired
    private RecetaRepository recetaRepository;

    @Autowired
    private ImageService imageService;

    // ✅ Obtener todas las recetas (paginadas)
    @GetMapping
    public Page<Receta> getAllRecetas(Pageable pageable) {
        return recetaRepository.findAll(pageable);
    }

    // Obtener recetas por usuario (sin paginación para el modal de eliminación)
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Receta>> getRecetasByUsuario(@PathVariable Long usuarioId) {
        List<Receta> recetas = recetaRepository.findByUsuarioId(usuarioId, Pageable.unpaged()).getContent();
        return ResponseEntity.ok(recetas);
    }

    // ✅ Crear nueva receta
    @PostMapping
    public ResponseEntity<Receta> crearReceta(@RequestBody Receta receta) {
        receta.setFechaCreacion(LocalDateTime.now());
        return ResponseEntity.ok(recetaRepository.save(receta));
    }

    // ✅ Subida de imágenes
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

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarReceta(@PathVariable Long id, @RequestBody Receta recetaActualizada) {
        return recetaRepository.findById(id)
                .<ResponseEntity<?>>map(receta -> {   // 👈 Forzamos a que el map devuelva ResponseEntity<?>
                    receta.setNombre(recetaActualizada.getNombre());
                    receta.setDescripcion(recetaActualizada.getDescripcion());
                    receta.setIngredientes(recetaActualizada.getIngredientes());
                    receta.setInstrucciones(recetaActualizada.getInstrucciones());
                    receta.setImagenUrl(recetaActualizada.getImagenUrl());
                    receta.setFechaActualizacion(LocalDateTime.now());

                    Receta guardada = recetaRepository.save(receta);
                    return ResponseEntity.ok(guardada); // ✅ ahora el compilador entiende que es ResponseEntity<?>
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse(false, "Receta no encontrada"))); // ✅ también ResponseEntity<?>
    }

    // Eliminar receta
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarReceta(@PathVariable Long id) {
        return recetaRepository.findById(id)
                .<ResponseEntity<?>>map(receta -> {
                    recetaRepository.deleteById(id);
                    return ResponseEntity.ok(new ApiResponse(true, "Receta eliminada correctamente"));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse(false, "Receta no encontrada")));
    }
}
