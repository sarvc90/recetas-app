// script.js
const API_BASE_URL = 'http://localhost:8080/api';

// Mostrar resultados en la página
function mostrarResultado(elementId, mensaje, tipo) {
    const elemento = document.getElementById(elementId);
    elemento.innerHTML = mensaje;
    elemento.className = `result ${tipo}`;
}

// Limpiar formularios
function limpiarFormularios() {
    document.getElementById('nombre').value = '';
    document.getElementById('email').value = '';
    document.getElementById('password').value = '';
    document.getElementById('login-email').value = '';
    document.getElementById('login-password').value = '';
}

// Registrar usuario
async function registrarUsuario() {
    const nombre = document.getElementById('nombre').value.trim();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;

    // Validaciones
    if (!nombre || !email || !password) {
        mostrarResultado('resultado-registro', '❌ Por favor, completa todos los campos', 'error');
        return;
    }

    if (password.length < 6) {
        mostrarResultado('resultado-registro', '❌ La contraseña debe tener al menos 6 caracteres', 'error');
        return;
    }

    const usuario = { nombre, email, password };

    try {
        mostrarResultado('resultado-registro', '⏳ Registrando usuario...', 'loading');

        const response = await fetch(`${API_BASE_URL}/auth/registro`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(usuario)
        });

        if (response.ok) {
            const data = await response.json();
            mostrarResultado('resultado-registro',
                `✅ ¡Registro exitoso! Bienvenido ${data.data.nombre}.<br>Tu ID de usuario es: ${data.data.id}`, 'success');
            limpiarFormularios();
        } else {
            const errorText = await response.text();
            let errorMessage = '❌ Error en el registro';

            if (errorText.includes('email"))')) {
                errorMessage = '❌ Este email ya está registrado';
            } else if (errorText.includes('ConstraintViolationException')) {
                errorMessage = '❌ Error de validación: verifica los datos';
            }

            mostrarResultado('resultado-registro', errorMessage, 'error');
        }
    } catch (error) {
        mostrarResultado('resultado-registro',
            `❌ Error de conexión: ${error.message}<br>¿Está corriendo el backend en puerto 8080?`, 'error');
    }
}

// Login de usuario
async function loginUsuario() {
    const email = document.getElementById('login-email').value.trim();
    const password = document.getElementById('login-password').value;

    if (!email || !password) {
        mostrarResultado('resultado-login', '❌ Por favor, completa todos los campos', 'error');
        return;
    }

    const credenciales = { email, password };

    try {
        mostrarResultado('resultado-login', '⏳ Iniciando sesión...', 'loading');

        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(credenciales)
        });

        if (response.ok) {
            const data = await response.json();
            mostrarResultado('resultado-login',
                `✅ ¡Login exitoso! Bienvenido de nuevo ${data.data.nombre}<br>Email: ${data.data.email}`, 'success');
            // Limpiar campo contraseña
            document.getElementById('login-password').value = '';

            // Guardar datos en localStorage (puedes guardar token si tu backend lo envía)
            localStorage.setItem("usuario", JSON.stringify(data.data));

            // Redirigir a recetas.html después de 1.5 segundos
            setTimeout(() => {
                window.location.href = "recetas.html";
            }, 1500);
            document.getElementById('login-password').value = '';
        } else {
            const errorText = await response.text();
            let errorMessage = '❌ Error en el login';

            if (errorText.includes('Credenciales inválidas')) {
                errorMessage = '❌ Email o contraseña incorrectos';
            } else if (errorText.includes('Usuario no encontrado')) {
                errorMessage = '❌ Usuario no encontrado';
            }

            mostrarResultado('resultado-login', errorMessage, 'error');
        }
    } catch (error) {
        mostrarResultado('resultado-login',
            `❌ Error de conexión: ${error.message}<br>Verifica que el backend esté funcionando`, 'error');
    }
}

// Enter key support
document.addEventListener('DOMContentLoaded', function() {
    // Para el formulario de registro
    document.getElementById('password').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            registrarUsuario();
        }
    });

    // Para el formulario de login
    document.getElementById('login-password').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            loginUsuario();
        }
    });
});