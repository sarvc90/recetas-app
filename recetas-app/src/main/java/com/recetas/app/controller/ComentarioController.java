package com.recetas.app.controller;

import com.recetas.app.dto.ApiResponse;
import com.recetas.app.dto.EmailDto;
import com.recetas.app.entity.Comentario;
import com.recetas.app.entity.Usuario;
import com.recetas.app.repository.ComentarioRepository;
import com.recetas.app.repository.RecetaRepository;
import com.recetas.app.repository.UsuarioRepository;
import com.recetas.app.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    @Autowired
    private EmailService emailService;

    @Autowired
    private RecetaRepository recetaRepository;

    private static final Logger log = LoggerFactory.getLogger(ComentarioController.class);

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
            if (payload.get("usuarioId") == null || payload.get("texto") == null || payload.get("calificacion") == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Datos incompletos"));
            }

            Long usuarioId = ((Number) payload.get("usuarioId")).longValue();
            String texto = (String) payload.get("texto");
            Integer calificacion = (Integer) payload.get("calificacion");

            if (comentarioRepository.existsByRecetaIdAndUsuarioId(recetaId, usuarioId)) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Ya has comentado esta receta"));
            }

            Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
            String nombreUsuario = usuarioOpt.map(Usuario::getNombre).orElse("Usuario");

            Comentario nuevoComentario = new Comentario(recetaId, usuarioId, texto, calificacion, nombreUsuario);
            nuevoComentario.setFechaCreacion(LocalDateTime.now());
            Comentario guardado = comentarioRepository.save(nuevoComentario);

            try {
                recetaRepository.findById(recetaId).ifPresent(receta -> {

                    // Email al creador de la receta
                    usuarioRepository.findById(receta.getUsuarioId()).ifPresent(creador -> {

                        try {
                            emailService.sendMail(new EmailDto(
                                    "Nuevo comentario en tu receta \"%s\"".formatted(receta.getNombre()),
                                    """
                                    Hola %s,
                        
                                    %s ha dejado un comentario en tu receta "%s":
                        
                                    "%s"
                        
                                    Calificación: %d/5
                        
                                    Ingresa a la plataforma para verlo.
                                    """.formatted(creador.getNombre(), nombreUsuario, receta.getNombre(), texto, calificacion),
                                    creador.getEmail()
                            ));
                        } catch (Exception e) {
                            log.warn("No se pudo notificar al creador {}: {}", creador.getEmail(), e.getMessage());
                        }
                    });

                    // Email al usuario que comentó
                    usuarioOpt.ifPresent(comentarista -> {
                        try {
                            emailService.sendMail(new EmailDto(
                                    "Tu comentario fue publicado en \"%s\"".formatted(receta.getNombre()),
                                    """
                                    Hola %s,
                        
                                    Tu comentario en la receta "%s" fue publicado exitosamente.
                        
                                    "%s"
                        
                                    Calificación: %d/5
                        
                                    ¡Gracias por participar!
                                    """.formatted(comentarista.getNombre(), receta.getNombre(), texto, calificacion),
                                    comentarista.getEmail()
                            ));
                        } catch (Exception e) {
                            log.warn("No se pudo notificar al comentarista {}: {}", comentarista.getEmail(), e.getMessage());
                        }
                    });
                });
            } catch (Exception e) {
                log.warn("Error al procesar notificaciones de comentario: {}", e.getMessage());
            }

            return ResponseEntity.ok(new ApiResponse<>(true, "Comentario publicado correctamente", guardado));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Error al guardar el comentario"));
        }
    }
}
