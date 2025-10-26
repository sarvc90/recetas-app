package com.recetas.app.controller;

import com.recetas.app.dto.ApiResponse;
import com.recetas.app.entity.Comentario;
import com.recetas.app.entity.Usuario;
import com.recetas.app.repository.ComentarioRepository;
import com.recetas.app.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/recetas")
@CrossOrigin(origins = "*")
public class ComentarioController {

    @Autowired
    private ComentarioRepository comentarioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    //  Obtener comentarios de una receta (con promedio y total)
    @GetMapping("/{recetaId}/comentarios")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerComentariosPorReceta(@PathVariable Long recetaId) {
        List<Comentario> comentarios = comentarioRepository.findByRecetaIdOrderByFechaCreacionDesc(recetaId);

        double promedio = comentarios.isEmpty()
                ? 0
                : comentarios.stream().mapToInt(Comentario::getCalificacion).average().orElse(0);

        Map<String, Object> data = new HashMap<>();
        data.put("comentarios", comentarios);
        data.put("promedioCalificacion", promedio);
        data.put("totalComentarios", comentarios.size());

        return ResponseEntity.ok(new ApiResponse<>(true, "Comentarios obtenidos correctamente", data));
    }

    //  Verificar si un usuario ya comentó una receta (para ocultar el formulario)
    @GetMapping("/{recetaId}/comentarios/verificar")
    public ResponseEntity<ApiResponse<Boolean>> verificarComentario(
            @PathVariable Long recetaId,
            @RequestParam Long usuarioId) {

        boolean yaComento = comentarioRepository.existsByRecetaIdAndUsuarioId(recetaId, usuarioId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Verificación realizada", yaComento));
    }

    //  Publicar un nuevo comentario
    @PostMapping("/{recetaId}/comentarios")
    public ResponseEntity<ApiResponse<Comentario>> crearComentario(
            @PathVariable Long recetaId,
            @RequestBody Map<String, Object> payload) {

        try {
            Long usuarioId = ((Number) payload.get("usuarioId")).longValue();
            String texto = (String) payload.get("texto");
            Integer calificacion = (Integer) payload.get("calificacion");

            if (usuarioId == null || texto == null || calificacion == null) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Datos incompletos"));
            }

            // Evitar duplicados: un usuario solo puede comentar una vez por receta
            if (comentarioRepository.existsByRecetaIdAndUsuarioId(recetaId, usuarioId)) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Ya has comentado esta receta"));
            }

            // Obtener nombre del usuario
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
            String nombreUsuario = usuarioOpt.map(Usuario::getNombre).orElse("Usuario");

            Comentario nuevoComentario = new Comentario(
                    recetaId,
                    usuarioId,
                    texto,
                    calificacion,
                    nombreUsuario
            );

            nuevoComentario.setFechaCreacion(LocalDateTime.now());
            Comentario guardado = comentarioRepository.save(nuevoComentario);

            return ResponseEntity.ok(new ApiResponse<>(true, "Comentario publicado correctamente", guardado));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Error al guardar el comentario"));
        }
    }
}
