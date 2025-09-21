package com.recetas.app.controller;

import com.recetas.app.entity.Receta;
import com.recetas.app.repository.RecetaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    @Autowired
    private RecetaRepository recetaRepository;

    // Endpoint de prueba para verificar que el repository funciona
    @GetMapping("/recetas")
    public List<Receta> getAllRecetas() {
        return recetaRepository.findAll();
    }

    // Endpoint de prueba para contar recetas
    @GetMapping("/count")
    public String countRecetas() {
        long count = recetaRepository.count();
        return "Total de recetas en la base de datos: " + count;
    }

    // Endpoint de prueba para obtener una receta específica
    @GetMapping("/receta/{id}")
    public Receta getReceta(@PathVariable Long id) {
        return recetaRepository.findById(id).orElse(null);
    }
}