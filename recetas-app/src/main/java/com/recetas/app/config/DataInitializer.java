package com.recetas.app.config;

import com.recetas.app.entity.*;
import com.recetas.app.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

/**
 * Inicializador de datos de prueba para el perfil de desarrollo.
 * Se ejecuta automáticamente al iniciar la aplicación con el perfil "dev".
 *
 * Usuarios de prueba (password: password123):
 *   carlos@email.com, maria@email.com, juan@email.com,
 *   ana@email.com, pedro@email.com
 */
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RecetaRepository recetaRepository;

    @Autowired
    private ComentarioRepository comentarioRepository;

    @Autowired
    private FavoritoRepository favoritoRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        // Solo insertar si la BD está vacía
        if (usuarioRepository.count() > 0) {
            System.out.println("📦 Base de datos ya tiene datos. Saltando inicialización.");
            return;
        }

        System.out.println("🚀 Insertando datos de prueba...");

        String hashedPassword = passwordEncoder.encode("password123");

        // ===================== USUARIOS =====================
        Usuario carlos = new Usuario("Carlos García", "carlos@email.com", hashedPassword);
        carlos.setEmailVerificado(true);
        carlos.setFechaCreacion(LocalDateTime.of(2025, 1, 15, 10, 30));
        carlos.setUltimoAcceso(LocalDateTime.of(2026, 2, 28, 14, 0));

        Usuario maria = new Usuario("María López", "maria@email.com", hashedPassword);
        maria.setEmailVerificado(true);
        maria.setFechaCreacion(LocalDateTime.of(2025, 2, 20, 8, 15));
        maria.setUltimoAcceso(LocalDateTime.of(2026, 3, 1, 9, 30));

        Usuario juan = new Usuario("Juan Rodríguez", "juan@email.com", hashedPassword);
        juan.setEmailVerificado(true);
        juan.setFechaCreacion(LocalDateTime.of(2025, 3, 10, 16, 45));
        juan.setUltimoAcceso(LocalDateTime.of(2026, 2, 25, 20, 15));

        Usuario ana = new Usuario("Ana Martínez", "ana@email.com", hashedPassword);
        ana.setEmailVerificado(true);
        ana.setFechaCreacion(LocalDateTime.of(2025, 4, 5, 12, 0));
        ana.setUltimoAcceso(LocalDateTime.of(2026, 3, 1, 11, 0));

        Usuario pedro = new Usuario("Pedro Sánchez", "pedro@email.com", hashedPassword);
        pedro.setEmailVerificado(true);
        pedro.setFechaCreacion(LocalDateTime.of(2025, 5, 18, 9, 0));
        pedro.setUltimoAcceso(LocalDateTime.of(2026, 2, 27, 18, 45));

        carlos = usuarioRepository.save(carlos);
        maria = usuarioRepository.save(maria);
        juan = usuarioRepository.save(juan);
        ana = usuarioRepository.save(ana);
        pedro = usuarioRepository.save(pedro);

        System.out.println("✅ 5 usuarios creados");

        // ===================== RECETAS =====================

        // -- Recetas de Carlos --
        Receta bandejaPaisa = crearReceta(
                "Bandeja Paisa",
                "El plato más representativo de la gastronomía antioqueña colombiana.",
                "Frijoles rojos\nArroz blanco\nCarne molida\nChicharrón\nHuevo frito\nPlátano maduro\nAguacate\nArepa\nMorcilla\nHogao (tomate y cebolla)",
                "1. Cocinar los frijoles rojos desde la noche anterior en olla a presión con sal.\n2. Preparar el arroz blanco.\n3. Freír la carne molida con hogao.\n4. Freír el chicharrón hasta que esté crocante.\n5. Freír los huevos.\n6. Freír los plátanos maduros en rodajas.\n7. Preparar las arepas y la morcilla a la plancha.\n8. Servir todo en un plato grande junto con aguacate.",
                90, 4, carlos.getId(), LocalDateTime.of(2025, 6, 1, 10, 0));

        Receta ajiaco = crearReceta(
                "Ajiaco Santafereño",
                "Sopa tradicional bogotana con tres tipos de papa, pollo y guascas.",
                "Pollo (pechugas y muslos)\nPapa criolla\nPapa pastusa\nPapa sabanera\nMazorca\nGuascas\nAlcaparras\nCrema de leche\nAguacate\nSal y pimienta",
                "1. Cocinar el pollo en agua con sal hasta que esté tierno, retirar y desmechar.\n2. En el mismo caldo, agregar las papas sabaneras y pastusas peladas y picadas.\n3. Agregar las mazorcas cortadas en rodajas.\n4. Cocinar hasta que las papas se deshagan y espesen el caldo.\n5. Agregar las papas criollas enteras.\n6. Añadir las guascas y cocinar 10 minutos más.\n7. Servir con el pollo desmechado, crema de leche, alcaparras y aguacate.",
                60, 6, carlos.getId(), LocalDateTime.of(2025, 7, 15, 14, 30));

        // -- Recetas de María --
        Receta arrozConPollo = crearReceta(
                "Arroz con Pollo Colombiano",
                "Clásico arroz con pollo al estilo colombiano, lleno de color y sabor.",
                "Arroz\nPollo en presas\nCerveza\nZanahoria rallada\nArvejas\nPimentón rojo\nCebolla\nAjo\nComino\nColor (achiote)\nCilantro\nSal",
                "1. Sofreír el pollo con cebolla, ajo y comino hasta dorar.\n2. Agregar la cerveza y dejar reducir.\n3. Añadir la zanahoria, arvejas y pimentón.\n4. Agregar el arroz lavado, el color disuelto en agua y caldo suficiente.\n5. Cocinar a fuego bajo tapado por 25 minutos.\n6. Dejar reposar 5 minutos y servir con cilantro picado.",
                50, 6, maria.getId(), LocalDateTime.of(2025, 8, 10, 11, 0));

        Receta empanadas = crearReceta(
                "Empanadas Colombianas",
                "Empanadas crujientes rellenas de carne y papa, el snack perfecto.",
                "Masa de maíz amarillo\nCarne molida de res\nPapa cocida y machacada\nHogao (tomate, cebolla)\nComino\nColor (achiote)\nSal\nAceite para freír\nAjí colombiano para acompañar",
                "1. Preparar el relleno: sofreír la carne con hogao, comino y sal.\n2. Mezclar la carne con la papa machacada.\n3. Preparar la masa de maíz con agua tibia, sal y color.\n4. Formar bolitas de masa, aplanar y rellenar con la mezcla.\n5. Cerrar dando forma de media luna.\n6. Freír en aceite bien caliente hasta dorar.\n7. Servir con ají colombiano.",
                45, 15, maria.getId(), LocalDateTime.of(2025, 9, 5, 16, 0));

        Receta sancocho = crearReceta(
                "Sancocho de Gallina",
                "Sopa sustanciosa colombiana ideal para reuniones familiares.",
                "Gallina en presas\nYuca\nPlátano verde\nPapa\nMazorca\nCebolla larga\nCilantro\nAjo\nComino\nSal\nArroz blanco (acompañamiento)\nAguacate (acompañamiento)",
                "1. Cocinar la gallina en abundante agua con cebolla, ajo y comino.\n2. Cuando esté casi tierna, agregar la yuca, plátano y papa picados.\n3. Añadir la mazorca en rodajas.\n4. Cocinar a fuego medio hasta que todo esté tierno.\n5. Agregar cilantro picado al final.\n6. Servir con arroz blanco y aguacate aparte.",
                120, 8, maria.getId(), LocalDateTime.of(2025, 10, 20, 9, 0));

        // -- Recetas de Juan --
        Receta patacones = crearReceta(
                "Patacones con Hogao",
                "Plátano verde frito y aplastado con salsa hogao, acompañamiento clásico.",
                "Plátanos verdes\nAceite para freír\nSal\nTomate maduro\nCebolla larga\nAjo\nCilantro\nComino\nSal y pimienta",
                "1. Pelar y cortar los plátanos en trozos gruesos.\n2. Freír a fuego medio hasta que estén ligeramente dorados.\n3. Retirar y aplastar con una pataconera o plato.\n4. Freír nuevamente hasta que estén crocantes y dorados.\n5. Para el hogao: sofreír cebolla y ajo, agregar tomate picado, comino y sal.\n6. Cocinar hasta que espese. Servir los patacones con hogao y cilantro.",
                25, 4, juan.getId(), LocalDateTime.of(2025, 11, 1, 13, 0));

        Receta tamales = crearReceta(
                "Tamales Tolimenses",
                "Tamales envueltos en hoja de plátano al estilo del Tolima.",
                "Masa de arroz\nCarne de cerdo\nPollo\nTocino\nArvejas\nZanahoria\nHuevo cocido\nHogao\nComino\nColor\nHojas de plátano\nSal",
                "1. Preparar la masa de arroz con caldo, color y sal.\n2. Cocinar las carnes con hogao y comino.\n3. Cocinar arvejas y zanahoria.\n4. Soasar las hojas de plátano para hacerlas flexibles.\n5. Colocar masa sobre la hoja, añadir carnes, arvejas, zanahoria y huevo.\n6. Envolver bien y amarrar.\n7. Cocinar en olla grande con agua por 3-4 horas.\n8. Servir calientes.",
                240, 10, juan.getId(), LocalDateTime.of(2025, 11, 15, 7, 0));

        // -- Recetas de Ana --
        Receta bunuelos = crearReceta(
                "Buñuelos Colombianos",
                "Bolitas fritas de queso, perfectas para la Navidad colombiana.",
                "Queso costeño rallado\nAlmidón de yuca\nHuevos\nAzúcar\nPolvo de hornear\nAceite para freír",
                "1. Mezclar el queso rallado con el almidón de yuca.\n2. Agregar los huevos, azúcar y polvo de hornear.\n3. Amasar hasta obtener una masa suave y homogénea.\n4. Formar bolitas del tamaño de una pelota de golf.\n5. Freír en aceite a temperatura media, girando constantemente.\n6. Retirar cuando estén dorados por todos lados.\n7. Escurrir y servir calientes.",
                30, 20, ana.getId(), LocalDateTime.of(2025, 12, 1, 15, 0));

        Receta changua = crearReceta(
                "Changua",
                "Caldo de leche con huevo, desayuno tradicional boyacense.",
                "Leche entera\nAgua\nHuevos\nCebolla larga picada\nCilantro\nPan (calado)\nSal",
                "1. Hervir partes iguales de agua y leche con sal.\n2. Cuando hierva, agregar la cebolla larga picada.\n3. Romper los huevos directamente en el caldo sin batir.\n4. Cocinar 3 minutos sin revolver para que los huevos queden escalfados.\n5. Servir en un plato hondo con cilantro y pan calado.",
                15, 2, ana.getId(), LocalDateTime.of(2026, 1, 10, 6, 30));

        // -- Recetas de Pedro --
        Receta lechona = crearReceta(
                "Lechona Tolimense",
                "Cerdo relleno de arroz y arvejas, fiesta de sabor colombiana.",
                "Cerdo entero\nArroz\nArvejas secas\nCebolla\nAjo\nComino\nPimienta\nColor\nSal\nArepa (acompañamiento)",
                "1. Limpiar y adobar el cerdo con ajo, comino, pimienta y sal.\n2. Dejar marinar toda la noche.\n3. Cocinar el arroz y las arvejas por separado.\n4. Mezclar arroz, arvejas con hogao y condimentos.\n5. Rellenar el cerdo con la mezcla.\n6. Coser y hornear a temperatura baja por 8-10 horas.\n7. Servir con arepa.",
                600, 20, pedro.getId(), LocalDateTime.of(2026, 1, 20, 8, 0));

        Receta pandebono = crearReceta(
                "Pandebono",
                "Pan de queso colombiano, crujiente por fuera y suave por dentro.",
                "Almidón de yuca\nQueso costeño\nQueso mozzarella\nHuevo\nAzúcar\nSal",
                "1. Rallar los quesos y mezclar con el almidón de yuca.\n2. Agregar el huevo, azúcar y sal.\n3. Amasar hasta que la masa sea manejable.\n4. Formar bolitas o roscas.\n5. Colocar en bandeja engrasada.\n6. Hornear a 200°C por 15-20 minutos hasta que estén dorados.",
                30, 12, pedro.getId(), LocalDateTime.of(2026, 2, 5, 10, 30));

        Receta arepasChoclo = crearReceta(
                "Arepa de Choclo",
                "Arepa dulce de maíz tierno, típica de la región andina colombiana.",
                "Maíz tierno (choclo)\nQueso campesino\nAzúcar\nSal\nMantequilla",
                "1. Desgranar y moler el maíz tierno hasta obtener una masa.\n2. Mezclar con azúcar y sal al gusto.\n3. Formar las arepas y rellenar con queso campesino.\n4. Cocinar en sartén engrasado con mantequilla a fuego bajo.\n5. Voltear cuando esté dorada por un lado.\n6. Servir calientes.",
                20, 6, pedro.getId(), LocalDateTime.of(2026, 2, 15, 12, 0));

        System.out.println("✅ 12 recetas creadas");

        // ===================== COMENTARIOS =====================

        // Bandeja Paisa
        guardarComentario(bandejaPaisa.getId(), maria.getId(), "¡Espectacular! La mejor bandeja paisa que he preparado siguiendo una receta. Los frijoles quedaron perfectos.", 5, "María López", LocalDateTime.of(2025, 6, 15, 12, 0));
        guardarComentario(bandejaPaisa.getId(), juan.getId(), "Muy buena receta, aunque yo le agregaría chorizo también. De resto, excelente.", 4, "Juan Rodríguez", LocalDateTime.of(2025, 6, 20, 18, 30));
        guardarComentario(bandejaPaisa.getId(), ana.getId(), "Me encantó. Perfecta para un domingo en familia. El hogao es clave.", 5, "Ana Martínez", LocalDateTime.of(2025, 7, 1, 10, 15));

        // Ajiaco
        guardarComentario(ajiaco.getId(), maria.getId(), "El secreto está en las guascas. Muy auténtico el sabor.", 5, "María López", LocalDateTime.of(2025, 7, 20, 14, 0));
        guardarComentario(ajiaco.getId(), pedro.getId(), "Quedó delicioso. Lo hice para una reunión y todos repitieron.", 4, "Pedro Sánchez", LocalDateTime.of(2025, 8, 5, 19, 0));

        // Arroz con Pollo
        guardarComentario(arrozConPollo.getId(), carlos.getId(), "El truco de la cerveza le da un sabor increíble. Muy recomendada.", 5, "Carlos García", LocalDateTime.of(2025, 8, 15, 13, 0));
        guardarComentario(arrozConPollo.getId(), ana.getId(), "Lo preparé para mis hijos y les encantó. Fácil y rendidor.", 4, "Ana Martínez", LocalDateTime.of(2025, 9, 1, 11, 30));
        guardarComentario(arrozConPollo.getId(), pedro.getId(), "Buen arroz, aunque a mí me gusta un poco más seco. Le bajé el caldo y quedó perfecto.", 4, "Pedro Sánchez", LocalDateTime.of(2025, 9, 10, 17, 0));

        // Empanadas
        guardarComentario(empanadas.getId(), carlos.getId(), "¡Quedan crujientes y deliciosas! El ají es fundamental para acompañarlas.", 5, "Carlos García", LocalDateTime.of(2025, 9, 15, 14, 0));
        guardarComentario(empanadas.getId(), juan.getId(), "Excelente receta. Las hice para una fiesta y volaron. Recomiendo usar maíz pelado.", 5, "Juan Rodríguez", LocalDateTime.of(2025, 10, 1, 16, 0));

        // Sancocho
        guardarComentario(sancocho.getId(), carlos.getId(), "Sancocho de verdad. El sabor de la gallina no se compara con el pollo. ¡Divino!", 5, "Carlos García", LocalDateTime.of(2025, 10, 25, 12, 30));
        guardarComentario(sancocho.getId(), juan.getId(), "Lo hice para un asado familiar. Combinado con aguacate, espectacular.", 4, "Juan Rodríguez", LocalDateTime.of(2025, 11, 5, 13, 0));
        guardarComentario(sancocho.getId(), ana.getId(), "Riquísimo. Solo le faltaría un poco más de cilantro al final.", 4, "Ana Martínez", LocalDateTime.of(2025, 11, 10, 19, 30));

        // Patacones
        guardarComentario(patacones.getId(), maria.getId(), "Super fáciles de hacer y quedan crocantes. El hogao es lo mejor.", 5, "María López", LocalDateTime.of(2025, 11, 10, 14, 0));
        guardarComentario(patacones.getId(), ana.getId(), "Acompañé con guacamole y suero costeño. ¡Delicia!", 4, "Ana Martínez", LocalDateTime.of(2025, 11, 20, 18, 0));

        // Tamales
        guardarComentario(tamales.getId(), carlos.getId(), "Trabajo de todo un día, pero vale cada minuto. Auténticos tamales tolimenses.", 5, "Carlos García", LocalDateTime.of(2025, 11, 25, 8, 0));
        guardarComentario(tamales.getId(), maria.getId(), "Mi abuela los hacía igualitos. Gracias por compartir esta receta.", 5, "María López", LocalDateTime.of(2025, 12, 1, 10, 0));

        // Buñuelos
        guardarComentario(bunuelos.getId(), carlos.getId(), "Perfectos para Navidad. Quedan esponjosos y con mucho sabor a queso.", 5, "Carlos García", LocalDateTime.of(2025, 12, 10, 16, 0));
        guardarComentario(bunuelos.getId(), maria.getId(), "Los hice para la novena y fueron un éxito. ¡Facilísimos!", 5, "María López", LocalDateTime.of(2025, 12, 15, 20, 0));
        guardarComentario(bunuelos.getId(), pedro.getId(), "El truco es que el aceite no esté muy caliente para que crezcan bien.", 4, "Pedro Sánchez", LocalDateTime.of(2025, 12, 20, 11, 0));

        // Changua
        guardarComentario(changua.getId(), juan.getId(), "Desayuno bogotano de toda la vida. Simple pero reconfortante.", 4, "Juan Rodríguez", LocalDateTime.of(2026, 1, 15, 7, 30));
        guardarComentario(changua.getId(), pedro.getId(), "No la conocía y me sorprendió. El huevo escalfado en leche es una delicia.", 4, "Pedro Sánchez", LocalDateTime.of(2026, 1, 20, 8, 0));

        // Lechona
        guardarComentario(lechona.getId(), carlos.getId(), "Impresionante receta. La hicimos para una celebración grande y fue el plato estrella.", 5, "Carlos García", LocalDateTime.of(2026, 2, 1, 13, 0));
        guardarComentario(lechona.getId(), maria.getId(), "El arroz queda con un sabor increíble al cocinarse dentro del cerdo. ¡Top!", 5, "María López", LocalDateTime.of(2026, 2, 10, 15, 30));

        // Pandebono
        guardarComentario(pandebono.getId(), juan.getId(), "Quedan crujientes por fuera y suaves por dentro. Adictivos.", 5, "Juan Rodríguez", LocalDateTime.of(2026, 2, 10, 9, 0));
        guardarComentario(pandebono.getId(), ana.getId(), "Los hago cada fin de semana. Rapidísimos y deliciosos.", 5, "Ana Martínez", LocalDateTime.of(2026, 2, 20, 11, 0));

        // Arepa de Choclo
        guardarComentario(arepasChoclo.getId(), carlos.getId(), "La arepa de choclo con quesito es mi debilidad. Gracias por la receta.", 5, "Carlos García", LocalDateTime.of(2026, 2, 20, 8, 0));
        guardarComentario(arepasChoclo.getId(), maria.getId(), "Sencilla y riquísima. El choclo fresco marca toda la diferencia.", 4, "María López", LocalDateTime.of(2026, 2, 25, 10, 30));

        System.out.println("✅ 28 comentarios creados");

        // ===================== FAVORITOS =====================

        // Carlos
        guardarFavorito(carlos.getId(), arrozConPollo.getId(), LocalDateTime.of(2025, 8, 16, 13, 0));
        guardarFavorito(carlos.getId(), empanadas.getId(), LocalDateTime.of(2025, 9, 16, 14, 30));
        guardarFavorito(carlos.getId(), bunuelos.getId(), LocalDateTime.of(2025, 12, 11, 16, 30));
        guardarFavorito(carlos.getId(), arepasChoclo.getId(), LocalDateTime.of(2026, 2, 21, 8, 30));

        // María
        guardarFavorito(maria.getId(), bandejaPaisa.getId(), LocalDateTime.of(2025, 6, 16, 12, 30));
        guardarFavorito(maria.getId(), patacones.getId(), LocalDateTime.of(2025, 11, 11, 14, 30));
        guardarFavorito(maria.getId(), tamales.getId(), LocalDateTime.of(2025, 12, 2, 10, 30));
        guardarFavorito(maria.getId(), lechona.getId(), LocalDateTime.of(2026, 2, 11, 16, 0));

        // Juan
        guardarFavorito(juan.getId(), bandejaPaisa.getId(), LocalDateTime.of(2025, 6, 21, 19, 0));
        guardarFavorito(juan.getId(), sancocho.getId(), LocalDateTime.of(2025, 11, 6, 13, 30));
        guardarFavorito(juan.getId(), pandebono.getId(), LocalDateTime.of(2026, 2, 11, 9, 30));

        // Ana
        guardarFavorito(ana.getId(), bandejaPaisa.getId(), LocalDateTime.of(2025, 7, 2, 10, 45));
        guardarFavorito(ana.getId(), arrozConPollo.getId(), LocalDateTime.of(2025, 9, 2, 12, 0));
        guardarFavorito(ana.getId(), patacones.getId(), LocalDateTime.of(2025, 11, 21, 18, 30));
        guardarFavorito(ana.getId(), pandebono.getId(), LocalDateTime.of(2026, 2, 21, 11, 30));

        // Pedro
        guardarFavorito(pedro.getId(), ajiaco.getId(), LocalDateTime.of(2025, 8, 6, 19, 30));
        guardarFavorito(pedro.getId(), empanadas.getId(), LocalDateTime.of(2025, 10, 2, 16, 30));
        guardarFavorito(pedro.getId(), bunuelos.getId(), LocalDateTime.of(2025, 12, 21, 11, 30));
        guardarFavorito(pedro.getId(), changua.getId(), LocalDateTime.of(2026, 1, 21, 8, 30));

        System.out.println("✅ 19 favoritos creados");
        System.out.println("🎉 Datos de prueba insertados exitosamente!");
        System.out.println("📋 Credenciales: [carlos|maria|juan|ana|pedro]@email.com / password123");
    }

    // ===================== HELPERS =====================

    private Receta crearReceta(String nombre, String descripcion, String ingredientes,
                                String instrucciones, int tiempoPrep, int porciones,
                                Long usuarioId, LocalDateTime fechaCreacion) {
        Receta r = new Receta(nombre, descripcion, ingredientes, instrucciones, usuarioId);
        r.setTiempoPreparacion(tiempoPrep);
        r.setPorciones(porciones);
        r.setFechaCreacion(fechaCreacion);
        return recetaRepository.save(r);
    }

    private void guardarComentario(Long recetaId, Long usuarioId, String texto,
                                    int calificacion, String nombreUsuario,
                                    LocalDateTime fechaCreacion) {
        Comentario c = new Comentario(recetaId, usuarioId, texto, calificacion, nombreUsuario);
        c.setFechaCreacion(fechaCreacion);
        comentarioRepository.save(c);
    }

    private void guardarFavorito(Long usuarioId, Long recetaId, LocalDateTime fechaGuardado) {
        Favorito f = new Favorito(usuarioId, recetaId);
        f.setFechaGuardado(fechaGuardado);
        favoritoRepository.save(f);
    }
}

