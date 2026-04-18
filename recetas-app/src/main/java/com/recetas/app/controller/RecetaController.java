package com.recetas.app.controller;

import com.recetas.app.dto.ApiResponse;
import com.recetas.app.entity.Receta;
import com.recetas.app.entity.Usuario;
import com.recetas.app.repository.RecetaRepository;
import com.recetas.app.repository.UsuarioRepository;
import com.recetas.app.service.ImageService;
import java.time.LocalDateTime;
import java.util.*;

import com.recetas.app.config.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/recetas")
@CrossOrigin(origins = "*")
public class RecetaController {

  @Autowired
  private RecetaRepository recetaRepository;

  @Autowired
  private ImageService imageService;

  @Autowired
  private JwtUtil jwtUtil;

  @Autowired
  private UsuarioRepository usuarioRepository;

 /* // ✅ Obtener todas las recetas (paginadas)
  @GetMapping
  public Page<Receta> getAllRecetas(Pageable pageable) {
    return recetaRepository.findAll(pageable);
  }*/
 @GetMapping
 public ResponseEntity<Map<String, Object>> getAllRecetas(Pageable pageable) {
     Page<Receta> page = recetaRepository.findAll(pageable);
     Map<String, Object> response = new LinkedHashMap<>();
     response.put("content", new ArrayList<>(page.getContent()));
     response.put("totalElements", page.getTotalElements());
     response.put("totalPages", page.getTotalPages());
     response.put("number", page.getNumber());
     response.put("size", page.getSize());
     return ResponseEntity.ok(response);
 }

  // ✅ Crear nueva receta
  @PostMapping
  public ResponseEntity<Receta> crearReceta(@Valid @RequestBody Receta receta) {
    receta.setFechaCreacion(LocalDateTime.now());
    return ResponseEntity.ok(recetaRepository.save(receta));
  }

