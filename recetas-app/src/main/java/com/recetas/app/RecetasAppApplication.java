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
}