package com.recetas.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.recetas.app.entity.Receta;
import com.recetas.app.repository.RecetaRepository;
import org.springframework.boot.CommandLineRunner;

@SpringBootApplication
public class RecetasAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecetasAppApplication.class, args);
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CommandLineRunner initData(RecetaRepository recetaRepository) {
		return args -> {
			if (recetaRepository.count() == 0) {
				Receta paella = new Receta(
						"Paella Valenciana",
						"Arroz típico español con mariscos y pollo",
						"Arroz, pollo, camarones, mejillones, verduras, azafrán",
						"Cocinar el arroz con caldo, añadir ingredientes y dejar reposar",
						1L
				);
				paella.setImagenUrl("https://upload.wikimedia.org/wikipedia/commons/4/44/Paella-mixta.jpg");
				paella.setTiempoPreparacion(60);
				paella.setPorciones(4);

				Receta arepa = new Receta(
						"Arepas Rellenas",
						"Arepas de maíz rellenas con queso y jamón",
						"Harina de maíz, agua, sal, queso, jamón",
						"Mezclar la masa, formar arepas, cocinar y rellenar",
						2L
				);
				arepa.setImagenUrl("https://upload.wikimedia.org/wikipedia/commons/7/7c/Arepa_de_queso.jpg");
				arepa.setTiempoPreparacion(20);
				arepa.setPorciones(2);

				recetaRepository.save(paella);
				recetaRepository.save(arepa);
			}
		};
	}
}