# 🏗️ Estructura del Proyecto Recetas App

## 📁 Estructura General del Proyecto

```
recetas-app/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/recetas/app/          👈 **Paquete principal**
│       │       ├── entity/               👈 **Modelos de datos**
│       │       ├── dto/                  👈 **Objetos de transferencia**
│       │       ├── repository/           👈 **Acceso a datos**
│       │       ├── service/              👈 **Lógica de negocio**
│       │       ├── controller/           👈 **APIs REST**
│       │       └── RecetasAppApplication.java  👈 **Clase principal**
│       └── resources/
│           └── application.properties    👈 **Configuración**
├── pom.xml                              👈 **Dependencias Maven**
└── target/                              👈 **Archivos compilados**
```

---

## 📂 Explicación de cada capa

### 1. **entity/** - Modelos de Datos
**¿Qué es?** Son las clases que representan las tablas de tu base de datos.

**Ejemplo: Usuario.java**
```java
@Entity                    // Dice "esto es una tabla"
@Table(name = "usuarios")  // Nombre de la tabla en BD
public class Usuario {
    @Id                    // Clave primaria
    @GeneratedValue        // Auto-incremento
    private Long id;
    
    @Column(nullable = false) // Columna obligatoria
    private String nombre;
}
```

**¿Por qué existe?**
- Cada clase = Una tabla en la base de datos
- Spring automáticamente crea/actualiza las tablas
- Puedes hacer consultas como: `usuarioRepository.findByEmail(email)`

---

### 2. **dto/** - Data Transfer Objects
**¿Qué son?** Son objetos para enviar/recibir datos, pero NO se guardan en la base de datos.

**¿Por qué no usar directamente las entidades?**
```java
// ❌ MAL: Exponer la entidad directamente
public Usuario login(String email) {
    return usuario; // Incluye password, fechas internas, etc.
}

// ✅ BIEN: Usar DTO
public UsuarioResponse login(String email) {
    return new UsuarioResponse(id, nombre, email); // Solo lo necesario
}
```

**Los DTOs que tenemos:**
- `LoginRequest`: Lo que envía el frontend para login
- `RegistroRequest`: Lo que envía el frontend para registro  
- `UsuarioResponse`: Lo que devolvemos (sin password)
- `ApiResponse`: Envoltorio para todas las respuestas

---

### 3. **repository/** - Acceso a Datos
**¿Qué hace?** Se conecta con la base de datos para hacer consultas.

**Con Repository (forma fácil):**
```java
Optional<Usuario> usuario = usuarioRepository.findByEmail(email);
// ¡Spring hace toda la magia automáticamente!
```

**Métodos que Spring crea automáticamente:**
- `save(usuario)` - Guardar
- `findById(id)` - Buscar por ID  
- `findAll()` - Traer todos
- `findByEmail(email)` - Spring deduce la consulta por el nombre

---

### 4. **service/** - Lógica de Negocio
**¿Qué hace?** Contiene las reglas y validaciones de tu aplicación.

**Ejemplo de lo que hace:**
```java
public ApiResponse<UsuarioResponse> login(LoginRequest request) {
    // 1. Validar que exista el usuario
    // 2. Verificar la contraseña  
    // 3. Actualizar último acceso
    // 4. Devolver respuesta segura (sin password)
}
```

**¿Por qué separarlo del Controller?**
- **Controller**: Solo recibe/envía datos
- **Service**: Hace todo el trabajo pesado
- Si después quieres cambiar la API, solo cambias el Controller
- Puedes reutilizar el Service en otros lugares

---

### 5. **controller/** - APIs REST
**¿Qué hace?** Define las URLs de tu API y recibe las peticiones HTTP.

**Anatomía de un Controller:**
```java
@RestController                    // "Esto es una API REST"
@RequestMapping("/api/auth")       // Prefijo: localhost:8080/api/auth
@CrossOrigin(origins = "*")        // Permitir peticiones desde el frontend
public class AuthController {
    
    @PostMapping("/login")         // POST a /api/auth/login
    public ResponseEntity<ApiResponse<UsuarioResponse>> login(@RequestBody LoginRequest request) {
        // Delegar el trabajo al Service
        ApiResponse<UsuarioResponse> response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
```

**URLs que se crean automáticamente:**
- `POST http://localhost:8080/api/auth/login`
- `POST http://localhost:8080/api/auth/registro`

