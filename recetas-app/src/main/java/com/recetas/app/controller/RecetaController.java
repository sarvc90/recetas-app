package com.recetas.app.controller;

import com.recetas.app.entity.Receta;
import com.recetas.app.repository.RecetaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/recetas")
@CrossOrigin(origins = "*")
public class RecetaController {

    @Autowired
    private RecetaRepository recetaRepository;

    @GetMapping
    public Page<Receta> getAllRecetas(Pageable pageable) {
        return recetaRepository.findAll(pageable);
    }

    @PostMapping
    public ResponseEntity<Receta> crearReceta(@RequestBody Receta receta) {
        receta.setFechaCreacion(LocalDateTime.now());
        return ResponseEntity.ok(recetaRepository.save(receta));
    }
}