  // ✅ Subida de imágenes
  @PostMapping("/upload-image")
  public ResponseEntity<?> uploadImage(
    @RequestParam("file") MultipartFile file
  ) {
    try {
      System.out.println(
        "Archivo recibido: " +
          file.getOriginalFilename() +
          " tamaño: " +
          file.getSize()
      );
      String imageUrl = imageService.uploadImage(file);
      return ResponseEntity.ok(Map.of("url", imageUrl));
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        Map.of("error", "Error al subir la imagen: " + e.getMessage())
      );
    }
  }

  // ✅ Actualizar receta
  @PutMapping("/{id}")
  public ResponseEntity<?> actualizarReceta(
    @PathVariable Long id,
    @RequestBody Receta recetaActualizada,
    @RequestHeader("Authorization") String token
  ) {

      Long usuarioId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));

      return recetaRepository.findById(id)
              .<ResponseEntity<?>>map(receta -> {

                  // 🔴 VALIDACIÓN DE SEGURIDAD
                  if (!receta.getUsuarioId().equals(usuarioId)) {
                      return ResponseEntity.status(HttpStatus.FORBIDDEN)
                              .body(new ApiResponse<>(false, "No tienes permiso para actualizar esta receta"));
                  }

                  receta.setNombre(recetaActualizada.getNombre());
        receta.setDescripcion(recetaActualizada.getDescripcion());
        receta.setIngredientes(recetaActualizada.getIngredientes());
        receta.setInstrucciones(recetaActualizada.getInstrucciones());
        receta.setImagenUrl(recetaActualizada.getImagenUrl());
        receta.setFechaActualizacion(LocalDateTime.now());

        Receta guardada = recetaRepository.save(receta);
        return ResponseEntity.ok(guardada);
      })
      .orElseGet(() ->
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
          new ApiResponse<>(false, "Receta no encontrada")
        )
      );
  }

  // ✅ Eliminar receta
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Object>> eliminarReceta(
          @PathVariable Long id,
          @RequestHeader("Authorization") String token) {

      String jwt = token.replace("Bearer ", "");
      String emailUsuario = jwtUtil.getEmailFromToken(jwt);

      Usuario usuarioActual = usuarioRepository.findByEmail(emailUsuario)
              .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

      Receta receta = recetaRepository.findById(id)
              .orElseThrow(() -> new NoSuchElementException("Receta no encontrada"));

      // 🔐 Validación de seguridad
      if (!receta.getUsuarioId().equals(usuarioActual.getId())) {
          throw new SecurityException("No tienes permiso para eliminar esta receta");
      }

      recetaRepository.deleteById(id);

      return ResponseEntity.ok(
              new ApiResponse<>(true, "Receta eliminada correctamente", null)
      );
  }

  // ✅ Obtener recetas por usuario (todas)
  @GetMapping("/usuario/{usuarioId}")
  public ResponseEntity<List<Receta>> getRecetasByUsuario(
    @PathVariable Long usuarioId
  ) {
    List<Receta> recetas = recetaRepository
      .findByUsuarioId(usuarioId, Pageable.unpaged())
      .getContent();
    return ResponseEntity.ok(recetas);
  }

  // ✅ Buscar recetas por usuario y título
  @GetMapping("/usuario/{usuarioId}/buscar")
  public ResponseEntity<ApiResponse<List<Receta>>> buscarRecetasPorUsuario(
    @PathVariable Long usuarioId,
    @RequestParam String titulo
  ) {
    List<Receta> recetas =
      recetaRepository.findByUsuarioIdAndNombreContainingIgnoreCase(
        usuarioId,
        titulo
      );

    if (recetas.isEmpty()) {
      return ResponseEntity.ok(
        new ApiResponse<>(
          false,
          "No se encontraron recetas con ese título",
          recetas
        )
      );
    }
    return ResponseEntity.ok(
      new ApiResponse<>(true, "Recetas encontradas", recetas)
    );
  }

  // ✅ Buscar recetas por título general
  @GetMapping("/buscar/titulo")
  public ResponseEntity<ApiResponse<List<Receta>>> buscarRecetasEnPagina(
    @RequestParam String titulo
  ) {
    List<Receta> recetas = recetaRepository.findByNombreContainingIgnoreCase(
      titulo
    );

    if (recetas.isEmpty()) {
      return ResponseEntity.ok(
        new ApiResponse<>(false, "No se encontraron recetas", recetas)
      );
    }
    return ResponseEntity.ok(
      new ApiResponse<>(true, "Recetas encontradas", recetas)
    );
  }

  // ✅ Buscar recetas por autor
  @GetMapping("/buscar/autor")
  public ResponseEntity<ApiResponse<List<Receta>>> buscarRecetasPorAutor(
    @RequestParam String nombreAutor
  ) {
    List<Usuario> usuarios = usuarioRepository.findByNombreContainingIgnoreCase(
      nombreAutor
    );

    if (usuarios.isEmpty()) {
      return ResponseEntity.ok(
        new ApiResponse<>(false, "No se encontraron usuarios con ese nombre")
      );
    }

    List<Receta> recetas = usuarios
      .stream()
      .flatMap(u -> recetaRepository.findByUsuarioId(u.getId()).stream())
      .toList();

    if (recetas.isEmpty()) {
      return ResponseEntity.ok(
        new ApiResponse<>(false, "No se encontraron recetas para ese autor")
      );
    }
    return ResponseEntity.ok(
      new ApiResponse<>(true, "Recetas encontradas", recetas)
    );
  }

  // ✅ Buscar recetas por término general (compatible con /buscar?termino=)
  @GetMapping("/buscar")
  public ResponseEntity<ApiResponse<List<Receta>>> buscarRecetasPorTermino(
    @RequestParam String termino
  ) {
    List<Receta> recetas = recetaRepository.findByNombreContainingIgnoreCase(
      termino
    );

    if (recetas.isEmpty()) {
      return ResponseEntity.ok(
        new ApiResponse<>(false, "No se encontraron recetas", recetas)
      );
    }
    return ResponseEntity.ok(
      new ApiResponse<>(true, "Recetas encontradas", recetas)
    );
  }
}
