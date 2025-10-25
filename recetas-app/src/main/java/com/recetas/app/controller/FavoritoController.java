package com.recetas.app.controller;

import com.recetas.app.dto.ApiResponse;
import com.recetas.app.entity.Favorito;
import com.recetas.app.entity.Receta;
import com.recetas.app.repository.FavoritoRepository;
import com.recetas.app.repository.RecetaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favoritos")
@CrossOrigin(origins = "*")
public class FavoritoController {

    @Autowired
    private FavoritoRepository favoritoRepository;

    @Autowired
    private RecetaRepository recetaRepository;

    // ✅ Agregar o quitar de favoritos
    @PostMapping("/toggle")
    public ResponseEntity<ApiResponse<?>> toggleFavorito(@RequestParam Long usuarioId, @RequestParam Long recetaId) {
        var favoritoExistente = favoritoRepository.findByUsuarioIdAndRecetaId(usuarioId, recetaId);

        if (favoritoExistente.isPresent()) {
            favoritoRepository.delete(favoritoExistente.get());
            return ResponseEntity.ok(new ApiResponse<>(true, "Receta eliminada de favoritos"));
        } else {
            favoritoRepository.save(new Favorito(usuarioId, recetaId));
            return ResponseEntity.ok(new ApiResponse<>(true, "Receta agregada a favoritos"));
        }
    }

    // ✅ Obtener todos los favoritos de un usuario
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<?> obtenerFavoritosPorUsuario(@PathVariable Long usuarioId) {
        List<Favorito> favoritos = favoritoRepository.findByUsuarioId(usuarioId);
        if (favoritos.isEmpty()) {
            return ResponseEntity.ok(new ApiResponse<>(false, "Aún no has agregado recetas a favoritos"));
        }

        List<Receta> recetasFav = favoritos.stream()
                .map(f -> recetaRepository.findById(f.getRecetaId()).orElse(null))
                .filter(r -> r != null)
                .toList();

        return ResponseEntity.ok(new ApiResponse<>(true, "Favoritos cargados correctamente", recetasFav));
    }

    // ✅ Verificar si una receta está en favoritos
    @GetMapping("/usuario/{usuarioId}/receta/{recetaId}")
    public ResponseEntity<ApiResponse<Boolean>> esFavorito(
            @PathVariable Long usuarioId, @PathVariable Long recetaId) {

        boolean esFavorito = favoritoRepository.findByUsuarioIdAndRecetaId(usuarioId, recetaId).isPresent();
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado de favorito obtenido", esFavorito));
    }
}