---

## 🔄 Flujo completo de una petición

1. **Frontend envía:** `POST /api/auth/login`
   ```json
   { "email": "juan@test.com", "password": "123456" }
   ```

2. **AuthController** recibe la petición  
   ↓  
3. **AuthController** llama a `AuthService.login()`  
   ↓  
4. **AuthService** busca el usuario usando `UsuarioRepository`  
   ↓  
5. **UsuarioRepository** ejecuta: `SELECT * FROM usuarios WHERE email = 'juan@test.com'`  
   ↓  
6. Si encuentra el usuario, verifica la password  
   ↓  
7. **AuthService** crea un `UsuarioResponse` (sin password)  
   ↓  
8. **AuthController** devuelve la respuesta al Frontend  
   ↓  
9. **Frontend recibe:**
   ```json
   { 
     "success": true, 
     "data": { 
       "id": 1, 
       "nombre": "Juan" 
     } 
   }
   ```

---

## 📄 Archivos de Configuración

### **application.properties**
```properties
# Puerto donde corre la aplicación
server.port=8080

# Configuración de base de datos
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=RecetasApp
spring.datasource.username=tu_usuario
spring.datasource.password=tu_password

# JPA/Hibernate (manejo automático de BD)
spring.jpa.hibernate.ddl-auto=update  # Crea/actualiza tablas automáticamente
spring.jpa.show-sql=true              # Muestra las consultas SQL en consola
```

### **pom.xml**
Lista todas las librerías que necesita tu proyecto:
- `spring-boot-starter-web`: Para crear APIs REST
- `spring-boot-starter-data-jpa`: Para trabajar con base de datos  
- `mssql-jdbc`: Driver para conectar con SQL Server
- `spring-security-crypto`: Para encriptar contraseñas

---

## 🧩 Analogía de Componentes

- **Controller** = "Cartero" (recibe y envía)
- **Service** = "Cerebro" (piensa y decide)  
- **Repository** = "Bibliotecario" (busca y guarda datos)
- **Entity** = "Ficha" (estructura de los datos)
- **DTO** = "Sobre" (empaque para enviar/recibir)

---

## 🎨 Frontend (Interfaz de Usuario)

El frontend es la parte visual que ven los usuarios. En este proyecto:

- **HTML**: Estructura de la página (formularios, botones)
- **CSS**: Diseño y apariencia (colores, animaciones)  
- **JavaScript**: Lógica y comunicación con el backend

**Función**: Permitir a los usuarios registrarse e iniciar sesión de forma intuitiva mediante formularios interactivos.

---

## ⚙️ Backend (Lógica del Servidor)

- **Spring Boot**: Framework Java que maneja peticiones HTTP
- **Base de Datos**: Almacena usuarios de forma segura
- **APIs REST**: Endpoints que el frontend consume

---

## 🗃️ Base de Datos

```sql
-- 1. Crear base de datos (ejecutar en SQL Server)
CREATE DATABASE RecetasApp;

-- 2. Crear usuario con permisos (opcional pero recomendado)
USE RecetasApp;
CREATE LOGIN recetas_user WITH PASSWORD = 'RecetasApp123*';
CREATE USER recetas_user FOR LOGIN recetas_user;
EXEC sp_addrolemember 'db_owner', 'recetas_user';
```

---

## 🚀 Proceso de Ejecución

### Paso 1: Clonar/Descargar el proyecto
```
proyecto/
├── backend/           # Código Spring Boot
└── frontend/          # Archivos HTML/CSS/JS
```

### Paso 2: Configurar Base de Datos
- Ejecutar SQL Server
- Verificar que la BD RecetasApp existe
- Configurar credenciales en `application.properties`

### Paso 3: Ejecutar Backend
```bash
cd backend
mvn spring-boot:run
```
**Verificar**: http://localhost:8080 → Debe mostrar error "Whitelabel" (normal)

### Paso 4: Ejecutar Frontend
```bash
cd frontend
# Abrir index.html en el navegador
# O ejecutar servidor local:
python -m http.server 3000
```
**Abrir**: http://localhost:3000 (o doble clic en index.html)

### Paso 5: Probar Funcionalidad
- Registrar usuario en el formulario
- Iniciar sesión con las mismas credenciales
- Verificar en BD: `SELECT * FROM usuarios;` (debe verse password encriptado)
